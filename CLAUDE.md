# CLAUDE.md — Violet UML Editor

Working notes for Claude Code. Verified against the code at commit `44e16e5` (2026-10-19).
When this file disagrees with older docs, **trust the code, then this file**.

## What it is

Open-source UML editor (Java Swing, originally by Cay S. Horstmann, GPLv2+). Six diagram types:
class, sequence, activity, state, object, use case. The same JAR runs:

- as a **desktop app** (default), and
- **in the browser via CheerpJ** (`-cheerpjMode`), driven by `violetproduct-swing/src/main/resources/web/index.html`.

## Build & test

Toolchain on this machine: Maven 3.9.9, JDK 21 (bytecode target is **17**, `maven.compiler.release=17`).

```bash
mvn -B -q package                       # full build + tests, ~1-2 min, passes clean
mvn -B test -pl violet-framework        # one module
mvn -B test -pl violetproduct-swing -am -Dtest=LegacyXhtmlCompatibilityTest
java -jar violetproduct-swing/target/violetumleditor-2.3.2-SNAPSHOT.jar   # run (shaded fat JAR)
```

- Version: `applicationVersion` = `2.3.2-SNAPSHOT` in root `pom.xml` (parent POM itself is `1.0.0-SNAPSHOT`).
- Extra Maven repo on SourceForge (`violet.sourceforge.net/maven2/repo`) is needed for some old deps.
- Tests (JUnit 5, 21 tests, 7 classes) — all about persistence/serialization:
  - `violet-framework/src/test/.../persistence/{JSON,XML}PersistenceServiceTest`
  - `violetproduct-swing/src/test/.../application/{LegacyXhtmlCompatibility,LegacyXmlWriteCompatibility,GridStickerSerialization,ImageNodeClipboardSerialization,ContainerNodeSerialization}Test`
  - Swing-level tests live in `violetproduct-swing` because they need all plugins on the classpath.
- CI: `.github/workflows/maven-publish.yml` on push to `master` → JDK 21, injects `app.version.number`/`app.release.date` into `OtherStrings*.properties`, `mvn package deploy` (GitHub Packages + SourceForge scp), publishes latest JAR + download page to GitHub Pages. `get-certificate.yml` is disabled.

## Modules (reactor order)

| Module | Notes |
|---|---|
| `violet-framework` | Core: model, workspace/canvas, behaviors, persistence, plugin loader, DI, themes, i18n (~32k LOC) |
| `violetplugin-{class,sequence,activity,state,object,usecase}diagram` | One diagram type each, registered via SPI |
| `violetproduct-swing` | Entry point `com.horstmann.violet.UMLEditorApplication`, menus, `MainFrame`, CheerpJ bridge, shaded JAR |
| `violetproduct-exe`, `violetproduct-deb` | Packaging only, no Java |
| *(not in reactor)* `violetproduct-eclipseplugin`, `-jnlp`, `-rpm`, `violetplugin-communicationdiagram` | Obsolete / disabled — don't touch unless asked |

There is **no `violetproduct-web` (JWT/Jetty) module** any more and **no XStream** in the code.

## Architecture essentials

Root package `com.horstmann.violet`.

- **Model** (`product.diagram.abstracts`): `IGraph`/`AbstractGraph`, `INode`/`AbstractNode` (→ `RectangularNode`, `EllipticalNode`), `IEdge`/`AbstractEdge` (→ `SegmentedLineEdge`, `ShapeEdge`). Shared elements (notes, images, points, containers) in `product.diagram.common`. `ContainerNode` (background frame shown as "Grouping area" in the UI, available in all six diagrams; its tool bar prototype draws `group.png` via `enableToolIcon()`) is special-cased in `AbstractGraph` (drawn first largest→smallest, found last by `findNode`, never a parent, never connected); dragging it moves the top-level nodes inside it (`DragSelectedBehavior`). `FreeTextNode` ("Free text", all six diagrams) is its opposite: no border nor background, drawn upon every element including notes (Z just above them), found first by `findNode`, never a parent, never connected; its tool bar prototype draws a "T" via `enableToolIcon()`.
- **Plugins**: implement `IDiagramPlugin` (name, category, file extension, graph class) + list it in `src/main/resources/META-INF/services/com.horstmann.violet.framework.plugin.IDiagramPlugin`. Shade's `AppendingTransformer` merges those files — keep it, or only one plugin loads. Node/edge prototypes are built in a `static {}` block of each `*Graph` (tooltips freeze to the startup locale).
- **Workspace** (`workspace`): `Workspace` facade = `EditorPart` (canvas) + `SideBar` + `IGraphFile`. Interactions are `IEditorPartBehavior` classes in `workspace.editorpart.behavior`, wired in `Workspace` / `EditorPartBehaviorManager` (incl. undo/redo handlers and `AutoSaveBehavior`).
- **DI — "Manioc"** (`framework.injection.bean.ManiocFramework`), custom, *not* Spring:
  `@ManagedBean`, `@InjectedBean`, `@ImplementedBy`, `@PostConstruct`, `@ResourceBundleBean`;
  classes call `BeanInjector.getInjector().inject(this)` in constructors; global lookup via `BeanFactory.getFactory().getBean(...)`.
  Interface → impl bindings for files/prefs are registered manually at the top of `UMLEditorApplication`'s constructor. Do not add Spring (the `spring-boot-maven-plugin` in the swing POM is a leftover).
- **i18n**: `.properties` co-located with Java sources (`Foo.properties`, `Foo_fr`, `Foo_de`, some `_pl`). When adding a user-visible string, add it to en + fr + de at least.

## Files & persistence (area of recent work)

Package `framework.file`:

- `IFile` (directory + filename), `LocalFile`, `GraphFile`/`IGraphFile` (load, `save(...)`, `saveToNewLocation`, listeners, `isSaveRequired()`), `TempFilenameGenerator` (new diagrams get a temp name and live in the chooser's temp dir until "Save as").
- `file.chooser.IFileChooserService` — abstraction for all file I/O: `chooseAndGetFileReader/Writer` (with optional pre-filled filename), `getFileReader/Writer/Deleter(IFile)`, `getTempDirectory()`. Impl: `JFileChooserService` (desktop) or `CheerpJFileChooserService` (browser).
- `file.persistence`: `IFilePersistenceService` registered as **`XHTMLPersistenceService`** — writes `*.violet.html` (XML embedded in an HTML template `XHTMLFileTemplate.violet.html`), reads it back via the HTML parser and delegates to `XMLPersistenceService`. Also `JSONPersistenceService`, `StandardJavaFilePersistenceService`, `Violet016BackportFormatService` (old class-name mapping, plus per-plugin `Violet016FileFilterExtensionPoint`).
- **Backward compatibility of files is a hard requirement** — run the `Legacy*CompatibilityTest`s after any persistence change.
- `AutoSaveBehavior`: always on (despite the `-autoSave` help text); saves after 2 s idle when dirty, and on shutdown; on save, syncs opened/recent files in `UserPreferencesService`.
- File menu "Recent files" is rebuilt from preferences by a `MenuListener` each time the File menu opens (a `FocusListener` never fires on a `JMenu`).
- File extensions: `.class.violet.html`, `.seq.violet.html`, `.activity.violet.html`, `.state.violet.html`, `.object.violet.html`, `.ucase.violet.html`.

## CheerpJ (browser) mode

- Package `violetproduct-swing/.../application/cheerpj/`:
  `CheerpJInterfaceService` (Java `native` methods implemented in JS), `CheerpJFileChooserService`, `CheerpJFileReader`, `CheerpJStorageFileWriter`/`Deleter` (persist in CheerpJ's IndexedDB-backed FS), `CheerpJDownloadFileWriter` (browser download), `CheerpJFileMenuCustomizerCommand` (adjusts File menu in browser mode: Print → PDF tab, Exit hidden, adds "Import from > Diagram file" (local picker, copied into `/files`) and "Export to > Diagram file" (download); replaces **Open** with a Swing list of the diagrams in `/files` — newest first, trash icon / Delete key per line with confirm, refuses to delete a diagram open in a workspace since auto-save would recreate it).
- JS side: `src/main/resources/web/index.html`. Each native is a function named
  `Java_com_horstmann_violet_application_cheerpj_CheerpJInterfaceService_<method>` and **must also be listed in `cheerpjInit({natives:{...}})`**. Adding a native = Java `native` decl + static wrapper in `CheerpJInterfaceService` + JS function + `natives` entry.
- Launch args used by the page: `-kioskMode -autoSave -cheerpjMode [-file=/str/<name>]`. Imported files are staged in `/str` (JS-writable, Java-readable); CheerpJ runs Java 17.
- Don't create directories at the VFS mount roots.
- Local test without packaging or web server: `mvn compile -Pcheerpj-test` (root dir) writes `violetproduct-swing/target/cheerpj-test/bundle.js` (all classes + jars, base64; generator `violetproduct-swing/src/test/cheerpj/BuildCheerpJTestBundle.java`), then open `web/test.html` via `file://`. `test.html` = copy of `index.html` with a different launch block — keep the JS bridge in sync. See README.
- `violetproduct-swing/CHEERPJ-INTEROP.md` is stale (uses old `...application.jni...` package name).

## CLI options (`framework.userpreferences.LaunchingPreferences`)

`-reset`, `-english`, `-help`/`-?`, `-kioskMode`, `-cheerpjMode`, `-adminPassword=…`, `-file=<path>`.
(The `-help` text says `-cheeprjMode` and `-autoSave`; neither is parsed as such.)

## Conventions & gotchas

- Code style: Allman braces, 4-space indent in most framework code (Eclipse formatter `horstmann.eclipse.code.formatter.xml`); newer CheerpJ classes use K&R — match the file you're editing.
- Resources (`gif/png/properties/xml/html`) sit next to Java sources in `src/main/java` and are packaged from there.
- `MultilineString` must render via JLabel HTML — don't switch it to `MultiLineLabel` (no HTML support).
- `VersionChecker` still checks for Java "1.6" — obsolete, harmless.
- Commit style: Conventional Commits (`feat:`, `fix:`, `refactor:`, `docs:`).
- Work goes directly on `master` historically; ask before pushing.

## Stale docs — don't trust blindly

- `.github/copilot-instructions.md`: wrong Java version (21), version (2.3.0), mentions XStream, `violetproduct-web`, communication plugin, "no tests".
- `CHEERPJ-MIGRATION-COMPLETE.md`, `violetproduct-swing/CHEERPJ-INTEROP.md`: partly outdated.
- `docs/development-specs/00…08` (2026-10-17, commit `70c092a`): thorough, technology-neutral migration specs — the best deep reference for domain model (02), file format (03), features/behaviors/shortcuts (04), coupling/dead code/risks (05), migration plan (06), requirements (07), inventory (08). Note: it implies CheerpJ was abandoned; it is in fact the active browser path.

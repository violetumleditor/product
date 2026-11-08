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

- **Model** (`product.diagram.abstracts`): `IGraph`/`AbstractGraph`, `INode`/`AbstractNode` (→ `RectangularNode`, `EllipticalNode`), `IEdge`/`AbstractEdge` (→ `SegmentedLineEdge`, `ShapeEdge`). Shared elements (notes, images, points, containers) in `product.diagram.common`. `ContainerNode` (background frame shown as "Grouping area" in the UI, available in all six diagrams; its tool bar prototype draws shapes via `enableToolIcon()`, not an image, since `DarkDiagramGraphics2D` only mirrors colors given to the graphics) is special-cased in `AbstractGraph` (drawn first largest→smallest, found last by `findNode`, never a parent, never connected); dragging it moves the top-level nodes inside it (`DragSelectedBehavior`). With its tool selected, press + drag on the canvas draws its frame (dedicated `AddContainerByFrameBehavior`, dashed preview; `DragSelectedBehavior` stays idle for this tool); a simple click (`AddNodeBehavior`) or a frame under 10 px creates it at its default size. Either way the tool bar then goes back to the selection tool. With this tool, a press on the selection (incl. anywhere inside a selected container) drags it instead (`BehaviorUtils.isCursorOnSelectedNode`). `FreeTextNode` ("Free text", all six diagrams) is its opposite: no border nor background, drawn upon every element including notes (Z just above them), found first by `findNode`, never a parent, never connected; its tool bar prototype draws a "T" via `enableToolIcon()`. `ImageNode` ("Image", all six diagrams) is always attached to the graph like notes, so it can overlap other nodes; without image (`image == null`, never persisted) it draws a vector placeholder, both in the tool bar and on the diagram. At the same Z, `AbstractGraph.draw` paints top-level nodes oldest first, so the most recent is on top and found first by `findNode`; children stay before their parent, which paints them again.
- **Plugins**: implement `IDiagramPlugin` (name, category, file extension, graph class) + list it in `src/main/resources/META-INF/services/com.horstmann.violet.framework.plugin.IDiagramPlugin`. Shade's `AppendingTransformer` merges those files — keep it, or only one plugin loads. Node/edge prototypes are built in a `static {}` block of each `*Graph` (tooltips freeze to the startup locale). Plugins keep declaring all their prototypes; `GraphToolsBar` splits them in the side bar: an "Outils communs" section at the top, then a section titled with the plugin name (menu prefix `1.` stripped) holding the diagram's own elements then links. The common section is a 4-column icon grid with short labels, on two rows: Select, `NoteNode`, `FreeTextNode`, `ImageNode`, `ContainerNode`; labels from `SideBar*.properties`, then `NoteEdge`. No paging: the side bar's own scroll pane scrolls.
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
- A click on the diagram name in the menu bar renames it (`FileMenu.renameActiveFile`, `GraphFile.rename`, `IFileChooserService.renameFile`). This label exists in all modes: FlatLaf's own title can't be clicked, so it is hidden (`TITLE_BAR_SHOW_TITLE`). On the desktop a diagram with a temporary name goes through "Save as", as it needs a real location. The browser version copies then deletes, the only operations known to work on `/files`. The desktop one uses `File.renameTo`: `Files.move` ignores a change of case on Windows, and writing the new name then deleting the old one would lose the file.
- The active diagram is remembered as soon as it changes or is saved (`MainFrame`), not only at exit: a reloaded web page never goes through `ApplicationStopper`. `DefaultUserPreferencesDao.put` flushes for the same reason, as `java.util.prefs` otherwise writes about every 30 s. At startup, read `getActiveDiagramFile()` before opening the files, since each opened diagram becomes the active one.
- File extensions: `.class.violet.html`, `.seq.violet.html`, `.activity.violet.html`, `.state.violet.html`, `.object.violet.html`, `.ucase.violet.html`.

## Themes (`framework.theme`)

- Two themes in practice: `FlatLightTheme` and `DarkBlueTheme` (the dark one, FlatDarkLaf + violet accents; the class kept its old name). The older themes stay installed but are only reachable through an old `look_and_feel` preference.
- Choice at startup (`ThemeManager.applyPreferedTheme`): `-theme=` argument, else `look_and_feel` preference, else the system theme (`-systemTheme=` argument, else `SystemThemeDetector`). An empty preference means "follow the system"; the switch at the right of `MenuBarToolBar` writes it (`MainFrame.toggleTheme`), and `-reset` clears it.
- Browser: `index.html` / `test.html` pass `-systemTheme=` from `prefers-color-scheme` and `-theme=` from `localStorage["violet.theme"]`, written by `nativeStoreTheme`. Dialog title bars and frames are DOM elements of CheerpJ (`.cjTitleBar`, `.cjBordered`), colored by the page CSS from `html[data-theme]`: keep them in sync with the themes' menu bar colors.
- Themes switch live, so a component with a custom UI must override `updateUI()` to keep it (see `SideBar`, `GraphToolsBarButton`), or `updateComponentTreeUI` replaces it by the default one. Read theme colors when painting or in `installUI`, not once in a constructor. Theme colors must be constants: never read them from Swing components in `configure()`, it runs under the previous look and feel.
- Diagram files keep their real colors. On screen, `ITheme.getDiagramGraphics` wraps the graphics (`DarkDiagramGraphics2D` mirrors lightness and keeps hue); exports and printing call `graph.draw` directly, so they stay light. Don't take a theme color inside a node's `draw`.
- Menu icons use `shape:<name>` in `MenuFactory*.properties` (`ToolShapes`, drawn by `ShapeIcon` in the menu text color), shared with the tool bar.

## CheerpJ (browser) mode

- Package `violetproduct-swing/.../application/cheerpj/`:
  `CheerpJInterfaceService` (Java `native` methods implemented in JS), `CheerpJFileChooserService`, `CheerpJFileReader`, `CheerpJStorageFileWriter`/`Deleter` (persist in CheerpJ's IndexedDB-backed FS), `CheerpJDownloadFileWriter` (browser download), `CheerpJFileMenuCustomizerCommand` (adjusts File menu in browser mode: Print → PDF tab, Exit hidden, adds "Import from > Diagram file" (local picker, copied into `/files`) and "Export to > Diagram file" (download); replaces **Open** with a Swing list of the diagrams in `/files` — newest first, trash icon / Delete key per line with confirm, refuses to delete a diagram open in a workspace since auto-save would recreate it).
- JS side: `src/main/resources/web/index.html`. Each native is a function named
  `Java_com_horstmann_violet_application_cheerpj_CheerpJInterfaceService_<method>` and **must also be listed in `cheerpjInit({natives:{...}})`**. Adding a native = Java `native` decl + static wrapper in `CheerpJInterfaceService` + JS function + `natives` entry.
- Launch args used by the page: `-kioskMode -autoSave -cheerpjMode -systemTheme=<light|dark> [-theme=<light|dark>] [-file=/str/<name>]`. Imported files are staged in `/str` (JS-writable, Java-readable); CheerpJ runs Java 17.
- Mobile layout: the page needs its `<meta name="viewport">` (else a 980 px virtual screen) and a `position: fixed` body at `100dvh` (else the address bar area makes the page scroll and hides the menu bar). A maximized frame doesn't follow the display, so once the JavaScript bridge is found, `CheerpJInterfaceService` polls the body size (`nativeGetDisplayWidth/Height`) and resizes every showing undecorated or maximized frame (window resize, screen rotation); nothing to call from the common code.
- Don't create directories at the VFS mount roots.
- No native `javajpeg` in CheerpJ: `ImageIO` lists JPEG writers/readers but they throw `UnsatisfiedLinkError`, and `new ImageIcon(jpeg)` never returns. So image export is PNG only (all versions, `FileNamingService.getImageExportExtensionFilter`), and on import the page turns the picked image into PNG bytes (`readImageAsPngBytes`) before staging it in `/str`, whatever the file extension.
- CheerpJ gives Java the physical key (`event.code`), not the letter, so shortcuts would depend on the keyboard layout (Ctrl+Z on AZERTY). `useLetterAsKeyCode` in `index.html` / `test.html` rewrites `code` from `event.key` for Ctrl/Cmd + letter.
- Ctrl+W belongs to the browser (closes the tab) and never reaches the page: in browser mode "Close" has no shortcut (`CheerpJFileMenuCustomizerCommand`; keep shortcuts identical to the desktop rather than adding a web-only one). The page's `beforeunload` handler asks before leaving or reloading **only while a diagram is unsaved** (waiting for its 2 s auto-save): `MainFrame` listens to each `IGraphFile` and calls `CheerpJInterfaceService.setUnsavedChanges` → native `nativeSetUnsavedChanges`. Never ask unconditionally: the user doesn't want the warning once everything is saved.
- Local test without packaging or web server: `mvn compile -Pcheerpj-test` (root dir) writes `violetproduct-swing/target/cheerpj-test/bundle.js` (all classes + jars, base64; generator `violetproduct-swing/src/test/cheerpj/BuildCheerpJTestBundle.java`), then open `web/test.html` via `file://`. `test.html` = copy of `index.html` with a different launch block — keep the JS bridge in sync. See README.
- `violetproduct-swing/CHEERPJ-INTEROP.md` is stale (uses old `...application.jni...` package name).

## CLI options (`framework.userpreferences.LaunchingPreferences`)

`-reset`, `-english`, `-help`/`-?`, `-kioskMode`, `-cheerpjMode`, `-adminPassword=…`, `-file=<path>`, `-theme=light|dark`, `-systemTheme=light|dark`.
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

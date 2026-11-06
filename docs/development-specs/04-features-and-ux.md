# 04 — Features and user experience

Functional catalogue of the application as it is today. It serves as the **parity reference**: each entry must be carried over, explicitly modified, or explicitly dropped in the target (see 07).

Evidence level: [V] = observed in the code; [D] = deduced from a name or structure; [?] = to confirm at runtime.

## 1. Launch modes

### 1.1 Command-line options [V]

Options actually parsed by `LaunchingPreferences`:

| Option | Effect | Remark |
|---|---|---|
| `-reset` | Resets user preferences | — |
| `-english` | Forces English | Acts before the interface is created |
| `-help` | Displays help then exits (`exit 0`) | — |
| `-kioskMode` | **Undecorated** window (`setUndecorated`); splash screen displayed for 0 ms instead of 1000 ms | The help text announces "no menu, no toolbar, no status bar": **the actual hiding of menus has not been verified** [?] |
| `-cheerpjMode` | Enables the browser's file chooser (see 1.2) | — |
| *free arguments* | Files to open at startup | The first argument is treated as a file [D] |

**Documentation anomalies [V]:**
- The help displays `-cheeprjMode` (wrong spelling): the code expects `-cheerpjMode`.
- The help announces `-autoSave`: **this option is not parsed**. Auto-save is always active (see 4.5).

### 1.2 "CheerpJ" browser mode [V]

The Swing product can run in a browser through **CheerpJ** (execution of Java bytecode via WebAssembly). In this mode:
- the file dialogs are replaced by the browser's native picker;
- opening reads the file that was sent (`CheerpJFileReader`);
- saving triggers a download (`CheerpJDownloadFileWriter`) or writes to the browser's storage (`CheerpJStorageFileWriter`, `CheerpJStorageFileDeleter`).

Files concerned: 8 classes in `com.horstmann.violet.application.cheerpj`, plus `index.html` (404 lines) in `src/main/resources/web/` [V]. This mode is **published on GitHub Pages** by the CI workflow (see 01, section 10).

> **Migration issue [D]:** this mode is the only "web" version in production. A native web target would advantageously replace it, but the "open / save in the browser" flow must be covered identically.

### 1.3 Applet mode [V]

`PreferencesServiceFactory` can return `AppletUserPreferencesDao` (preferences adapted to an applet). **Whether it is ever activated has not been traced** [?]. The applet code is not exposed as a product.

## 2. Main window and navigation

### 2.1 Structure [V]

- **Main window** `MainFrame` (404 lines): menu bar and work area [V]; toolbar and status bar [?] not verified.
- **Workspace** `Workspace`: each workspace contains a canvas (`EditorPart`), a side bar (`SideBar`) and a `GraphFile` [V]. Whether they are presented as tabs or as multiple windows has not been verified [?].
- **Welcome panel** (`WelcomePanel`) and **splash screen** (`SplashScreen`) [V].
- **Side bar** made of five panels (see 3).

### 2.2 Menus [V]

Menus: **File**, **Edit**, **View**, **Document**, **Help**, plus **Window** (`window.*` shortcuts). The file `MenuFactory.properties` contains 146 entries.

| Menu | Entries (keys) |
|---|---|
| File | `new`, `open`, `recent`, `save`, `save_as`, `close`, `export` (submenu: `export_to_image`, `export_to_clipboard`, `export_to_pdf`), `print`, `exit` |
| Edit | `undo`, `redo`, `cut`, `copy`, `paste`, `delete`, `select_all`, `select_next`, `select_previous`, `properties` |
| View | `zoom_in`, `zoom_out` |
| Document | 2 entries (not detailed here) |
| Help | `about`, `homepage`, `userguide`, `shortcut` |
| Window | `next`, `previous`, `close` |

**Menu entries not exposed [V]:**
- `file.export_to_java` and `file.export_to_python`: the listeners exist but **are empty** (no processing), and their addition to the menu is **commented out** (`FileMenu`, lines 209-210). **Feature not implemented.**
- `file.open_remote` (Ctrl+R): a shortcut defined in the properties, **no menu item** uses it. It remains from an old "peer-to-peer" mode (constants `NETWORK_GUESTCONFIG_*` never used) [V].

### 2.3 Keyboard shortcuts [V]

| Action | Shortcut | Action | Shortcut |
|---|---|---|---|
| Open | Ctrl+O | Undo | Ctrl+Z |
| Close | Ctrl+W | Redo | Ctrl+Y |
| Save | Ctrl+S | Properties | Ctrl+Enter |
| Quit | Ctrl+Q | Delete | Delete |
| Cut / Copy / Paste | Ctrl+X / C / V | Select all | Ctrl+A |
| Next / previous selection | Ctrl+→ / Ctrl+← | Zoom in / out | Ctrl+= / Ctrl+− |
| Next / previous window | Ctrl+Tab / Ctrl+Shift+Tab | | |

The tab keys (`TAB`, `shift TAB`) are also registered at the interface level [V]. The "Shortcuts" dialog (`ShortcutDialog`) shows an *Action / Shortcut* table (`dialog.table.*` keys); **the exact origin of its data has not been traced** [?].

## 3. Side bar

| Panel | Class | Role [D from the name] |
|---|---|---|
| Graph tools | `GraphToolsBarPanel` (591 lines) | One button per node and edge **prototype**, for adding by click or drag |
| Colours | `ColorToolsBarPanel`, `ColorChoice` | Choice of background, border and text colour |
| Thicknesses | `ThicknessToolsBarPanel` | Line thickness |
| Editing tools | `EditorToolsPanel` | Editing actions |
| Optional tools | `OptionalToolsPanel` | Additional options |
| Navigator | `ScrollPaneNavigatorPanel` | Overview of the diagram [D] |

Graph tools are **derived from the plugin prototypes**: adding a node type to a diagram automatically adds it to the bar (see 02, section 5).

## 4. Interaction behaviours

The canvas (`EditorPart`) delegates every mouse and keyboard event to a chain of **behaviours** (`EditorPartBehaviorManager`). **24 behaviours** are registered in `Workspace` [V]. Undo actions are separate behaviours (see 02, section 7).

### 4.1 Selection and navigation

| Behaviour | Function [D from the name and usage] |
|---|---|
| `SelectByClickBehavior` | Selection of an element by click |
| `SelectByDistanceBehavior` | Selection of an element close to the cursor |
| `SelectByLassoBehavior` | Rectangle (lasso) selection |
| `SelectAllBehavior` | Select everything |
| `DragGraphBehavior` | Move the diagram (scrolling) |
| `ZoomByWheelBehavior` | Zoom with the mouse wheel |
| `ChangeToolByWeelBehavior` | Change tool with the mouse wheel |

### 4.2 Creation

| Behaviour | Function |
|---|---|
| `AddNodeBehavior` | Add a node according to the selected tool (click or drag) |
| `AddEdgeBehavior` | Draw an edge between two nodes (checks: non-null ends, tool type) |
| `AddTransitionPointBehavior` | Add a transition point on an edge |

### 4.3 Modification

| Behaviour | Function |
|---|---|
| `DragSelectedBehavior` | Move selected elements |
| `DragTransitionPointBehavior` | Move a transition point |
| `ResizeNodeBehavior` | Resize a node |
| `CropNodeBehavior` | Crop a node (e.g. image) |
| `ColorizeBehavior` | Apply a colour |
| `ChangeThicknessBehavior` | Change thickness |
| `EditSelectedBehavior` | Edit the content (text, properties) of the selected element |
| `CutCopyPasteBehavior` | Cut, copy, paste (including between diagrams) |

### 4.4 Context menus

| Behaviour | Function |
|---|---|
| `ShowMenuOnRightClickBehavior` | Context menu on right click |
| `ResetGraphToolBarOnRightClickBehavior` | Right click: reset of the graph toolbar [D from the name] |

### 4.5 File and saving

| Behaviour | Function |
|---|---|
| `AutoSaveBehavior` | **Automatic saving**: see below |
| `FileCouldBeSavedBehavior` | Tracking of the file's "modified" state |
| `SwingRepaintingBehavior` | Triggers repainting after a change |

**Automatic saving [V]:**
- Always active (added unconditionally to every workspace).
- After **2 seconds of inactivity** (`AUTO_SAVE_IDLE_MILLIS = 2000`) and only if the diagram is modified (`isSaveRequired`), calls `graphFile.save(false)`.
- A **JVM shutdown hook** (`Runtime.addShutdownHook`) triggers one last save.
- After each save, the file is added to the opened and recent files of the preferences.
- The code comment states that auto-save writes to a **temporary location** when the file does not yet have a final location [V].

> **Anomaly [D]:** each workspace adds a shutdown hook, which is never removed. Opening and closing diagrams accumulates hooks (minor leak).

### 4.6 Undo and redo

Nine classes (one composite, eight specific), see 02, section 7. Coverage: addition, colour, cropping, moving, editing, deletion, resizing, transition point.

## 5. Property editing

Each element exposes its fields to a Swing editing interface (`CustomPropertyEditor`, 411 lines):

| Editor | Edited type |
|---|---|
| `MultiLineStringEditor` (395 lines) | Multi-line text with HTML |
| `ImageEditor` / `ImageIconEditor` | Imported image |

Multi-line text accepts HTML when it starts with `<html>` (see 02, section 4.4).

## 6. Export and printing

| Output | Access | Mechanism | Status |
|---|---|---|---|
| PNG image | File → Export → Image | Java2D rendering, `ImageIO` | Implemented [V] |
| Clipboard | File → Export → Clipboard | Image transfer | Implemented [V] |
| PDF | File → Export → PDF | FreeHEP `PDFGraphics2D`, PDF version 6, embedded fonts, page sized to the diagram | Implemented [V] |
| Printing | File → Print | `java.awt.print` | Implemented [V] |
| Java code | Commented menu item | — | **Not implemented** [V] |
| Python code | Commented menu item | — | **Not implemented** [V] |
| `.violet.html` file | File → Save | See 03 | Implemented |
| Online editor | Button in the file | See 03, section 6 | Implemented |

The internal clipboard (copy/paste between diagrams) is distinct from the export to the system clipboard: the former carries **objects** (`CutCopyPasteBehavior`, `Transferable`) [V].

## 7. Preferences and recent files

| Data | Storage | Source |
|---|---|---|
| User preferences | `java.util.prefs`, node = package of `DefaultUserPreferencesDao` | [V] |
| Opened files | List updated at each save | `AutoSaveBehavior` [V] |
| Recent files | "Recent" submenu | `file.recent` [V]; fed by `addRecentFile` [V] |
| Theme | Remembered choice | `ThemeManager` reads and writes the preferred look-and-feel through `UserPreferencesService` [V] |

**Consequence for the migration [D]:** `java.util.prefs` stores data in the Windows registry, in `.plist` files on macOS, or in `~/.java` on Linux. A target must define an equivalent location and **migrate** existing preferences, otherwise users' settings are reset.

## 8. Themes and appearance

- Themes offered: `BasicTheme`, `FlatLightTheme`, `BlueAmbianceTheme`, `ClassicMetalTheme`, `DarkAmbianceTheme`, `DarkBlueTheme` [V].
- Theme applied at startup (`ThemeManager`).
- The file format does **not** embed the theme: a diagram is displayed with the user's theme [D].

## 9. Help and external resources

| Item | Target | Status |
|---|---|---|
| User guide | `http://violet.sourceforge.net/help.php?lang=<fr|en|de>` | External, three languages [V] |
| Home page | `http://violet.sourceforge.net/index.php?lang=<…>` | External [V] |
| About | `AboutDialog` (400 lines): version, release date (injected by CI), licence | Local [V] |
| Opening the browser | `BrowserLauncher`: `rundll32` (Windows), Unix commands (`Runtime.exec`) | Local [V] |

> **Minor security point [V]:** URL opening goes through `Runtime.exec` with **argument arrays** (no intermediate shell), which limits command injection. The URL comes from the code, not from the user.

## 10. Internationalisation as seen by the user

| Language | Interface | Tool tooltips | Help |
|---|---|---|---|
| English | Yes (default) | Yes | Yes |
| French | Yes | Yes | Yes |
| German | Yes | Yes | Yes |
| Polish | Partial (3 localisation `.properties` files) | Partial [D] | **No**: only `en`, `fr`, `de` URLs exist [V] |

The language follows the system language; `-english` forces English. Changing language while the application runs is not possible without restarting (see 01, section 7).

## 11. Summary of features to carry over or decide

| # | Feature | Decision for the target |
|---|---|---|
| F-1 | Editing of 6 diagram types | Mandatory carry-over |
| F-2 | Saving / opening `.violet.html` | Mandatory carry-over (compatibility, 03) |
| F-3 | Automatic saving (2 s, shutdown) | Carry over; fix the hook leak |
| F-4 | Undo / redo | Carry over; redefine as a command model |
| F-5 | PNG, PDF, clipboard export, printing | Mandatory carry-over |
| F-6 | **Java / Python code export** | **Not implemented**: drop or implement (decision) |
| F-7 | Online editor (diagram transfer) | Decide: keep, replace or remove (03, section 6) |
| F-8 | CheerpJ browser mode | Replace with a native web version or drop |
| F-9 | Dark and light themes | Carry over, desirable |
| F-10 | Internationalisation (4 languages) | Carry over; reuse the existing translations |
| F-11 | Preferences and recent files | Carry over with preference migration |
| F-12 | CLI options | Carry over; fix the help (`-cheerpjMode`, `-autoSave`) |
| F-13 | Kiosk mode | Carry over, to confirm [?] |
| F-14 | Applet / peer-to-peer mode | **Dead code**: drop (05) |

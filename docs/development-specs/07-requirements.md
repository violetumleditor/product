# 07 — Functional and non-functional requirements

Requirements written independently of the target. Each requirement has a stable identifier, an **origin** in the previous documents, and a verifiable **acceptance criterion**. Open decisions are marked **[DECISION]**.

Priorities: **M** = mandatory (parity or compatibility); **S** = desirable; **C** = to be decided.

## 1. Functional requirements

### 1.1 Model and diagrams

| ID | Requirement | Origin | Prio | Acceptance criterion |
|---|---|---|:---:|---|
| FR-MOD-01 | The system supports six diagram types: class, sequence, activity, state, object, use case | 02, §5 | M | Each type can be created, edited, saved and reopened |
| FR-MOD-02 | Each type exposes the nodes and edges listed in §5 of 02 | 02, §5 | M | Toolbar compared with the table in 02 |
| FR-MOD-03 | Each node and edge has an identifier unique within the diagram | 02, I-1 | M | No duplicate after 1,000 random operations |
| FR-MOD-04 | An edge has two non-null ends | 02, I-2 | M | Attempt to connect without an end: explicit error, no crash |
| FR-MOD-05 | An activation bar can only be placed on a lifeline | 02, 6.2 | M | Placement off a lifeline refused |
| FR-MOD-06 | A transition from a decision follows the imposed routing style | 02, 6.1 | M | `AUTO` routing towards a synchronisation bar, `HV` otherwise |
| FR-MOD-07 | Nodes can be nested (a package containing classes) | 02, §3.1 | M | Moving the parent moves the children |
| FR-MOD-08 | **[DECISION]** Validation of link types (none today) | 02, 6.3 | C | If adopted: incompatible links refused with a message |

### 1.2 Editing and interactions

| ID | Requirement | Origin | Prio | Acceptance criterion |
|---|---|---|:---:|---|
| FR-EDT-01 | Selection by click, by lasso, by proximity and select-all | 04, §4.1 | M | Acceptance scenarios SC-01 to SC-04 (§3) |
| FR-EDT-02 | Adding a node by click or by dragging from the toolbar | 04, §4.2 | M | SC-05 |
| FR-EDT-03 | Drawing an edge between two nodes, with waypoints | 04, §4.2 | M | SC-06 |
| FR-EDT-04 | Moving, resizing, clipping | 04, §4.3 | M | SC-07 |
| FR-EDT-05 | Background, border and text colours; line widths | 04, §3 and §4.3 | M | SC-08 |
| FR-EDT-06 | Editing of content (text, properties) by double-click or `Ctrl+Enter` | 04, §4.3 and 2.3 | M | SC-09 |
| FR-EDT-07 | Cut, copy, paste, including between two diagrams | 04, §4.3 and §6 | M | SC-10 |
| FR-EDT-08 | Context menu on right-click | 04, §4.4 | M | SC-11 |
| FR-EDT-09 | Zoom with the mouse wheel and shortcuts (`Ctrl+=`, `Ctrl+−`) | 04, §2.3 and §4.1 | M | SC-12 |
| FR-EDT-10 | Alignment grid: always displayed | 04, §2.2 | M | Grid visible, no effect on the saved file (FR-FIC-11) |
| FR-EDT-11 | Text editing with limited HTML formatting (tags from 05, §2.1) | 02, §4.4 | M | Text starting with `<html>…` displayed with formatting |
| FR-EDT-12 | Undo and redo of all actions in §1.2 | 02, §7; 04, §4.6 | M | Property P-01 (§4) |

### 1.3 Files and persistence

| ID | Requirement | Origin | Prio | Acceptance criterion |
|---|---|---|:---:|---|
| FR-FIC-01 | Open a `.violet.html` file of each type | 03, §4 | M | Corpus: read without error (CA-01) |
| FR-FIC-02 | Save in `.violet.html` format readable by the current application | 03, §4.4 | M | Cross test (CA-02) |
| FR-FIC-03 | Open 0.16 files (`com.horstmann.violet.*` names) | 03, §4.2 and §5 | M | 0.16 corpus files read (CA-03) |
| FR-FIC-04 | Open files written by XStream (older versions) | 03, §4.4 | M | `sample.class.violet.html` read (CA-04) |
| FR-FIC-05 | Save and open a bare XML file (without HTML wrapper) | 03, §4.4 | S | Round trip |
| FR-FIC-06 | Reject files containing a DOCTYPE | 03, §9 | M | File with DOCTYPE refused with an error |
| FR-FIC-07 | Auto-save after 2 s of inactivity if modified, and on closing | 04, §4.5 | M | SC-13 |
| FR-FIC-08 | Tracking of the "modified" state; confirmation before closing an unsaved diagram | 04, §4.5 | M | SC-14 |
| FR-FIC-09 | List of open and recent files, updated at each save | 04, §7 | M | SC-15 |
| FR-FIC-10 | An imported image is stored only once even if it appears several times | 03, R-21 | M | Only one `<image>` in the file |
| FR-FIC-11 | The alignment grid is **not** saved in the file | 02, §3.4 | M | Content check |
| FR-FIC-12 | **[DECISION]** Thumbnail generated at each save, or only if modified | 03, §7; 05, A-7 | S | Performance measurement (NFR-PERF-02) |

### 1.4 Export and printing

| ID | Requirement | Origin | Prio | Acceptance criterion |
|---|---|---|:---:|---|
| FR-EXP-01 | PNG export of the whole diagram | 04, §6 | M | Image comparable to the corpus (tolerance VIS-01) |
| FR-EXP-02 | PDF export, embedded fonts, page sized to the diagram | 04, §6 | M | Opens in a PDF reader; identical dimensions |
| FR-EXP-03 | Copy of the image to the clipboard | 04, §6 | M | Pastes into another application |
| FR-EXP-04 | Printing of the diagram | 04, §6 | M | Print preview matches |
| FR-EXP-05 | Export to Java and Python code | 04, F-6 | **C** | **[DECISION]**: not implemented today |
| FR-EXP-06 | SVG export | — | S | New feature, outside parity |

### 1.5 Interface and customisation

| ID | Requirement | Origin | Prio | Acceptance criterion |
|---|---|---|:---:|---|
| FR-UI-01 | File, Edit, View, Document, Help, Window menus, with the entries of 04, §2.2 | 04, §2.2 | M | Inventory compared |
| FR-UI-02 | Keyboard shortcuts of 04, §2.3 | 04, §2.3 | M | SC-16 |
| FR-UI-03 | "Shortcuts" dialog listing actions and shortcuts | 04, §2.3 | M | Complete list on screen |
| FR-UI-04 | Welcome screen and splash screen | 04, §2.1 | S | Displayed at startup |
| FR-UI-05 | Choice of theme (light, dark) remembered | 04, §8 | M | Theme applied on restart |
| FR-UI-06 | Full-screen mode without decoration (kiosk) | 04, §1.1 | S | `-kioskMode` option (to confirm, see 04) |
| FR-UI-07 | Online help in French, English, German | 04, §9 | M | Links valid |
| FR-UI-08 | Open the user guide in the browser | 04, §9 | M | Link opened |

### 1.6 Internationalisation

| ID | Requirement | Origin | Prio | Acceptance criterion |
|---|---|---|:---:|---|
| FR-I18N-01 | Interface and tooltips in English, French, German, Polish | 01, §7 | M | No missing key in any language (CA-05) |
| FR-I18N-02 | The language follows the system language; `-english` forces it | 01, §4 and 04, §10 | M | SC-17 |
| FR-I18N-03 | Language change without restart | 01, §7 (point of attention) | S | SC-18 |
| FR-I18N-04 | Help in Polish | 04, §10 | C | **[DECISION]**: not available today |

### 1.7 Preferences and options

| ID | Requirement | Origin | Prio | Acceptance criterion |
|---|---|---|:---:|---|
| FR-PRF-01 | User preferences are kept between sessions | 04, §7 | M | SC-19 |
| FR-PRF-02 | Automatic migration of preferences from the current version | 06, R-7 | M | SC-20 |
| FR-PRF-03 | `-reset` option: resets preferences | 04, §1.1 | M | SC-21 |
| FR-PRF-04 | CLI options: `-english`, `-help`, `-kioskMode`, `-cheerpjMode` (or target equivalent) | 04, §1.1 | M | Correct help (fixes A-9) |
| FR-PRF-05 | Opening one or several files passed as arguments | 04, §1.1 | M | SC-22 |

### 1.8 Browser mode and integration

| ID | Requirement | Origin | Prio | Acceptance criterion |
|---|---|---|:---:|---|
| FR-WEB-01 | Use in a browser without installation | 04, §1.2 | **C** | **[DECISION]**: replacement of CheerpJ by a native version |
| FR-WEB-02 | Open and save files through the browser's dialogs | 04, §1.2 | M if FR-WEB-01 | SC-23 |
| FR-WEB-03 | Transfer of a diagram to the online editor | 03, §6 | **C** | **[DECISION]**: keep, replace or remove |

## 2. Non-functional requirements

| ID | Category | Requirement | Measure | Target value |
|---|---|---|---|---|
| NFR-PERF-01 | Performance | Opening a 500-element diagram | Median time over 10 runs | ≤ that of the current version, **[to measure in phase 0]** |
| NFR-PERF-02 | Performance | Saving a 500-element diagram | Median time | Same |
| NFR-PERF-03 | Performance | Response to a move (500-element diagram) | Frames per second | ≥ 30 fps **[to validate]** |
| NFR-PERF-04 | Performance | 2,000-element diagram: opening and editing | No freeze > 1 s | **[to validate]** |
| NFR-REL-01 | Reliability | No data loss on closing | Forced-closing test | Auto-save preserved |
| NFR-REL-02 | Reliability | No crash on malformed file | Reader fuzzing | Explicit error, no unhandled exception |
| NFR-COMP-01 | Compatibility | Reading files from all known versions | Corpus | 100 % of corpus files |
| NFR-COMP-02 | Compatibility | Writing readable by the current application | Cross test | 100 % of written files |
| NFR-COMP-03 | Compatibility | Platforms: Windows, Linux, macOS (desktop); modern browser | Tests per platform | **[DECISION on macOS, not delivered today]** |
| NFR-SEC-01 | Security | No external XML entity | XXE test | Refused |
| NFR-SEC-02 | Security | No network call without user action | Traffic analysis | No automatic call |
| NFR-SEC-03 | Security | No signing key in the repository | Review | Key removed (S-5) |
| NFR-SEC-04 | Security | Transfer to an external service: origin verified in both directions | Protocol review | `event.origin` checked if the transfer is kept |
| NFR-A11Y-01 | Accessibility | Keyboard navigation of all menu actions | Manual test | All entries reachable |
| NFR-A11Y-02 | Accessibility | Compliant contrast in light and dark themes | Measurement | **[to define: WCAG AA]** |
| NFR-MAINT-01 | Maintainability | No dead code (05, §3) | Static analysis | 0 unreferenced class |
| NFR-MAINT-02 | Maintainability | Repository documentation consistent with the code | Review | No discrepancy listed in 00, §5 |
| NFR-MAINT-03 | Maintainability | Reproducible build | Offline build with cache | Success |
| NFR-LIC-01 | Licence | GPLv2+ licence of the project respected; dependencies compatible | Inventory | Licence report |
| NFR-PKG-01 | Delivery | Signed Windows installer | Signature check | Signed |
| NFR-PKG-02 | Delivery | Installable Debian package | `apt install` on Debian | Successful installation |

## 3. Acceptance scenarios

Each scenario is run on the current version **and** on the target. Accepted differences are recorded.

| ID | Scenario | Expected result |
|---|---|---|
| SC-01 | Click on a node | Node selected, handles visible |
| SC-02 | Lasso around three nodes | All three selected |
| SC-03 | Selection by proximity | Closest element selected |
| SC-04 | Select all (`Ctrl+A`) | All elements selected |
| SC-05 | Drag "Class" from the toolbar to the canvas | `ClassNode` created at the drop position |
| SC-06 | Draw an association from A to B | Edge created, ends correct |
| SC-07 | Resize a node by its handle | Size changed, content clipped if needed |
| SC-08 | Colour a node red | Colour applied and saved |
| SC-09 | Double-click a class, change the name | Name updated in the diagram |
| SC-10 | Copy a class, paste into another diagram | Copy present, without a shared identifier |
| SC-11 | Right-click a node | Context menu displayed |
| SC-12 | `Ctrl+=` then `Ctrl+−` | Zoom increased then decreased |
| SC-13 | Modify a diagram and wait 3 s | File saved without action |
| SC-14 | Close a modified, unsaved diagram | Confirmation requested |
| SC-15 | Save two files, check "Recent" | Both appear |
| SC-16 | Each shortcut of 04, §2.3 | Action triggered |
| SC-17 | System in French, no option | Interface in French |
| SC-18 | Change the language while running | Interface updated (**[S]**) |
| SC-19 | Change a preference, restart | Preference kept |
| SC-20 | Preferences of the current version present | Migrated at first run |
| SC-21 | Launch with `-reset` | Preferences reset |
| SC-22 | Launch with a file as argument | File opened |
| SC-23 | Open a file through the browser dialog (FR-WEB-02) | Diagram loaded |

## 4. Properties to verify

| ID | Property | Method |
|---|---|---|
| P-01 | For any sequence of undoable actions, undoing all actions then redoing them returns to the same state | Random generation of actions |
| P-02 | Reading then writing a file does not change the model | Corpus and generation |
| P-03 | Writing then reading a diagram produces an equivalent model | Random generation |
| P-04 | No identifier appears twice in a diagram | Random generation |

## 5. Conformity criteria (CA)

| ID | Criterion |
|---|---|
| CA-01 | 100 % of corpus files read without error |
| CA-02 | 100 % of files written, re-read by the current application, models compared |
| CA-03 | 100 % of 0.16 corpus files read |
| CA-04 | Reference XStream file read |
| CA-05 | No missing translation key for the delivered languages |
| VIS-01 | PNG rendering compared to the corpus: mean deviation ≤ **[threshold to define]** per channel, in pixels |

## 6. Out of scope (proposed)

To be confirmed with the sponsor:
- Eclipse module (05, §3);
- JNLP and RPM modules (disabled);
- applet and peer-to-peer modes (dead code);
- Java and Python code export (not implemented);
- communication plugin (absent).

## 7. Delivery priority order

1. Reading and writing the format, on the corpus (FR-FIC-01 to 06, CA-01 to 04).
2. Editing the six diagrams: selection, addition, movement, drawing (FR-MOD, FR-EDT-01 to 07).
3. Undo and redo (FR-EDT-12, P-01).
4. PNG, PDF, clipboard and printing exports (FR-EXP).
5. Menus, shortcuts, auto-save, recent files (FR-UI, FR-FIC-07 to 09).
6. Internationalisation and preferences (FR-I18N, FR-PRF).
7. Themes and help (FR-UI-05, FR-UI-07).
8. Optional decisions (FR-WEB, FR-EXP-05, FR-I18N-04).

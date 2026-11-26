<p align="center">
  <img src="docs/images/violet-logo.png" alt="Violet logo" width="96" height="96">
</p>

<h1 align="center">Violet UML Editor</h1>

<p align="center">
  <b>Free, open-source and lightweight UML editor. Draw a diagram in seconds, on the desktop or in your browser.</b>
</p>

<p align="center">
  <a href="https://violetumleditor.github.io/product"><b>Open online</b></a> ·
  <a href="https://sourceforge.net/projects/violet/">Download desktop version</a> ·
  <a href="https://violetumleditor.github.io/">Website</a> ·
  <a href="https://violetumleditor.github.io/docs/">User guide</a> ·
  <a href="https://github.com/violetumleditor/product/issues">Report an issue</a>
</p>

<p align="center">
  <img src="docs/images/violet-hero.gif" alt="Violet drawing a class diagram, then a sequence diagram, then an activity diagram" width="800">
</p>


What is Violet?
===============

Violet is a UML editor written in Java (Swing) and designed to be **simple and fast**: pick an element in the side bar, drop it on the diagram, link it to another one. No project to configure, no model repository, no learning curve. It is the editor you open to sketch a design in a meeting, illustrate a course or document a piece of code.

+ **Six UML diagrams**: class, sequence, activity, state, object and use case, plus a simple **BPMN process diagram**.
+ **One JAR, two ways to run it**: as a desktop application (Windows, macOS, Linux), or **directly in a web browser** with nothing to install, thanks to [CheerpJ](https://cheerpj.com/).
+ **Diagrams are HTML files** (`*.violet.html`): anyone can open them in a browser to see the picture, and reopen them in Violet to edit them (see [below](#diagrams-are-html-files)).
+ **Touch friendly**: the same interface works with a mouse, a tablet or a phone.
+ Light and dark themes, English, French and German user interface, export to PNG and PDF, printing, undo/redo, auto-save.

Violet was originally created by Cay S. Horstmann and is distributed under the GNU General Public License, version 2 or later.


The workspace
=============

![Annotated Violet workspace](docs/images/workspace-annotated.png)

| # | Element | What it does |
|---|---------|--------------|
| 1 | **Menu bar** | File, Edit, View, Document and Help menus. On a narrow screen, all the menus collapse into a single "hamburger" menu. |
| 2 | **Diagram name** | Name of the active file. Click it to rename the diagram. |
| 3 | **Tool bar** | Undo/redo, cut/copy/paste, delete, zoom, light/dark theme switch, full screen. Buttons move into a "⋯" menu when space runs out. |
| 4 | **Tool search** | Type a few letters to filter the tools of the side bar. |
| 5 | **Common tools** | Elements available in every diagram (selection, note, free text, image, grouping area, marker) and five generic links (line, arrow, double arrow, dotted, bent). |
| 6 | **Diagram tools** | Elements and UML relationships provided by the plugin of the active diagram (here: class, interface, package, dependency, inheritance…). |
| 7 | **Style** | Color palette and line thickness, applied to the selection. It also shows the style of the selected element. |
| 8 | **Side bar handle** | Collapses the side bar into a narrow rail of icons (done automatically under 1000 px wide). |
| 9 | **Graph (diagram canvas)** | The diagram itself: drag elements, draw a lasso, double-click (or double-tap) and drag to move the view, Ctrl + wheel to zoom. |
| 10 | **Selection buttons** | Around the selected element: the pencil opens its properties, the arrow draws a link to another element (or creates a new one where you release it). |

Other handy gestures: type on a selected element to rename it, use the arrow keys to move it, tap an empty spot then the "+" to add an element there.


Diagrams are HTML files
=======================

Violet saves every diagram as a single, self-contained **`.violet.html`** file (`.class.violet.html`, `.seq.violet.html`, `.activity.violet.html`, `.state.violet.html`, `.object.violet.html`, `.ucase.violet.html`, `.bpmn.violet.html`). Opened in any browser, with no plugin and no Violet installed, such a file is a small **diagram viewer**, which works offline, even from a local file:

+ the diagram fills the window, on a dotted grid: drag to move it, zoom with the wheel, a pinch or the `+` / `-` keys, double-click (or press `0`) to fit it to the window;
+ a floating bar shows the file name and the kind of diagram, the zoom, a light/dark theme switch (the page follows the system theme and remembers your choice) and a full screen button;
+ **Edit online** opens the diagram in the online version of Violet (see below), and **Get Violet. It's free!** leads to the website. The bottom of the page tells which version of Violet made the file.

Behind the viewer, the file contains:

+ two **PNG pictures** of the diagram, for the light and the dark theme, embedded in the page as base64 data URIs;
+ the **diagram itself, as XML**, in a `<script id="content" type="text/xml">` block. This is what Violet reads when the file is reopened, so nothing is lost.

You can send a diagram by e-mail, attach it to a ticket or commit it next to your code: the recipient sees it immediately, and it diffs reasonably well in Git. Files written by older versions of Violet are still read (backward compatibility is a hard requirement of the project).

Edit a diagram you received, in one click
-----------------------------------------

When you receive a `.violet.html` file, just open it in your browser and click **Edit online**. The page opens the online version of Violet (`https://violetumleditor.github.io/product`, with the version of Violet that saved the file) in a new tab and **sends it the diagram** (file name, XML content and the whole HTML page) with the browser's `window.postMessage()` API, until the editor acknowledges it. The editor checks the origin of the message, stores the diagram in its own browser storage and opens it, ready to edit. Nothing is uploaded to a server: the transfer happens between the two tabs of your browser. (If messages are filtered, the diagram is passed through `window.name` instead.)

The implementation lives in [`XHTMLFileTemplate.violet.html`](violet-framework/src/main/java/com/horstmann/violet/framework/file/persistence/XHTMLFileTemplate.violet.html) (the viewer and the sender, template of every saved file) and [`web/index.html`](violetproduct-swing/src/main/resources/web/index.html) (the receiver).

> Try it: [`framework.class.violet.html`](docs/images/framework.class.violet.html), the class diagram below, is such a file. Download it, open it in your browser, and click **Edit online**.


The framework
=============

Violet is split into a generic **framework** (`violet-framework`) and **one plugin per diagram type**. The framework knows nothing about classes, lifelines or activities: it handles graphs made of nodes and edges, their display and editing, the files and the user interface. The class diagram below, drawn with Violet itself, shows its main abstractions.

![Class diagram of the Violet framework, drawn with Violet](docs/images/framework-class-diagram.png)

+ **Model** (blue, package `com.horstmann.violet.product.diagram.abstracts`): an `IGraph` holds `INode`s and `IEdge`s and draws itself. `AbstractGraph`, `AbstractNode` / `RectangularNode` / `EllipticalNode` and `AbstractEdge` / `SegmentedLineEdge` do most of the work, so a diagram type only describes its own shapes and rules. Elements shared by all diagrams (note, free text, image, grouping area, marker, generic link) are in `product.diagram.common`.
+ **Workspace** (green, package `com.horstmann.violet.workspace`): an `IWorkspace` is one open diagram. It combines the `IEditorPart` (the canvas), the `ISideBar` (tools built from the node and edge prototypes of the graph) and the `IGraphFile`. Every interaction on the canvas (select, drag, add a node, draw a link, undo, auto-save…) is a separate `IEditorPartBehavior` plugged into the editor part (see [Editor behaviors](#editor-behaviors)).
+ **Files** (orange, package `com.horstmann.violet.framework.file`): an `IGraphFile` reads and writes its graph through an `IFilePersistenceService`. The default one, `XHTMLPersistenceService`, writes the `.violet.html` format described above. File dialogs go through `IFileChooserService`, with one implementation for the desktop and one for the browser.
+ **Plugins** (violet): see next section.

The framework also brings its own small dependency injection engine ("Manioc": `@ManagedBean`, `@InjectedBean`, `BeanInjector`), themes (FlatLaf light and dark), property editors and internationalization (`.properties` files next to the Java sources).


Editor behaviors
================

Everything the user does on the diagram goes through **behaviors**. The canvas (`EditorPart`) contains almost no interaction code: it receives the Swing mouse events and paints the graph, and hands everything else to a list of small, independent objects implementing `IEditorPartBehavior` (package `com.horstmann.violet.workspace.editorpart.behavior`). Selecting, dragging, adding a node, drawing a link, zooming, undoing, auto-saving: each of these is one behavior class, from a few dozen to a few hundred lines, that can be read, fixed or replaced on its own.

How it works
------------

```
 Swing events          EditorPart                EditorPartBehaviorManager            behaviors (in order)
 ────────────          ──────────                ─────────────────────────            ────────────────────
 mouse pressed  ───►   fireOnMousePressed  ───►  for each behavior:          ───►   EditButtonBehavior
 mouse dragged                                     behavior.onMousePressed(e)        AddEdgeByDragBehavior
 mouse released                                    stop if e.isConsumed()            DragGraphBehavior
 wheel, click…                                                                       SelectByClickBehavior
                                                                                     AddNodeBehavior
 paint          ───►   image of the graph    ───►  behavior.onPaint(g2)              DragSelectedBehavior
                       (redrawn only if it                                           …
                       changed), then every
                       onPaint(g2)
                                                                                     AutoSaveBehavior
 a behavior     ───►   fireBeforeAddingNode… ───►  behavior.beforeAddingNodeAtPoint  UndoRedoCompoundBehavior
 changes the           fireAfterAddingNode…        behavior.afterAddingNodeAtPoint   …
 graph
```

+ **One manager per diagram.** Each `Workspace` creates its `EditorPart` and registers its behaviors in its `IEditorPartBehaviorManager` (see `Workspace.getEditorPart()`). The manager simply calls every behavior, **in the order they were added**.
+ **Input events.** `onMousePressed`, `onMouseDragged`, `onMouseReleased`, `onMouseClicked`, `onMouseMoved`, `onMouseWheelMoved`, plus `onToolSelected` when the user picks another tool in the side bar. Each behavior decides by itself whether the event concerns it (selected tool, modifier keys, what lies under the mouse…) and ignores it otherwise.
+ **The order matters.** A behavior that handles a mouse button event can call `event.consume()`: the manager then stops and the following behaviors never see it. This is how the pencil and link buttons drawn around the selection (`EditButtonBehavior`, `AddEdgeByDragBehavior`, registered first) keep their clicks from also selecting or moving the element below them.
+ **Painting.** After drawing the graph, the editor calls `onPaint(g2)` on every behavior, in diagram coordinates. Behaviors use it to draw their own feedback over the diagram: the lasso rectangle, selection handles, the dashed outline of a drop target, the "+" button, the edge being drawn… The graph itself is usually not drawn again: see [How the diagram is drawn](#how-the-diagram-is-drawn).
+ **Change notifications.** When a behavior modifies the graph, it surrounds the change with a pair of `before…`/`after…` notifications sent through the manager: `beforeAddingNodeAtPoint` / `afterAddingNodeAtPoint`, `beforeAddingEdgeAtPoints`, `beforeEditingNode`, `beforeRemovingSelectedElements`, `beforeChangingTransitionPointsOnEdge`, `beforeChangingColorOnElement`… Other behaviors react to them without knowing who made the change. For instance `UndoRedoCompoundBehavior` records an undoable edit for each pair, and `AutoSaveBehavior` / `FileCouldBeSavedBehavior` mark the file as modified and save it 2 seconds later.
+ **Behaviors can find each other** with `getBehaviors(Class)`, for the rare cases where one needs another (the pencil button opens the properties dialog of `EditSelectedBehavior`).

The main behaviors
------------------

| Family | Behaviors |
|--------|-----------|
| Selection | `SelectByClickBehavior`, `SelectByLassoBehavior`, `SelectByDistanceBehavior`, `SelectAllBehavior` |
| Creation | `AddNodeBehavior` (drop a tool on the diagram), `AddNodeByTapBehavior` (the "+" on an empty spot), `AddContainerByFrameBehavior`, `AddEdgeBehavior`, `AddEdgeByDragBehavior` (link handle), `AddTransitionPointBehavior` |
| Moving and resizing | `DragSelectedBehavior` (incl. dropping a class into a package), `DragTransitionPointBehavior`, `MoveSelectedByKeyboardBehavior`, `ResizeNodeBehavior`, `CropNodeBehavior`, `DragGraphBehavior` (move the view), `ZoomByWheelBehavior` |
| Editing | `EditButtonBehavior` (pencil), `EditSelectedBehavior` (properties dialog), `TypeTextOnSelectedNodeBehavior` (type to rename), `CutCopyPasteBehavior`, `ColorizeBehavior`, `ChangeThicknessBehavior`, `ShowSelectionStyleBehavior` |
| Menus and keys | `ShowMenuOnRightClickBehavior`, `ResetGraphToolBarOnRightClickBehavior`, `EscapeKeyBehavior` |
| Undo / redo | `UndoRedoCompoundBehavior`, which owns one sub-behavior per kind of change: `UndoRedoOnAddBehavior`, `UndoRedoOnDragBehavior`, `UndoRedoOnEditBehavior`, `UndoRedoOnRemoveBehavior`, `UndoRedoOnColorizeBehavior`, `UndoRedoOnResizeBehavior`… |
| File and display | `FileCouldBeSavedBehavior`, `AutoSaveBehavior`, `SwingRepaintingBehavior` |

Writing a behavior
------------------

Extend `AbstractEditorPartBehavior`, which implements every method of the interface with an empty body, and override only what you need. Here is the whole zoom behavior:

```java
public class ZoomByWheelBehavior extends AbstractEditorPartBehavior
{
    private IEditorPart editorPart;

    public ZoomByWheelBehavior(IEditorPart editorPart)
    {
        this.editorPart = editorPart;
    }

    @Override
    public void onMouseWheelMoved(MouseWheelEvent event)
    {
        if (!KeyModifierUtil.isCtrl(event))
        {
            return; // not for us: the wheel scrolls the diagram
        }
        int scroll = event.getUnitsToScroll();
        if (scroll < 0) this.editorPart.changeZoom(1);
        if (scroll > 0) this.editorPart.changeZoom(-1);
    }
}
```

Then register it in `Workspace.getEditorPart()` with `behaviorManager.addBehavior(new ZoomByWheelBehavior(this.getEditorPart()))`, choosing its place in the list. If it modifies the graph, send the matching `before…`/`after…` notifications through the manager, and undo/redo and auto-save will work with no extra code.

Three rules keep the display right and fast (see the next section for the reasons):

1. **A change of the graph is followed by a repaint.** The `after…` notifications already repaint (`SwingRepaintingBehavior`), and so does any press, drag, release or click on the diagram. A behavior that changes the graph in any other way (a timer, a key, a menu action) must call `editorPart.getSwingComponent().repaint()`.
2. **`onMouseMoved` never changes the graph.** It may only look for what lies under the mouse and paint a feedback (halo, cursor, handle).
3. **A behavior that changes the graph while the mouse is dragged says what it changes**, at each step, with `editorPart.startMoving(nodes, edges)` (only the first call of a gesture counts). It passes empty lists when it only paints over the graph (a lasso, a link being drawn). Without this call everything still works, only more slowly.


How the diagram is drawn
========================

Diagrams are drawn with Java2D, and in the browser this Java code runs through CheerpJ, much slower than on the desktop. The framework therefore avoids computing or drawing twice what didn't change. Plugins get all of this for free, provided they declare one setting (see [Plugin architecture](#plugin-architecture)).

**One geometry per state of the graph.** Laying out an edge (its route, where it meets its nodes, its texts) depends on the other edges of its nodes and, for routed diagrams, on every node. `GeometryCache` (package `product.diagram.abstracts`) remembers these results during a *pass* over the graph: drawing it, measuring it, or looking for the element under the mouse. The editor keeps the same cache from one pass to the next as long as the graph hasn't changed: it counts every `repaint()` asked outside a mouse move, and every press, drag, release and click, as a possible change. So all the behaviors that look under the mouse share one computation, and a mouse move costs almost nothing. Code that changes the graph *during* a pass (the sequence diagram spaces its activation bars just before drawing them) must call `GeometryCache.suspend()` / `resume()` around the change, then `forget()`.

**One image of the diagram.** The editor keeps an image of the visible part of the diagram (background, grid and graph), drawn again only when the graph, the view, the zoom or the theme change. A halo following the mouse or a selection handle is painted over a copy of this image. Elements out of the visible part are not drawn at all.

**Drawing in layers during a gesture.** While an element is dragged, resized, or a segment or a text of a link is moved, the behavior tells the editor what it changes (`IEditorPart.startMoving`). The graph then tells which elements may change as a consequence (`IGraph.getChangingElements`, computed by `AbstractGraph` from the plugin's setting). The editor draws all the others once in an image, then, at each step of the gesture, copies it and draws only the changing elements. During the gesture these elements are drawn above the others; everything is drawn normally again on release.

| Setting (`AbstractGraph.LayeredDrawing`) | What may change while an element moves | Diagrams |
|---|---|---|
| `NONE` (default) | anything: everything is redrawn at each step | sequence (moving a bar moves the others and lengthens every lifeline) |
| `NEIGHBORS` | the element, its parent and children, its links, and the other links of the nodes it is linked to (their ends are spread along the same sides) | class, object, use case, state |
| `ALL_EDGES` | the element and **all** the links, routed according to every node; plus the nodes that place themselves after their neighbors (`isPlacedByNeighbors`) | activity (synchronization bars), BPMN |

**Tests.** `RenderingOptimizationTest` draws the samples and large generated diagrams with and without these optimizations (`GeometryCache.setEnabled(false)`) and compares every pixel, the bounds and the element found under a grid of points. `LayeredDrawingTest` moves every node, segment and text of several diagrams and checks that the elements drawn once keep exactly the same pixels: it fails if a diagram's setting is too optimistic. `DragLayerTest` plays real drags, lassos and hovers in a workspace and compares each step with a full paint.


Plugin architecture
===================

Each diagram type is a Maven module that depends only on the framework:

```
violet-framework                    core: model, workspace, files, plugin loader
violetplugin-classdiagram           ┐
violetplugin-sequencediagram        │
violetplugin-activitydiagram        │  one plugin per diagram type
violetplugin-statediagram           │
violetplugin-objectdiagram          │
violetplugin-usecasediagram         │
violetplugin-bpmndiagram            ┘
violetproduct-swing                 the application: menus, main window, browser bridge, fat JAR
violetproduct-exe / -deb            installers (Windows / Debian)
```

A plugin contains:

1. **An `IDiagramPlugin` implementation** giving the diagram name, its menu category, its file extension and its graph class. For instance `ClassDiagramPlugin` returns `ClassDiagramGraph.class` and the `.class.violet.html` extension.
2. **A graph class** (`ClassDiagramGraph extends AbstractGraph`) that declares the **node and edge prototypes** shown in the side bar, and the **connection rules** (which UML relationship may go from which element to which element: a class implements an interface, but never inherits from it).
3. **Node and edge classes** (`ClassNode`, `InterfaceNode`, `InheritanceEdge`…) that draw themselves, plus their `.properties` files for tooltips and labels.
4. **A service declaration**: the file `src/main/resources/META-INF/services/com.horstmann.violet.framework.plugin.IDiagramPlugin` lists the plugin class.
5. **Its drawing setting**: the graph overrides `getLayeredDrawing()` to tell what may change while an element is dragged (see [How the diagram is drawn](#how-the-diagram-is-drawn)). Choose `NONE` (the default) when moving an element may move, resize or reshape elements not linked to it; `NEIGHBORS` when elements stay where the user puts them and a link only depends on its two nodes; `ALL_EDGES` when links are routed according to all the nodes, with `isPlacedByNeighbors(node)` returning true for the nodes that place or size themselves after the nodes they are linked to. **When in doubt, keep `NONE`**: it is always right, only slower on large diagrams. Then add the diagram to `LayeredDrawingTest.diagrams()`, which checks the choice.

At startup, `PluginLoader` discovers every plugin on the classpath with the standard Java `ServiceLoader` and registers it in the `PluginRegistry`. The "File > New" menu, the file filters and the side bar are then built from the registered plugins. Adding a new diagram type therefore needs **no change in the framework**: create a module modeled on an existing plugin, and add it to the root `pom.xml` and to the dependencies of `violetproduct-swing`. When packaging the fat JAR, the Maven Shade plugin merges the service files of all plugins (`AppendingTransformer`), so that they all load.


Example: a mind map plugin
--------------------------

Here is a complete, minimal plugin that adds a seventh kind of diagram, a mind map, made of rounded **topics** linked by **branches**. It is four short classes and one text file.

![The mind map plugin running in Violet](docs/images/plugin-example-mindmap.png)

**1. The module.** Create a `violetplugin-mindmap` directory next to the other plugins, with this `pom.xml`:

```xml
<project>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>violetplugin-mindmap</artifactId>
    <version>${applicationVersion}</version>
    <parent>
        <groupId>com.horstmann.violet</groupId>
        <artifactId>parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <dependencies>
        <dependency>
            <groupId>com.horstmann.violet</groupId>
            <artifactId>violet-framework</artifactId>
            <version>${applicationVersion}</version>
        </dependency>
    </dependencies>
</project>
```

**2. The plugin** (`src/main/java/com/example/violet/mindmap/MindMapPlugin.java`) tells Violet the name of the diagram, its menu category, its file extension and its graph class:

```java
public class MindMapPlugin implements IDiagramPlugin
{
    public String getDescription()       { return "Mind map diagram"; }
    public String getProvider()          { return "Me"; }
    public String getVersion()           { return "1.0.0"; }
    public String getName()              { return "7.Mind map"; }        // "7." = position in File > New
    public String getCategory()          { return "3.Other diagrams"; }
    public String getFileExtension()     { return ".mindmap.violet.html"; }
    public String getFileExtensionName() { return "Mind maps (.mindmap.violet.html)"; }
    public Class<? extends IGraph> getGraphClass() { return MindMapGraph.class; }
}
```

**3. The graph** declares what the side bar offers (the node and edge prototypes) and which links are allowed between which nodes:

```java
public class MindMapGraph extends AbstractGraph
{
    private static final List<INode> NODE_PROTOTYPES = new ArrayList<INode>();
    private static final List<IEdge> EDGE_PROTOTYPES = new ArrayList<IEdge>();

    /** A branch goes from a topic to a topic */
    private static final List<ConnectionRule> CONNECTION_RULES = List.of(
            new ConnectionRule(TopicNode.class, TopicNode.class, BranchEdge.class));

    static
    {
        TopicNode topic = new TopicNode();
        topic.setToolTip("Topic");
        NODE_PROTOTYPES.add(topic);
        NODE_PROTOTYPES.add(new NoteNode());                    // common elements are welcome too
        BranchEdge branch = new BranchEdge();
        branch.setToolTip("Branch");
        EDGE_PROTOTYPES.add(branch);
        EDGE_PROTOTYPES.addAll(GenericEdge.createToolPrototypes());
    }

    public List<INode> getNodePrototypes() { return NODE_PROTOTYPES; }
    public List<IEdge> getEdgePrototypes() { return EDGE_PROTOTYPES; }
    @Override
    protected List<ConnectionRule> getConnectionRules() { return CONNECTION_RULES; }

    /** Topics stay where the user puts them and a branch only depends on its two topics: fast drags */
    @Override
    protected LayeredDrawing getLayeredDrawing() { return LayeredDrawing.NEIGHBORS; }
}
```

**4. The node** computes its bounds and draws itself. Its `name` field has a getter and a setter: that is all it takes for it to be saved in the files, edited in the properties dialog, and replaced by typing on the selected topic.

```java
/** A rounded box holding a text. Every field with a getter and a setter is saved and shown in the properties dialog. */
public class TopicNode extends RectangularNode
{
    private MultiLineString name = new MultiLineString();

    @Override
    public Rectangle2D getBounds()
    {
        Rectangle2D text = name.getBounds();
        return new Rectangle2D.Double(getLocation().getX(), getLocation().getY(),
                Math.max(text.getWidth() + 20, 100), Math.max(text.getHeight() + 10, 40));
    }

    @Override
    public Shape getShape()
    {
        Rectangle2D b = getBounds();
        return new RoundRectangle2D.Double(b.getX(), b.getY(), b.getWidth(), b.getHeight(), b.getHeight(), b.getHeight());
    }

    @Override
    public void draw(Graphics2D g2)
    {
        super.draw(g2);                                 // selection, shadow...
        Color oldColor = g2.getColor();
        g2.setColor(getBackgroundColor());
        g2.fill(getShape());
        g2.setColor(getBorderColor());
        g2.draw(getShape());
        g2.setColor(getTextColor());
        name.draw(g2, getBounds());
        g2.setColor(oldColor);
    }

    public MultiLineString getName()                { return name; }
    public void setName(MultiLineString newValue)   { name = newValue; }

    @Override
    public TopicNode clone()                        // the side bar clones the prototype for each new topic
    {
        TopicNode cloned = (TopicNode) super.clone();
        cloned.name = name.clone();
        return cloned;
    }
}
```

**5. The edge.** `SegmentedLineEdge` already knows how to draw a line with labels, line styles, arrow heads and bends, so a branch only changes a default:

```java
/** SegmentedLineEdge already draws the line, its labels, styles and arrow heads: only the default changes */
public class BranchEdge extends SegmentedLineEdge
{
    public BranchEdge()
    {
        setBentStyle(BentStyle.STRAIGHT);
    }
}
```

**6. Register it.** Create `src/main/resources/META-INF/services/com.horstmann.violet.framework.plugin.IDiagramPlugin` containing the single line:

```
com.example.violet.mindmap.MindMapPlugin
```

Then add `<module>violetplugin-mindmap</module>` to the root `pom.xml` and the module as a dependency of `violetproduct-swing/pom.xml` (and to the folders of its `cheerpj-test` profile to try it in the browser). Add a small mind map to `LayeredDrawingTest.diagrams()` in `violetproduct-swing/src/test` to check the drawing setting. Run `mvn clean package`: "Mind map" appears in File > New, with its own section in the side bar, and saves to `.mindmap.violet.html` files, undo/redo, copy/paste, colors and export included.

> Real plugins keep their texts (names, tooltips) in `.properties` files in English, French and German rather than in the code: see `ClassDiagramPlugin` and `ClassDiagramGraph` for the complete pattern.


How to compile Violet's source code
===================================

Prerequisites:

+ **Java Development Kit 17** or greater (the bytecode targets Java 17; CI builds with JDK 21)
+ **Maven 3** or greater

From the root directory:

```bash
git clone https://github.com/violetumleditor/product.git
cd product
mvn clean package
```

This compiles every module, runs the tests and packages everything. Each module puts its result in its own `target` directory, for example:

+ `violetproduct-swing/target/violetumleditor-<version>.jar`: the application (a single runnable JAR with all plugins)
+ `violetproduct-exe/target/violetumleditor-<version>.exe`: the Windows executable
+ `violetproduct-deb/target/*.deb`: the Debian package

To build and run only the application:

```bash
mvn clean package -pl violetproduct-swing -am
java -jar violetproduct-swing/target/violetumleditor-<version>.jar
```

Useful launch options: `-english` (force English), `-theme=light|dark`, `-file=<path>` (open a diagram), `-reset` (clear the preferences), `-help`.

Test the browser version (CheerpJ) without packaging
----------------------------------------------------

The online version runs the very same Swing application in the browser with CheerpJ (see `violetproduct-swing/src/main/resources/web/index.html`). While developing, you can run your local code the same way, from your file system, with no web server:

1. From the root directory, run `mvn compile -Pcheerpj-test`. Besides compiling, this profile generates `violetproduct-swing/target/cheerpj-test/bundle.js`, which contains the compiled classes of every module and all the third-party jars.
2. Open `violetproduct-swing/src/main/resources/web/test.html` in your browser (double-click it, or use a `file://` URL). An internet connection is required to download the CheerpJ runtime.
3. After each change in the Java code, run step 1 again, then reload the page.

Notes:
+ Always run the command from the root directory. From `violetproduct-swing` alone, Maven would use previously installed framework and plugin jars instead of your current code.
+ Compiling from your IDE is not enough: the bundle is only refreshed by the Maven command.
+ `test.html` is a copy of `index.html` that only differs by the way it starts the application. When you change the JavaScript bridge (native functions) in `index.html`, report the change in `test.html`.

How files are handled in the browser version
--------------------------------------------

In the browser, diagrams are kept in the browser's own storage (CheerpJ's `/files` directory, stored in IndexedDB). They survive a page reload, but they are tied to this browser and this site.

+ New diagrams are auto-saved 2 seconds after the last change, then show up in File > Recent files.
+ File > Open lists the diagrams kept in the browser, most recent first; the trash icon (or the Delete key) removes one.
+ File > Import from > This device > Diagram file loads a diagram from your computer; File > Export to > This device > Diagram file downloads the current one. The diagrams kept in the browser storage hold no pictures (Violet alone reads them back); the downloaded file is a complete viewer, pictures included.
+ File > Export to > This device > Archive file downloads all the diagrams kept in the browser as one `.zip` (a backup, or to move them to another browser); File > Import from > This device > Archive file puts them back, asking for each diagram of the same name whether to replace it or skip it.


How to contribute
=================

**Please do not open pull requests: they will not be merged.** Violet is maintained by a single developer who integrates every change by hand.

Contributions are very welcome as **[issues](https://github.com/violetumleditor/product/issues)**:

+ **Bug report**: what you did, what you expected, what happened, your version of Violet, and whether you use the desktop or the online version (with your browser). Attach the `.violet.html` file when a diagram is involved, it is the quickest way to reproduce the problem.
+ **Feature request**: describe your need and, if possible, the use case behind it. A mockup or a screenshot helps a lot.

Every issue is read, and the good ideas end up in the next version.


Kind regards,
Alex

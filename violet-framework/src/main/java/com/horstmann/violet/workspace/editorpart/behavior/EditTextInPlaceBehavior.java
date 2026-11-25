package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Color;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentAdapter;
import java.awt.Container;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.undo.UndoManager;

import com.horstmann.violet.framework.propertyeditor.customeditor.DiagramPlainTextEditorKit;
import com.horstmann.violet.framework.propertyeditor.customeditor.DiagramTextPane;
import com.horstmann.violet.framework.propertyeditor.customeditor.MultiLineStringEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.ZoomedTextBox;
import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.sidebar.colortools.ColorChoice;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * A double click on a text of the diagram edits it in place: a text box opens on it, holding the current text, with a caret that
 * can be moved anywhere. On an edge, its start, middle or end text (a plain String) is typed in a plain text pane. On a node, the
 * text under the mouse (the name of a class, its attributes or its methods, the text of a note...) is typed in the rich text pane
 * of the properties dialog, its toolbar floating above the node, so its formatting is kept.
 * <p>
 * The box is transparent and shows the text where and as the diagram draws it, at any zoom (ZoomedTextBox); the diagram doesn't
 * draw this text meanwhile. It is placed again when the text, so the element, changes, and when the diagram is zoomed.
 * <p>
 * The diagram shows the text as it is typed. Enter starts a new line; a click elsewhere on the diagram, a tool change or Ctrl+Enter
 * validates, as one undoable edit; Escape restores the former text. Ctrl+Z / Ctrl+Y undo and redo the typing in the box, not the
 * diagram. A double click elsewhere (no text under the mouse) still opens the properties dialog (EditSelectedBehavior, which comes
 * after this behavior: the double click is consumed when a text is edited).
 */
public class EditTextInPlaceBehavior extends AbstractEditorPartBehavior
{

    public EditTextInPlaceBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        this.editorPart = editorPart;
        this.graphToolsBar = graphToolsBar;
    }

    @Override
    public void onMouseClicked(MouseEvent event)
    {
        if (event.getClickCount() != 2 || event.getButton() != MouseEvent.BUTTON1 || !isSelectionTool())
        {
            return;
        }
        Point2D point = this.editorPart.toGraphPoint(event.getPoint());
        if (startEditing(point))
        {
            event.consume();
        }
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        commit();
    }

    @Override
    public void onToolSelected(GraphTool selectedTool)
    {
        commit();
    }

    @Override
    public void beforeRemovingSelectedElements()
    {
        commit();
    }

    /**
     * Opens the text box on the text at the given point, if there is one
     *
     * @param point on the graph (unzoomed)
     * @return true if a text is being edited
     */
    public boolean startEditing(Point2D point)
    {
        commit();
        if (startEditingEdgeText(point))
        {
            return true;
        }
        return startEditingNodeText(point);
    }

    public boolean isEditing()
    {
        return this.session != null;
    }

    /**
     * Keeps the typed text, as one undoable property edit, and closes the box
     */
    public void commit()
    {
        Session closed = close();
        if (closed == null || !closed.isModified)
        {
            return;
        }
        Object newValue = readProperty(closed);
        PropertyChangeEvent event = new PropertyChangeEvent(closed.element, closed.property.getName(), closed.originalValue, newValue);
        IEditorPartBehaviorManager behaviorManager = this.editorPart.getBehaviorManager();
        // Undo/redo, auto-save and repainting listen to these
        if (closed.element instanceof INode)
        {
            INode node = (INode) closed.element;
            behaviorManager.fireBeforeEditingNode(node);
            behaviorManager.fireWhileEditingNode(node, event);
            behaviorManager.fireAfterEditingNode(node);
        }
        if (closed.element instanceof IEdge)
        {
            IEdge edge = (IEdge) closed.element;
            behaviorManager.fireBeforeEditingEdge(edge);
            behaviorManager.fireWhileEditingEdge(edge, event);
            behaviorManager.fireAfterEditingEdge(edge);
        }
        refreshDiagram();
    }

    /**
     * Gives the element its former text back and closes the box
     */
    public void cancel()
    {
        Session closed = close();
        if (closed == null)
        {
            return;
        }
        writeProperty(closed, closed.originalValue);
        refreshDiagram();
    }

    // ------------------------------------------------------------------
    // Edges
    // ------------------------------------------------------------------

    /**
     * The texts of the edges are drawn beside their lines, over the nodes: they are looked for first
     */
    private boolean startEditingEdgeText(Point2D point)
    {
        for (IEdge edge : getGraph().getAllEdges())
        {
            if (edge instanceof SegmentedLineEdge)
            {
                SegmentedLineEdge segmentedEdge = (SegmentedLineEdge) edge;
                if (startEditingEdgeText(edge, "startLabel", segmentedEdge.getStartLabelBounds(), point)
                        || startEditingEdgeText(edge, "middleLabel", segmentedEdge.getMiddleLabelBounds(), point)
                        || startEditingEdgeText(edge, "endLabel", segmentedEdge.getEndLabelBounds(), point))
                {
                    return true;
                }
                continue;
            }
            // Another kind of edge (a state transition): its only text, at its middle
            for (String name : OTHER_EDGE_TEXT_PROPERTIES)
            {
                if (startEditingEdgeText(edge, name, edge.getMiddleLabelBounds(), point))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean startEditingEdgeText(IEdge edge, String propertyName, Rectangle2D textBounds, Point2D point)
    {
        if (textBounds == null || !textBounds.contains(point))
        {
            return false;
        }
        PropertyDescriptor property = getProperty(edge, propertyName, String.class);
        if (property == null)
        {
            return false;
        }
        Session newSession = new Session(edge, property, readProperty(edge, property));
        final DiagramTextPane pane = createEdgeTextPane(newSession, textBounds);
        newSession.box = new ZoomedTextBox(pane);
        newSession.textComponent = pane;
        // Only the box shows the text while it is edited
        edge.setHiddenText(propertyName);
        open(newSession);
        pane.getDocument().addDocumentListener(new DocumentListener()
        {
            public void insertUpdate(DocumentEvent e)
            {
                update();
            }

            public void removeUpdate(DocumentEvent e)
            {
                update();
            }

            public void changedUpdate(DocumentEvent e)
            {
            }

            private void update()
            {
                newSession.isModified = true;
                writeProperty(newSession, getPlainText(pane));
                onTyped(newSession);
            }
        });
        return true;
    }

    /**
     * A plain text pane whose lines are those of the label, aligned like them
     */
    private DiagramTextPane createEdgeTextPane(Session newSession, Rectangle2D textBounds)
    {
        newSession.textArea = textBounds;
        newSession.justification = MultiLineString.Justification.LEFT;
        if (newSession.element instanceof SegmentedLineEdge)
        {
            newSession.justification = ((SegmentedLineEdge) newSession.element).getLabelJustification(newSession.property.getName());
        }
        DiagramTextPane pane = new DiagramTextPane();
        pane.setEditorKit(new DiagramPlainTextEditorKit(getTextColor(newSession.element)));
        pane.setCaretColor(getTextColor(newSession.element));
        String text = (String) newSession.originalValue;
        pane.setText(text == null ? "" : text);
        SimpleAttributeSet alignment = new SimpleAttributeSet();
        int[] alignments = { StyleConstants.ALIGN_LEFT, StyleConstants.ALIGN_CENTER, StyleConstants.ALIGN_RIGHT };
        StyleConstants.setAlignment(alignment, alignments[newSession.justification]);
        pane.getStyledDocument().setParagraphAttributes(0, pane.getDocument().getLength() + 1, alignment, false);
        return pane;
    }

    /**
     * @return the text of the pane, its lines separated by '\n' on every system
     */
    private String getPlainText(JTextComponent pane)
    {
        try
        {
            return pane.getDocument().getText(0, pane.getDocument().getLength());
        }
        catch (BadLocationException e)
        {
            return pane.getText();
        }
    }

    /**
     * @return where the label being edited is now, or null if it is empty
     */
    private Rectangle2D findEdgeTextArea(Session aSession)
    {
        if (aSession.element instanceof SegmentedLineEdge)
        {
            return ((SegmentedLineEdge) aSession.element).getLabelBounds(aSession.property.getName());
        }
        return ((IEdge) aSession.element).getMiddleLabelBounds();
    }

    // ------------------------------------------------------------------
    // Nodes
    // ------------------------------------------------------------------

    /**
     * The text of the node under the point is found by drawing the graph again on an image: each MultiLineString remembers where
     * it was drawn, on the graph since the image has no transform
     */
    private boolean startEditingNodeText(Point2D point)
    {
        INode node = getGraph().findNodeUnderMarkers(point);
        if (node == null)
        {
            return false;
        }
        PropertyDescriptor[] properties = getMultiLineStringProperties(node);
        for (PropertyDescriptor property : properties)
        {
            Object text = readProperty(node, property);
            if (text instanceof MultiLineString)
            {
                ((MultiLineString) text).forgetDrawnArea();
            }
        }
        drawOffScreen();
        // The smallest area holding the point: a text drawn inside the area of another one is the one aimed at
        PropertyDescriptor found = null;
        Rectangle2D foundArea = null;
        for (PropertyDescriptor property : properties)
        {
            Object text = readProperty(node, property);
            Rectangle2D area = text instanceof MultiLineString ? ((MultiLineString) text).getDrawnArea() : null;
            if (area == null || !area.contains(point))
            {
                continue;
            }
            if (foundArea == null || area.getWidth() * area.getHeight() < foundArea.getWidth() * foundArea.getHeight())
            {
                found = property;
                foundArea = area;
            }
        }
        if (found == null)
        {
            return false;
        }
        startEditingNodeText(node, found, (MultiLineString) readProperty(node, found));
        return true;
    }

    /**
     * The node keeps its MultiLineString, changed in place by the rich text box as typed; a copy of the former one is kept for
     * Escape and undo
     */
    private void startEditingNodeText(INode node, PropertyDescriptor property, MultiLineString text)
    {
        final Session newSession = new Session(node, property, text.clone());
        newSession.textArea = text.getDrawnTextArea();
        newSession.textToGraph = text.getDrawnTransform();
        MultiLineStringEditor textEditor = new MultiLineStringEditor();
        textEditor.setDefaultTextColor(getTextColor(node));
        textEditor.setValue(text);
        textEditor.getCustomEditor();
        newSession.textComponent = textEditor.getTextComponent();
        newSession.textComponent.setCaretColor(getTextColor(node));
        newSession.box = new ZoomedTextBox(textEditor.getTextComponent());
        newSession.textEditor = textEditor;
        newSession.toolbar = createFloatingToolbar(textEditor);
        // Changed by the user only from now on (loading the text into the box may rewrite its HTML)
        newSession.textComponent.getDocument().addDocumentListener(new DocumentListener()
        {
            public void insertUpdate(DocumentEvent e)
            {
                newSession.isModified = true;
            }

            public void removeUpdate(DocumentEvent e)
            {
                newSession.isModified = true;
            }

            public void changedUpdate(DocumentEvent e)
            {
                newSession.isModified = true;
            }
        });
        // Only the box shows the text while it is edited (the node still takes its size)
        text.setHidden(true);
        newSession.nodeText = text;
        open(newSession);
        textEditor.addPropertyChangeListener(e -> onTyped(newSession));
    }

    /**
     * The formatting toolbar of the editor, as a card lying on the diagram
     */
    private JComponent createFloatingToolbar(MultiLineStringEditor textEditor)
    {
        JComponent toolbar = textEditor.getToolbar();
        toolbar.setOpaque(true);
        Color borderColor = UIManager.getColor("Component.borderColor");
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor == null ? Color.GRAY : borderColor), toolbar.getBorder()));
        toolbar.addMouseListener(new MouseAdapter()
        {
            // Else a press between its buttons would go to the diagram, so close the box
        });
        toolbar.setCursor(Cursor.getDefaultCursor());
        return toolbar;
    }

    /**
     * Draws the graph on an image, so that each MultiLineString remembers where it is on the graph
     */
    private void drawOffScreen()
    {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try
        {
            getGraph().draw(g2);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * @return where the text of the node being edited is now, or null if its node doesn't draw it
     */
    private Rectangle2D findNodeTextArea(Session aSession)
    {
        aSession.nodeText.forgetDrawnArea();
        drawOffScreen();
        return aSession.nodeText.getDrawnTextArea();
    }

    /**
     * @return the MultiLineString properties of the node (with a getter and a setter)
     */
    private PropertyDescriptor[] getMultiLineStringProperties(INode node)
    {
        try
        {
            return java.util.Arrays.stream(Introspector.getBeanInfo(node.getClass()).getPropertyDescriptors())
                    .filter(p -> MultiLineString.class.equals(p.getPropertyType()) && p.getReadMethod() != null
                            && p.getWriteMethod() != null)
                    .toArray(PropertyDescriptor[]::new);
        }
        catch (IntrospectionException | RuntimeException e)
        {
            e.printStackTrace();
            return new PropertyDescriptor[0];
        }
    }

    // ------------------------------------------------------------------
    // The box
    // ------------------------------------------------------------------

    private void open(final Session newSession)
    {
        this.session = newSession;
        final JTextComponent textComponent = newSession.textComponent;
        bindKeys(newSession);
        textComponent.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                // A popup of the toolbar (text color) or another window takes it for a while; a component of the window keeps it
                Component opposite = e.getOppositeComponent();
                if (e.isTemporary() || opposite == null || SwingUtilities.isDescendingFrom(opposite, newSession.box)
                        || (newSession.toolbar != null && SwingUtilities.isDescendingFrom(opposite, newSession.toolbar)))
                {
                    return;
                }
                if (session == newSession)
                {
                    commit();
                }
            }
        });
        JComponent diagram = this.editorPart.getSwingComponent();
        diagram.add(newSession.box);
        if (newSession.toolbar != null)
        {
            diagram.add(newSession.toolbar, 0);
        }
        newSession.zoomListener = e -> placeBox(newSession);
        diagram.addPropertyChangeListener(IEditorPart.ZOOM_PROPERTY, newSession.zoomListener);
        diagram.addPropertyChangeListener(IEditorPart.DRAWING_AREA_PROPERTY, newSession.zoomListener);
        newSession.resizeListener = new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                // Rotation of the screen, on-screen keyboard
                placeBox(newSession);
                refreshDiagram();
            }
        };
        newSession.viewport = diagram.getParent();
        if (newSession.viewport != null)
        {
            newSession.viewport.addComponentListener(newSession.resizeListener);
        }
        textComponent.setCaretPosition(textComponent.getDocument().getLength());
        placeBox(newSession);
        refreshDiagram();
        SwingUtilities.invokeLater(() -> textComponent.requestFocusInWindow());
    }

    /**
     * Escape cancels, Ctrl+Enter validates, Ctrl+Z / Ctrl+Y undo and redo the typing (the menu would undo the diagram)
     */
    private void bindKeys(final Session newSession)
    {
        JTextComponent textComponent = newSession.textComponent;
        final UndoManager undoManager = new UndoManager();
        textComponent.getDocument().addUndoableEditListener(undoManager);
        int menuModifier = getMenuShortcutModifier();
        bind(textComponent, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "violet-inplace-cancel", () -> cancel());
        bind(textComponent, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, menuModifier), "violet-inplace-commit", () -> commit());
        bind(textComponent, KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuModifier), "violet-inplace-undo", () -> {
            if (undoManager.canUndo()) undoManager.undo();
        });
        Runnable redo = () -> {
            if (undoManager.canRedo()) undoManager.redo();
        };
        bind(textComponent, KeyStroke.getKeyStroke(KeyEvent.VK_Y, menuModifier), "violet-inplace-redo", redo);
        bind(textComponent, KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuModifier | InputEvent.SHIFT_DOWN_MASK), "violet-inplace-redo",
                redo);
    }

    private void bind(JComponent component, KeyStroke keyStroke, String actionKey, final Runnable action)
    {
        component.getInputMap(JComponent.WHEN_FOCUSED).put(keyStroke, actionKey);
        component.getActionMap().put(actionKey, new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                action.run();
            }
        });
    }

    private int getMenuShortcutModifier()
    {
        try
        {
            return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        }
        catch (RuntimeException e)
        {
            return InputEvent.CTRL_DOWN_MASK; // no screen
        }
    }

    /**
     * The element may have moved its text, and the box doesn't have its new size yet: this is called while its document tells its
     * change
     */
    private void onTyped(final Session aSession)
    {
        SwingUtilities.invokeLater(() -> {
            if (this.session == aSession)
            {
                placeBox(aSession);
                refreshDiagram();
            }
        });
        refreshDiagram();
    }

    /**
     * Puts the text of the box exactly where the diagram would draw it, at the current zoom
     */
    private void placeBox(Session aSession)
    {
        Rectangle2D area = aSession.nodeText != null ? findNodeTextArea(aSession) : findEdgeTextArea(aSession);
        if (area == null)
        {
            area = aSession.textArea; // emptied: where it was
        }
        else if (aSession.nodeText != null)
        {
            aSession.textToGraph = aSession.nodeText.getDrawnTransform();
        }
        aSession.textArea = area;
        int justification = aSession.nodeText != null ? aSession.nodeText.getJustification() : aSession.justification;
        Dimension textSize = aSession.box.getTextSize();
        int width = (int) Math.ceil(Math.max(textSize.width, area.getWidth()));
        double x = area.getX();
        if (justification == MultiLineString.Justification.CENTER)
        {
            x = area.getCenterX() - width / 2.0;
        }
        else if (justification == MultiLineString.Justification.RIGHT)
        {
            x = area.getMaxX() - width;
        }
        aSession.box.place(x, area.getY(), width, textSize.height, aSession.textToGraph, this.editorPart.getZoomFactor(),
                this.editorPart.toComponentPoint(new Point2D.Double()));
        if (aSession.toolbar != null)
        {
            placeToolbar(aSession);
        }
    }

    /**
     * The toolbar lies above the node, or below it when there is no room, inside the visible part of the diagram
     */
    private void placeToolbar(Session aSession)
    {
        INode node = (INode) aSession.element;
        double zoom = this.editorPart.getZoomFactor();
        Rectangle2D bounds = node.getBounds();
        Point2D onDiagram = this.editorPart.toComponentPoint(node.getLocationOnGraph());
        Rectangle box = aSession.box.getBounds();
        Rectangle onScreen = new Rectangle2D.Double(onDiagram.getX(), onDiagram.getY(), bounds.getWidth() * zoom,
                bounds.getHeight() * zoom).getBounds().union(box);
        JComponent diagram = this.editorPart.getSwingComponent();
        Rectangle visible = diagram.getVisibleRect();
        if (visible.isEmpty())
        {
            visible = new Rectangle(diagram.getSize());
        }
        if (visible.width > 0)
        {
            // On several lines when the diagram is too narrow (phone)
            aSession.textEditor.setMaxWidth(visible.width);
        }
        Dimension size = aSession.toolbar.getPreferredSize();
        int x = (int) box.getCenterX() - size.width / 2;
        int y = onScreen.y - size.height - TOOLBAR_GAP;
        if (y < visible.y)
        {
            y = onScreen.y + onScreen.height + TOOLBAR_GAP;
        }
        y = Math.max(visible.y, Math.min(y, visible.y + visible.height - size.height));
        x = Math.max(visible.x, Math.min(x, visible.x + visible.width - size.width));
        aSession.toolbar.setBounds(x, y, size.width, size.height);
        aSession.toolbar.doLayout();
    }

    private Color getTextColor(ISelectable element)
    {
        return element instanceof IColorable ? ((IColorable) element).getTextColor() : ColorChoice.DEFAULT_TEXT_COLOR;
    }

    /**
     * @return the session that was open, or null
     */
    private Session close()
    {
        Session closed = this.session;
        if (closed == null)
        {
            return null;
        }
        this.session = null;
        if (closed.element instanceof IEdge)
        {
            ((IEdge) closed.element).setHiddenText(null);
        }
        if (closed.nodeText != null)
        {
            closed.nodeText.setHidden(false);
        }
        JComponent diagram = this.editorPart.getSwingComponent();
        diagram.removePropertyChangeListener(IEditorPart.ZOOM_PROPERTY, closed.zoomListener);
        diagram.removePropertyChangeListener(IEditorPart.DRAWING_AREA_PROPERTY, closed.zoomListener);
        if (closed.viewport != null)
        {
            closed.viewport.removeComponentListener(closed.resizeListener);
        }
        diagram.remove(closed.box);
        if (closed.toolbar != null)
        {
            diagram.remove(closed.toolbar);
        }
        // The diagram paints only when invalid: else the box would stay on screen
        refreshDiagram();
        return closed;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private PropertyDescriptor getProperty(Object element, String name, Class<?> type)
    {
        try
        {
            for (PropertyDescriptor property : Introspector.getBeanInfo(element.getClass()).getPropertyDescriptors())
            {
                if (property.getName().equals(name) && type.equals(property.getPropertyType()) && property.getReadMethod() != null
                        && property.getWriteMethod() != null)
                {
                    return property;
                }
            }
        }
        catch (IntrospectionException | RuntimeException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private Object readProperty(Session aSession)
    {
        return readProperty(aSession.element, aSession.property);
    }

    private Object readProperty(Object element, PropertyDescriptor property)
    {
        try
        {
            return property.getReadMethod().invoke(element);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    private void writeProperty(Session aSession, Object value)
    {
        if (Objects.equals(readProperty(aSession), value) && !(value instanceof MultiLineString))
        {
            return;
        }
        try
        {
            aSession.property.getWriteMethod().invoke(aSession.element, value);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            e.printStackTrace();
        }
    }

    private void refreshDiagram()
    {
        JComponent diagram = this.editorPart.getSwingComponent();
        diagram.invalidate();
        diagram.repaint();
    }

    private boolean isSelectionTool()
    {
        return this.graphToolsBar == null || GraphTool.SELECTION_TOOL.equals(this.graphToolsBar.getSelectedTool());
    }

    private IGraph getGraph()
    {
        return this.editorPart.getGraph();
    }

    /**
     * The text being edited: in which element, which property, its former value, the box shown
     */
    private static class Session
    {
        Session(ISelectable element, PropertyDescriptor property, Object originalValue)
        {
            this.element = element;
            this.property = property;
            this.originalValue = originalValue;
        }

        final ISelectable element;

        final PropertyDescriptor property;

        final Object originalValue;

        ZoomedTextBox box;

        /** The formatting toolbar of the text of a node, or null for an edge */
        JComponent toolbar;

        /** The editor the toolbar belongs to, or null for an edge */
        MultiLineStringEditor textEditor;

        PropertyChangeListener zoomListener;

        /** The visible part of the diagram, and what follows its size */
        Container viewport;

        ComponentListener resizeListener;

        /** Where the text was last found, before textToGraph */
        Rectangle2D textArea;

        /** From textArea to the graph: the place of the node, and a rotation for a vertical text */
        AffineTransform textToGraph = new AffineTransform();

        /** How the lines of the text of an edge are aligned */
        int justification;

        JTextComponent textComponent;

        /** The text of the node, not drawn while edited, or null for an edge */
        MultiLineString nodeText;

        boolean isModified;
    }

    private final IEditorPart editorPart;

    private final IGraphToolsBar graphToolsBar;

    /** The text being edited, or null */
    private Session session;

    /** Text properties of the edges which are not segmented (a state transition has a label) */
    private static final String[] OTHER_EDGE_TEXT_PROPERTIES = { "middleLabel", "label" };

    /** Between a node and the toolbar, on screen */
    private static final int TOOLBAR_GAP = 8;
}

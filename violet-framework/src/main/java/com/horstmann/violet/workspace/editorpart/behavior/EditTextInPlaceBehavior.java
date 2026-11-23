package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.util.Objects;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;

import com.horstmann.violet.framework.propertyeditor.customeditor.MultiLineStringEditor;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.edge.SegmentedLineEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.abstracts.property.text.DiagramTextFonts;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * A double click on a text of the diagram edits it in place: a text box opens on it, holding the current text, with a caret that
 * can be moved anywhere. On an edge, its start, middle or end text (a plain String) is typed in a plain text area, at the size of
 * the zoom. On a node, the text under the mouse (the name of a class, its attributes or its methods, the text of a note...) is
 * typed in the rich text box of the properties dialog, toolbar included, so its formatting is kept.
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
        double zoom = this.editorPart.getZoomFactor();
        Point2D point = new Point2D.Double(event.getX() / zoom, event.getY() / zoom);
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
    public void onMouseWheelMoved(MouseWheelEvent event)
    {
        commit(); // the box doesn't follow a zoom
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
        JTextArea area = createEdgeTextArea(newSession);
        newSession.box = area;
        newSession.textComponent = area;
        open(newSession);
        // Only the box shows the text while it is edited: the label drawn on the diagram moves as it grows (a centered one
        // grows on both sides, one above its line grows upwards), the box grows to the right and downwards
        edge.setHiddenText(propertyName);
        refreshDiagram();
        placeEdgeTextArea(area, textBounds);
        return true;
    }

    /**
     * A plain text area with the font of the diagram texts at the size of the zoom, whose lines are those of the label
     */
    private JTextArea createEdgeTextArea(final Session newSession)
    {
        String text = (String) newSession.originalValue;
        final JTextArea area = new JTextArea(text == null ? "" : text);
        double zoom = this.editorPart.getZoomFactor();
        Font font = DiagramTextFonts.getInstance().getFont(false, false, (float) (MultiLineString.FontSize.NORMAL * zoom));
        area.setFont(font);
        area.setBorder(UIManager.getBorder("TextField.border"));
        area.getDocument().addDocumentListener(new DocumentListener()
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
                writeProperty(newSession, area.getText());
                area.setSize(getEdgeTextAreaSize(area, area.getWidth()));
                refreshDiagram();
            }
        });
        return area;
    }

    /**
     * Puts the text of the area where the label is drawn
     */
    private void placeEdgeTextArea(JTextArea area, Rectangle2D textBounds)
    {
        double zoom = this.editorPart.getZoomFactor();
        Insets insets = area.getInsets();
        int minimumWidth = (int) Math.ceil(textBounds.getWidth() * zoom) + insets.left + insets.right;
        Dimension size = getEdgeTextAreaSize(area, minimumWidth);
        int x = (int) Math.round(textBounds.getX() * zoom) - insets.left;
        int y = (int) Math.round(textBounds.getY() * zoom) - insets.top;
        setBoxBounds(area, x, y, size);
    }

    /**
     * @return the size of the area for its lines, a little wider so that the caret has room at the end of the longest one
     */
    private Dimension getEdgeTextAreaSize(JTextArea area, int minimumWidth)
    {
        Dimension preferred = area.getPreferredSize();
        int caretRoom = area.getFontMetrics(area.getFont()).charWidth('m');
        return new Dimension(Math.max(Math.max(minimumWidth, preferred.width + caretRoom), MIN_BOX_WIDTH), preferred.height);
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
        INode node = getGraph().findNode(point);
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
        startEditingNodeText(node, found, (MultiLineString) readProperty(node, found), foundArea);
        return true;
    }

    /**
     * The node keeps its MultiLineString, changed in place by the rich text box as typed; a copy of the former one is kept for
     * Escape and undo
     */
    private void startEditingNodeText(INode node, PropertyDescriptor property, MultiLineString text, Rectangle2D area)
    {
        final Session newSession = new Session(node, property, text.clone());
        MultiLineStringEditor textEditor = new MultiLineStringEditor();
        textEditor.setValue(text);
        JComponent box = (JComponent) textEditor.getCustomEditor();
        textEditor.addPropertyChangeListener(e -> refreshDiagram());
        newSession.box = box;
        newSession.textComponent = (JTextComponent) textEditor.getTextComponent();
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
        open(newSession);
        // Only the box shows the text while it is edited (the node still takes its size)
        text.setHidden(true);
        newSession.hiddenNodeText = text;
        refreshDiagram();
        // The text of the box on the text of the node, centered on it as most texts are, its toolbar above it
        box.doLayout();
        Dimension size = box.getPreferredSize();
        double zoom = this.editorPart.getZoomFactor();
        int width = Math.max(size.width, (int) Math.ceil(area.getWidth() * zoom));
        size = new Dimension(width, size.height);
        int toolbarHeight = textEditor.getToolbar().getPreferredSize().height;
        int x = (int) Math.round(area.getCenterX() * zoom - width / 2.0);
        int y = (int) Math.round(area.getY() * zoom) - toolbarHeight;
        setBoxBounds(box, x, y, size);
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
                if (e.isTemporary() || opposite == null || SwingUtilities.isDescendingFrom(opposite, newSession.box))
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
        textComponent.setCaretPosition(textComponent.getDocument().getLength());
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
     * Places the box, kept inside the visible width and height of the diagram
     */
    private void setBoxBounds(JComponent box, int x, int y, Dimension size)
    {
        JComponent diagram = this.editorPart.getSwingComponent();
        int maxX = Math.max(0, diagram.getWidth() - size.width);
        int boundedX = Math.max(0, Math.min(x, maxX));
        int boundedY = Math.max(0, y);
        box.setBounds(boundedX, boundedY, size.width, size.height);
        box.revalidate();
        box.repaint();
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
        if (closed.hiddenNodeText != null)
        {
            closed.hiddenNodeText.setHidden(false);
        }
        this.editorPart.getSwingComponent().remove(closed.box);
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

        JComponent box;

        JTextComponent textComponent;

        /** The text of the node, not drawn while edited, or null for an edge */
        MultiLineString hiddenNodeText;

        boolean isModified;
    }

    private final IEditorPart editorPart;

    private final IGraphToolsBar graphToolsBar;

    /** The text being edited, or null */
    private Session session;

    /** Text properties of the edges which are not segmented (a state transition has a label) */
    private static final String[] OTHER_EDGE_TEXT_PROPERTIES = { "middleLabel", "label" };

    /** Narrowest box, so that a short text gets room to type */
    private static final int MIN_BOX_WIDTH = 60;
}

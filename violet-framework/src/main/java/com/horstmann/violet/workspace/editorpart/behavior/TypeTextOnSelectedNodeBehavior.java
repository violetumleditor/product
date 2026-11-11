package com.horstmann.violet.workspace.editorpart.behavior;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import com.horstmann.violet.product.diagram.abstracts.ISelectable;
import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.workspace.editorpart.IEditorPart;
import com.horstmann.violet.workspace.editorpart.IEditorPartBehaviorManager;
import com.horstmann.violet.workspace.editorpart.IEditorPartSelectionHandler;
import com.horstmann.violet.workspace.sidebar.graphtools.GraphTool;
import com.horstmann.violet.workspace.sidebar.graphtools.IGraphToolsBar;

/**
 * Typing a character while a single node is selected with the selection tool or a node tool replaces its main text
 * (its first MultiLineString property, for instance the name of a class, not its attributes nor its methods, which stay in the
 * properties dialog) by what is typed, shown live on the diagram. The
 * new text gets the defaults of a new node of this kind (justification, size, underline), whatever the former one had. On a single
 * selected edge, it replaces the text drawn at its middle (its middle label, or the label of a state transition).
 * <p>
 * Backspace erases the last character, Enter starts a new line in a node and validates on an edge (its text is a single line). A
 * click anywhere validates, as one undoable edit (so does any
 * other action: a shortcut, a tool or selection change), and Escape restores the former text. Other keys are swallowed
 * meanwhile, so that Delete doesn't remove the node.
 * <p>
 * The diagram never takes the keyboard focus, so keys are caught by a dispatcher of the window, registered only while this
 * diagram is showing (Swing hides the diagrams of the other tabs). Keys typed in a text field are left alone.
 */
public class TypeTextOnSelectedNodeBehavior extends AbstractEditorPartBehavior implements KeyEventDispatcher
{

    public TypeTextOnSelectedNodeBehavior(IEditorPart editorPart, IGraphToolsBar graphToolsBar)
    {
        this.editorPart = editorPart;
        this.selectionHandler = editorPart.getSelectionHandler();
        this.behaviorManager = editorPart.getBehaviorManager();
        this.graphToolsBar = graphToolsBar;
        JComponent component = editorPart.getSwingComponent();
        component.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0)
            {
                updateKeyListening();
            }
        });
        updateKeyListening();
    }

    /**
     * Starts writing into a node: its main text is replaced by the given one, with the defaults of a new node.
     *
     * @param node the node to write into
     * @param text the first typed characters
     * @return false when this node has no text to write into
     */
    public boolean startTyping(INode node, String text)
    {
        commitTyping();
        PropertyDescriptor property = getTypedTextProperty(node);
        if (property == null)
        {
            return false;
        }
        try
        {
            Object originalText = property.getReadMethod().invoke(node);
            MultiLineString typedText = createDefaultText(node, property, originalText);
            property.getWriteMethod().invoke(node, typedText);
            this.typedText = typedText;
            return startTyping(node, property, originalText, text);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Starts writing into an edge: the text at its middle is replaced by the given one.
     *
     * @param edge the edge to write into
     * @param text the first typed characters
     * @return false when this edge has no text at its middle
     */
    public boolean startTyping(IEdge edge, String text)
    {
        commitTyping();
        PropertyDescriptor property = getTypedTextProperty(edge);
        if (property == null)
        {
            return false;
        }
        try
        {
            Object originalText = property.getReadMethod().invoke(edge);
            this.typedText = null;
            return startTyping(edge, property, originalText, text);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private boolean startTyping(ISelectable element, PropertyDescriptor property, Object originalText, String text)
    {
        this.typedElement = element;
        this.typedProperty = property;
        this.originalText = originalText;
        getBuffer().setLength(0);
        listenToMousePresses(true);
        typeText(text);
        return true;
    }

    /**
     * Adds characters at the end of the text being typed
     */
    public void typeText(String text)
    {
        if (!isTyping())
        {
            return;
        }
        getBuffer().append(text);
        showTypedText();
    }

    /**
     * A node gets its text from a MultiLineString, changed in place; an edge gets a new String each time
     */
    private void showTypedText()
    {
        if (this.typedText != null)
        {
            this.typedText.setText(getBuffer().toString());
        }
        else
        {
            try
            {
                this.typedProperty.getWriteMethod().invoke(this.typedElement, getBuffer().toString());
            }
            catch (ReflectiveOperationException | RuntimeException e)
            {
                e.printStackTrace();
            }
        }
        refresh();
    }

    /**
     * @return the text being typed, as it is given to the element
     */
    private Object getTypedValue()
    {
        return this.typedText != null ? this.typedText : getBuffer().toString();
    }

    /**
     * Erases the last character of the text being typed
     */
    public void deleteLastCharacter()
    {
        if (!isTyping() || getBuffer().length() == 0)
        {
            return;
        }
        int lastCodePoint = getBuffer().codePointBefore(getBuffer().length());
        getBuffer().setLength(getBuffer().length() - Character.charCount(lastCodePoint));
        showTypedText();
    }

    /**
     * Keeps the typed text, as a property edit, so that it can be undone and the diagram gets saved
     */
    public void commitTyping()
    {
        if (!isTyping())
        {
            return;
        }
        ISelectable element = this.typedElement;
        PropertyChangeEvent event = new PropertyChangeEvent(element, this.typedProperty.getName(), this.originalText,
                getTypedValue());
        stopTyping();
        // Undo/redo, auto-save and repainting listen to these
        if (element instanceof INode)
        {
            INode node = (INode) element;
            this.behaviorManager.fireBeforeEditingNode(node);
            this.behaviorManager.fireWhileEditingNode(node, event);
            this.behaviorManager.fireAfterEditingNode(node);
        }
        else if (element instanceof IEdge)
        {
            IEdge edge = (IEdge) element;
            this.behaviorManager.fireBeforeEditingEdge(edge);
            this.behaviorManager.fireWhileEditingEdge(edge, event);
            this.behaviorManager.fireAfterEditingEdge(edge);
        }
        refresh();
    }

    /**
     * Gives the node its former text back
     */
    public void cancelTyping()
    {
        if (!isTyping())
        {
            return;
        }
        try
        {
            this.typedProperty.getWriteMethod().invoke(this.typedElement, this.originalText);
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            e.printStackTrace();
        }
        stopTyping();
        refresh();
    }

    public boolean isTyping()
    {
        return this.typedElement != null;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event)
    {
        if (!isInEditorWindow(event.getComponent()))
        {
            commitTyping();
            return false;
        }
        if (!isTyping())
        {
            return startTypingOnSelectedNode(event);
        }
        if (event.getID() == KeyEvent.KEY_PRESSED)
        {
            int keyCode = event.getKeyCode();
            if (isModifierKey(keyCode))
            {
                return true;
            }
            if (isShortcut(event))
            {
                // Validates first, so that Ctrl+Z undoes the typing and Ctrl+S saves it
                commitTyping();
                return false;
            }
            if (keyCode == KeyEvent.VK_ENTER)
            {
                if (this.typedText != null)
                {
                    typeText("\n");
                }
                else
                {
                    commitTyping(); // an edge text is a single line
                }
            }
            else if (keyCode == KeyEvent.VK_ESCAPE)
            {
                cancelTyping();
            }
            else if (keyCode == KeyEvent.VK_BACK_SPACE)
            {
                deleteLastCharacter();
            }
            return true;
        }
        if (event.getID() == KeyEvent.KEY_TYPED)
        {
            if (isShortcut(event))
            {
                return false;
            }
            if (isTextCharacter(event))
            {
                typeText(String.valueOf(event.getKeyChar()));
            }
            return true;
        }
        return true;
    }

    private boolean startTypingOnSelectedNode(KeyEvent event)
    {
        if (event.getID() != KeyEvent.KEY_TYPED || !isTextCharacter(event) || event.getComponent() instanceof JTextComponent)
        {
            return false;
        }
        if (this.graphToolsBar != null && !isSelectionOrNodeTool(this.graphToolsBar.getSelectedTool()))
        {
            return false;
        }
        List<ISelectable> selection = this.selectionHandler.getSelectedElements();
        if (selection.size() != 1)
        {
            return false;
        }
        String text = String.valueOf(event.getKeyChar());
        if (selection.get(0) instanceof INode)
        {
            return startTyping((INode) selection.get(0), text);
        }
        if (selection.get(0) instanceof IEdge)
        {
            return startTyping((IEdge) selection.get(0), text);
        }
        return false;
    }

    /**
     * A node tool stays selected once its node is created (and selected), which can be written into right away
     */
    private boolean isSelectionOrNodeTool(GraphTool tool)
    {
        return GraphTool.SELECTION_TOOL.equals(tool) || (tool != null && tool.getNodeOrEdge() instanceof INode);
    }

    private boolean isTextCharacter(KeyEvent event)
    {
        char c = event.getKeyChar();
        return c != KeyEvent.CHAR_UNDEFINED && !Character.isISOControl(c) && !isShortcut(event);
    }

    /**
     * AltGr is Ctrl+Alt on Windows and gives characters, as Option (Alt) does on a Mac
     */
    private boolean isShortcut(KeyEvent event)
    {
        if (event.isMetaDown())
        {
            return true;
        }
        if (event.isControlDown())
        {
            return !event.isAltDown();
        }
        return event.isAltDown() && !IS_MAC;
    }

    private boolean isModifierKey(int keyCode)
    {
        return keyCode == KeyEvent.VK_SHIFT || keyCode == KeyEvent.VK_CONTROL || keyCode == KeyEvent.VK_ALT
                || keyCode == KeyEvent.VK_ALT_GRAPH || keyCode == KeyEvent.VK_META || keyCode == KeyEvent.VK_CAPS_LOCK;
    }

    private boolean isInEditorWindow(Component component)
    {
        if (component == null)
        {
            return false;
        }
        Window window = component instanceof Window ? (Window) component : SwingUtilities.getWindowAncestor(component);
        return window != null && window == SwingUtilities.getWindowAncestor(this.editorPart.getSwingComponent());
    }

    /**
     * Finds the first MultiLineString field declared by the node class (then its superclasses) which is a property with a getter
     * and a setter: the name of a class comes before its attributes and methods. Bean properties can't be used for this, as the
     * introspector sorts them by name.
     */
    private PropertyDescriptor getTypedTextProperty(INode node)
    {
        if (node == null)
        {
            return null;
        }
        try
        {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(node.getClass()).getPropertyDescriptors();
            for (Class<?> c = node.getClass(); c != null && c != Object.class; c = c.getSuperclass())
            {
                for (Field field : c.getDeclaredFields())
                {
                    if (!MultiLineString.class.equals(field.getType()) || Modifier.isStatic(field.getModifiers()))
                    {
                        continue;
                    }
                    for (PropertyDescriptor property : properties)
                    {
                        if (property.getName().equals(field.getName()) && MultiLineString.class.equals(property.getPropertyType())
                                && property.getReadMethod() != null && property.getWriteMethod() != null)
                        {
                            return property;
                        }
                    }
                }
            }
        }
        catch (IntrospectionException | RuntimeException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * The text drawn at the middle of an edge, a String property shown in its properties dialog: the middle label of a segmented
     * edge, the label of a state transition. Edges declaring neither (an object reference, for instance) can't be written into.
     */
    private PropertyDescriptor getTypedTextProperty(IEdge edge)
    {
        if (edge == null)
        {
            return null;
        }
        try
        {
            PropertyDescriptor[] properties = Introspector.getBeanInfo(edge.getClass()).getPropertyDescriptors();
            for (String name : EDGE_TEXT_PROPERTIES)
            {
                for (PropertyDescriptor property : properties)
                {
                    if (property.getName().equals(name) && String.class.equals(property.getPropertyType())
                            && property.getReadMethod() != null && property.getWriteMethod() != null)
                    {
                        return property;
                    }
                }
            }
        }
        catch (IntrospectionException | RuntimeException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Takes the text of a new node of the same kind, which has the defaults its constructor gives
     */
    private MultiLineString createDefaultText(INode node, PropertyDescriptor property, Object originalText)
    {
        try
        {
            Method getter = property.getReadMethod();
            Object defaultText = getter.invoke(node.getClass().getDeclaredConstructor().newInstance());
            if (defaultText instanceof MultiLineString)
            {
                return (MultiLineString) defaultText;
            }
        }
        catch (ReflectiveOperationException | RuntimeException e)
        {
            // No public constructor without argument: keeps the defaults of the current text
        }
        // Its text is replaced right after
        return originalText instanceof MultiLineString ? ((MultiLineString) originalText).clone() : new MultiLineString();
    }

    private void stopTyping()
    {
        this.typedElement = null;
        this.typedProperty = null;
        this.originalText = null;
        this.typedText = null;
        getBuffer().setLength(0);
        listenToMousePresses(false);
    }

    private void refresh()
    {
        JComponent component = this.editorPart.getSwingComponent();
        component.invalidate();
        component.repaint();
    }

    private void updateKeyListening()
    {
        boolean isShowing = this.editorPart.getSwingComponent().isShowing();
        if (isShowing == this.isListeningToKeys)
        {
            return;
        }
        KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        if (isShowing)
        {
            focusManager.addKeyEventDispatcher(this);
        }
        else
        {
            focusManager.removeKeyEventDispatcher(this);
            commitTyping();
        }
        this.isListeningToKeys = isShowing;
    }

    /**
     * A click anywhere (menus included) validates the typing before it is processed, so that an undo from the Edit menu undoes it
     */
    private void listenToMousePresses(boolean isListening)
    {
        try
        {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            toolkit.removeAWTEventListener(getMousePressListener());
            if (isListening)
            {
                toolkit.addAWTEventListener(getMousePressListener(), AWTEvent.MOUSE_EVENT_MASK);
            }
        }
        catch (RuntimeException e)
        {
            // Not allowed: clicks on the diagram still validate, through onMousePressed
        }
    }

    @Override
    public void onMousePressed(MouseEvent event)
    {
        commitTyping();
    }

    @Override
    public void onToolSelected(GraphTool selectedTool)
    {
        commitTyping();
    }

    @Override
    public void onNodeSelected(INode node)
    {
        commitTyping();
    }

    @Override
    public void onEdgeSelected(IEdge edge)
    {
        commitTyping();
    }

    @Override
    public void beforeEditingNode(INode node)
    {
        commitTyping();
    }

    @Override
    public void beforeEditingEdge(IEdge edge)
    {
        commitTyping();
    }

    @Override
    public void beforeRemovingSelectedElements()
    {
        commitTyping();
    }

    @Override
    public void beforeAddingNodeAtPoint(INode node, Point2D location)
    {
        commitTyping();
    }

    @Override
    public void beforeAddingEdgeAtPoints(IEdge edge, Point2D startPoint, Point2D endPoint)
    {
        commitTyping();
    }

    @Override
    public void beforeChangingTransitionPointsOnEdge(IEdge edge)
    {
        commitTyping();
    }

    @Override
    public void beforeChangingColorOnElement(IColorable element)
    {
        commitTyping();
    }

    private static final boolean IS_MAC = System.getProperty("os.name", "").toLowerCase().startsWith("mac");

    private StringBuilder getBuffer()
    {
        if (this.buffer == null)
        {
            this.buffer = new StringBuilder();
        }
        return this.buffer;
    }

    private AWTEventListener getMousePressListener()
    {
        if (this.mousePressListener == null)
        {
            this.mousePressListener = new AWTEventListener()
            {
                @Override
                public void eventDispatched(AWTEvent event)
                {
                    if (event.getID() == MouseEvent.MOUSE_PRESSED)
                    {
                        commitTyping();
                    }
                }
            };
        }
        return this.mousePressListener;
    }

    private StringBuilder buffer;
    private AWTEventListener mousePressListener;

    private IEditorPart editorPart;
    private IEditorPartSelectionHandler selectionHandler;
    private IEditorPartBehaviorManager behaviorManager;
    private IGraphToolsBar graphToolsBar;
    private boolean isListeningToKeys;

    /** Text properties of an edge, by preference */
    private static final String[] EDGE_TEXT_PROPERTIES = { "middleLabel", "label" };

    private ISelectable typedElement;
    private PropertyDescriptor typedProperty;
    private Object originalText;
    /** The text given to a node, changed in place, or null for an edge, which gets a String */
    private MultiLineString typedText;

}

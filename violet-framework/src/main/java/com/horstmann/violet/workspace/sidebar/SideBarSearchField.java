package com.horstmann.violet.workspace.sidebar;

import java.awt.AWTEvent;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;

/**
 * Field which searches the tools of the side bar by name, as the user types.
 * <p>
 * The diagram never takes the keyboard focus, and what is typed while an element is selected replaces its text: this field would
 * keep the focus, and the keys, for ever. So it takes the focus only when it is clicked and gives it up on Escape (which also
 * clears it) or on a press anywhere else.
 */
@ResourceBundleBean(resourceReference = SideBar.class)
public class SideBarSearchField extends JTextField
{

    /**
     * @param onSearch called with the text of this field each time it changes
     */
    public SideBarSearchField(final Consumer<String> onSearch)
    {
        ResourceBundleInjector.getInjector().inject(this);
        putClientProperty("JTextField.placeholderText", this.placeholder);
        putClientProperty("JTextField.showClearButton", Boolean.TRUE);
        setToolTipText(this.placeholder);
        getAccessibleContext().setAccessibleName(this.placeholder);
        setFocusable(false);
        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                setFocusable(true);
                requestFocusInWindow();
            }
        });
        addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                listenToPresses(true);
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                listenToPresses(false);
                setFocusable(false);
            }
        });
        addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                {
                    setText("");
                    releaseFocus();
                    e.consume();
                }
            }
        });
        getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                onSearch.accept(getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                onSearch.accept(getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                onSearch.accept(getText());
            }
        });
    }

    @Override
    public void removeNotify()
    {
        listenToPresses(false);
        super.removeNotify();
    }

    private void releaseFocus()
    {
        if (isFocusOwner())
        {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
        }
    }

    private void listenToPresses(boolean isListening)
    {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.removeAWTEventListener(getPressListener());
        if (isListening)
        {
            toolkit.addAWTEventListener(getPressListener(), AWTEvent.MOUSE_EVENT_MASK);
        }
    }

    /**
     * @return what gives the focus up when the mouse is pressed out of this field
     */
    private AWTEventListener getPressListener()
    {
        if (this.pressListener == null)
        {
            this.pressListener = event -> {
                if (event.getID() == MouseEvent.MOUSE_PRESSED && event.getSource() != this)
                {
                    releaseFocus();
                }
            };
        }
        return this.pressListener;
    }

    private AWTEventListener pressListener;

    @ResourceBundleBean(key = "search.placeholder.text")
    private String placeholder;

}

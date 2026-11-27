/*
 Violet - A program for editing UML diagrams.

 Copyright (C) 2007 Cay S. Horstmann (http://horstmann.com)
 Alexandre de Pellegrin (http://alexdp.free.fr);

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.horstmann.violet.framework.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import com.horstmann.violet.framework.injection.bean.ManiocFramework.ManagedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.theme.ThemeManager;

/**
 * This factory manages dialog. It could create dialogs or call a listener if the dialog constrction is delegated to an external
 * program such as an Eclipse plugin
 * 
 * @author Alexandre de Pellegrin
 * 
 */
@ManagedBean
public class DialogFactory
{

    /**
     * Default constructor.
     */
    public DialogFactory()
    {
        ResourceBundleInjector.getInjector().inject(this);
    }


    /**
     * Sets the factory mode. If the mode is set to INTERNAL, the factory creates and shows dialogs.
     * If the mode is set to DELEGATED, the factory calls a listener that must create and show dialogs.
     * 
     * Default mode is INTERNAL, but it can be changed by an external program such as an Eclipse plugin. So, the dialog factory is
     * really flexible and can be used in different contexts.
     * @param factoryMode
     */
    public void setFactoryMode(DialogFactoryMode factoryMode)
    {
        this.factoryMode = factoryMode;
    }


    /**
     * @return current factory mode
     */
    public DialogFactoryMode getFactoryMode()
    {
        return factoryMode;
    }


    public static DialogFactory getInstance() {
        throw new RuntimeException("Java method no longer reachable. Code needs to be refactored.")
;    }
    

    /**
     * Adds a listener that could be invoked if the factory is set to DELEGATED_MODE
     * 
     * @param listener
     */
    public void setListener(DialogFactoryListener listener)
    {
        this.listener = listener;
        performCachedDialogs();
    }

    /**
     * If the dialog factory is configured to DELEGATED_MODE and does not still have a listener that listens and display dialogs,
     * all dialogs are put into a kind of cache. Then the listener is registered, all cached dialogs are sent to it to be displayed.
     */
    private void performCachedDialogs()
    {
        while (!this.delegatedDialogCache.isEmpty())
        {
            Object[] object = (Object[]) this.delegatedDialogCache.get(0);
            JOptionPane optionPane = (JOptionPane) object[0];
            String title = (String) object[1];
            Boolean isModal = (Boolean) object[2];
            invokeListener(optionPane, title, isModal.booleanValue());
            this.delegatedDialogCache.remove(0);
        }
    }

    public void showErrorDialog(String message)
    {
        JOptionPane optionPane = new JOptionPane();
        JLabel label = new JLabel(message);
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        optionPane.setIcon(this.genericErrorImageIcon);
        optionPane.setMessage(label);
        showDialog(optionPane, this.genericErrorTitle, true);
    }

    public void showWarningDialog(String message)
    {
        JOptionPane optionPane = new JOptionPane();
        JLabel label = new JLabel(message);
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        optionPane.setIcon(this.genericWarningImageIcon);
        optionPane.setMessage(label);
        showDialog(optionPane, this.genericWarningTitle, true);
    }

    public void showInformationDialog(String message)
    {
        JOptionPane optionPane = new JOptionPane();
        JLabel label = new JLabel(message);
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        optionPane.setIcon(this.genericInfoImageIcon);
        optionPane.setMessage(label);
        showDialog(optionPane, this.genericInfoTitle, true);
    }

    /**
     * According to the choosen mode : creates and shows a dialog or invokes via the listeners external program that must create and
     * display this dialog.
     * 
     * @param optionPane
     * @param title
     * @param isModal
     */
    public void showDialog(JOptionPane optionPane, String title, boolean isModal)
    {
        updateBackgroundColor(optionPane);
        if (this.factoryMode.equals(DialogFactoryMode.INTERNAL))
        {
            createDialog(optionPane, title, isModal);
        }
        if (this.factoryMode.equals(DialogFactoryMode.DELEGATED))
        {
            invokeListener(optionPane, title, isModal);
        }
    }

    /**
     * Creates (and displays) a dialog
     * 
     * @param optionPane
     * @param title
     * @param isModal
     */
    private void createDialog(final JOptionPane optionPane, String title, boolean isModal)
    {
        Frame mainFrame = getDialogOwner();
        final JDialog dialog = new JDialog(mainFrame, title, isModal);
        dialog.setTitle(title);
        dialog.getContentPane().add(optionPane);
        dialog.pack();
        dialog.setResizable(false);
        optionPane.addPropertyChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent event)
            {
                if (dialog.isVisible() && (event.getPropertyName().equals(JOptionPane.VALUE_PROPERTY))
                        && event.getNewValue() != null && event.getNewValue() != JOptionPane.UNINITIALIZED_VALUE)
                {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            }
        });
        // On a small screen, a page over the window: a floating dialog would hide its buttons off screen or under the keyboard
        if (!new FullPageDialogSupport(dialog, mainFrame).install())
        {
            centerDialog(dialog, mainFrame);
        }
        dialog.setVisible(true);
    }

    /**
     * Updates the jOptionPane background color for all its components
     * 
     * @param optionPane
     */
    private void updateBackgroundColor(Component component)
    {

        // Replace color if only its color is the default background panel color
        // (Could be really optimized)
        if (component.getBackground().equals(JPANEL_BG_COLOR))
        {
            component.setBackground(ThemeManager.getInstance().getTheme().getBackgroundColor());
        }

        if (component instanceof Container)
        {
            Container container = (Container) component;
            for (int i = 0, ub = container.getComponentCount(); i < ub; ++i)
                updateBackgroundColor(container.getComponent(i));
        }

    }

    /**
     * Set dialog location to the center of the owner frame repscting the screen bound
     * 
     * @param dialog
     * @param owner
     */
    private void centerDialog(JDialog dialog, Frame owner)
    {
        Rectangle b = owner.getBounds();

        double x = b.getX() + b.getWidth() / 2 - dialog.getWidth() / 2;
        double y = b.getY() + b.getHeight() / 2 - dialog.getHeight() / 2;
        Dimension screenSize = owner.getToolkit().getScreenSize();
        if (x + dialog.getWidth() > screenSize.getWidth())
        {
            x = screenSize.getWidth() - dialog.getWidth();
        }
        if (y + dialog.getHeight() > screenSize.getHeight())
        {
            y = screenSize.getHeight() - dialog.getHeight();
        }
        Point newLocation = new Point(Math.max((int) x, 0), Math.max((int) y, 0));
        dialog.setLocation(newLocation);
    }

    /**
     * Calls listener that must display a dialog
     * 
     * @param optionPane
     * @param title
     * @param isModal
     */
    private void invokeListener(JOptionPane optionPane, String title, boolean isModal)
    {
        if (listener != null)
        {
            listener.mustDisplayPanel(optionPane, title, isModal);
        }
        if (listener == null)
        {
            this.delegatedDialogCache.add(new Object[]
            {
                    optionPane,
                    title,
                    Boolean.valueOf(isModal)
            });
        }
    }

    /**
     * Sets the dialog owner. This argument is mandatory. Anyway, I think that, as it usually never changes, it is not necessary to
     * give it as argument each time we construct a dialog. Only one time is efficient.
     * 
     * @param owner
     */
    public void setDialogOwner(Frame owner)
    {
        this.dialogOwner = owner;
    }
    
    /**
     * @return current's main frame
     */
    private Frame getDialogOwner() {
        if (this.dialogOwner != null) {
            return this.dialogOwner;
        }
        return getEmergencyDialogOwner();
    }

    /**
     * Return a temporary frame if the real frame owner is not yet set
     * (for example, 
     */
    private Frame getEmergencyDialogOwner()
    {
        JFrame emergencyFrame = new JFrame();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = (int) screenSize.getWidth();
        int screenHeight = (int) screenSize.getHeight();
        emergencyFrame.setBounds(screenWidth / 16, screenHeight / 16, screenWidth * 7 / 8, screenHeight * 7 / 8);
        return emergencyFrame;
    }




    /**
     * Listener that have the responsability to create and display dialogs Note that ther is one and only one listener. Why? Because
     * of the Eclipse framework that create an instance of the application for each document opened, and in the same JVM.
     */
    private DialogFactoryListener listener;

    /**
     * Working mode
     */
    private DialogFactoryMode factoryMode = DialogFactoryMode.INTERNAL;

    /**
     * The frame that owns all dialogs
     */
    private Frame dialogOwner;

    /**
     * JPanel default background color
     */
    private static final Color JPANEL_BG_COLOR = (new JPanel()).getBackground();

    /**
     * Delegated dialog cache (Used if the dialog mode is set to delegated and no listener is registered. So, the foactory stores
     * doalogs that will be displayed when a new listener will be registered
     */
    private List<Object[]> delegatedDialogCache = new ArrayList<Object[]>();

    @ResourceBundleBean(key="dialog.generic.error.icon")
    private ImageIcon genericErrorImageIcon;

    @ResourceBundleBean(key="dialog.generic.error.title")
    private String genericErrorTitle;
    
    @ResourceBundleBean(key="dialog.generic.warning.icon")
    private ImageIcon genericWarningImageIcon;

    @ResourceBundleBean(key="dialog.generic.warning.title")
    private String genericWarningTitle;
    
    @ResourceBundleBean(key="dialog.generic.information.icon")
    private ImageIcon genericInfoImageIcon;

    @ResourceBundleBean(key="dialog.generic.information.title")
    private String genericInfoTitle;

}

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

package com.horstmann.violet.application.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;

import com.horstmann.violet.application.menu.FileMenu;
import com.horstmann.violet.application.swingextension.WelcomeButtonUI;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.plugin.PluginRegistry;
import com.horstmann.violet.framework.theme.ITheme;
import com.horstmann.violet.framework.theme.ThemeManager;

public class WelcomePanel extends JPanel
{

    public WelcomePanel(FileMenu fileMenu)
    {
        ResourceBundleInjector.getInjector().inject(this);
        BeanInjector.getInjector().inject(this);
        this.fileMenu = fileMenu;

        setOpaque(false);
        setLayout(new BorderLayout());
        add(getScrollableOptionPanel(), BorderLayout.CENTER);
        add(getFootTextPanel(), BorderLayout.SOUTH);

    }

    public void paint(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g;
        Paint currentPaint = g2.getPaint();
        ITheme cLAF = ThemeManager.getInstance().getTheme();
        GradientPaint paint = new GradientPaint(getWidth() / 2, -getHeight() / 4, cLAF.getWelcomeBackgroundStartColor(),
                getWidth() / 2, getHeight() + getHeight() / 4, cLAF.getWelcomeBackgroundEndColor());
        g2.setPaint(paint);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setPaint(currentPaint);
        super.paint(g);
    }

    private JScrollPane getScrollableOptionPanel()
    {
        if (this.scrollableOptionPanel == null)
        {
            this.scrollableOptionPanel = new JScrollPane(getOptionPanel());
            this.scrollableOptionPanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            this.scrollableOptionPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
            this.scrollableOptionPanel.setOpaque(false);
            this.scrollableOptionPanel.getViewport().setOpaque(false);
            this.scrollableOptionPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
        }
        return this.scrollableOptionPanel;
    }

    private JPanel getOptionPanel()
    {
        if (this.optionPanel == null)
        {
            this.optionPanel = new JPanel();
            this.optionPanel.setOpaque(false);
            this.optionPanel.setLayout(new BoxLayout(this.optionPanel, BoxLayout.Y_AXIS));
            final JMenu newMenu = this.fileMenu.getFileNewUmlDiagramMenu();
            for (int i = 0; i < newMenu.getItemCount(); i++)
            {
                final JMenuItem item = newMenu.getItem(i);
                boolean isSubMenu = JMenu.class.isInstance(item);
                if (!isSubMenu)
                {
                    String label = item.getText();
                    JButton newDiagramShortcut = new JButton(label.toLowerCase());
                    newDiagramShortcut.setUI(new WelcomeButtonUI());
                    newDiagramShortcut.addActionListener(new ActionListener()
                    {
                        public void actionPerformed(ActionEvent e)
                        {
                            item.doClick();
                        }
                    });
                    this.optionPanel.add(newDiagramShortcut);
                }
                if (isSubMenu)
                {
                    JMenu subMenu = (JMenu) item;
                    String label = subMenu.getText();
                    JLabel title = new JLabel(label.toLowerCase());
                    ITheme cLAF = ThemeManager.getInstance().getTheme();
                    title.setFont(cLAF.getWelcomeBigFont());
                    title.setForeground(cLAF.getWelcomeBackgroundEndColor());
                    title.setBorder(new EmptyBorder(0, 45, 0, 30));
                    title.setAlignmentX(Component.LEFT_ALIGNMENT);
                    this.optionPanel.add(title);
                    JPanel diagramsPanel = new JPanel();
                    diagramsPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
                    diagramsPanel.setOpaque(false);
                    diagramsPanel.setBorder(new EmptyBorder(0, 30, 0, 30));
                    for (int j = 0; j < subMenu.getItemCount(); j++)
                    {
                        final JMenuItem subItem = subMenu.getItem(j);
                        JPanel diagramClickablePanel = getDiagramClickablePanel(subItem);
                        diagramsPanel.add(diagramClickablePanel);
                    }
                    diagramsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    this.optionPanel.add(diagramsPanel);
                }
            }

        }
        return this.optionPanel;
    }

    private JPanel getDiagramClickablePanel(final JMenuItem diagramMenuItem)
    {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        String subLabel = diagramMenuItem.getText();
        JButton newDiagramTextButton = new JButton(subLabel.toLowerCase());
        newDiagramTextButton.setUI(new WelcomeButtonUI());
        newDiagramTextButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                diagramMenuItem.doClick();
            }
        });
        panel.add(newDiagramTextButton);
        JButton newDiagramImageButton = new JButton();
        newDiagramImageButton.setPreferredSize(new Dimension(DIAGRAM_ICON_WIDTH, DIAGRAM_ICON_HEIGHT));
        newDiagramImageButton.setUI(new WelcomeButtonUI());
        Icon sampleDiagramIcon = diagramMenuItem.getRolloverIcon();
        if (sampleDiagramIcon != null)
        {
            ImageIcon resizedImageIcon = getResizedImageIcon((ImageIcon) sampleDiagramIcon);
            newDiagramImageButton.setIcon(resizedImageIcon);
        }
        newDiagramImageButton.addActionListener(new ActionListener()
        {

            @Override
            public void actionPerformed(ActionEvent e)
            {
                diagramMenuItem.doClick();
            }
        });
        panel.add(newDiagramImageButton);
        return panel;
    }

    private JPanel getFootTextPanel()
    {
        if (this.footTextPanel == null)
        {
            this.footTextPanel = new JPanel();
            this.footTextPanel.setOpaque(false);
            this.footTextPanel.setBorder(new EmptyBorder(0, 0, 10, 0));
            this.footTextPanel.setLayout(new BoxLayout(this.footTextPanel, BoxLayout.Y_AXIS));
            this.footTextPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel text = new JLabel(this.footText);
            ITheme cLAF = ThemeManager.getInstance().getTheme();
            text.setFont(cLAF.getWelcomeSmallFont());
            text.setForeground(cLAF.getWelcomeBigForegroundColor());
            text.setAlignmentX(Component.CENTER_ALIGNMENT);

            this.footTextPanel.add(text);
        }

        return this.footTextPanel;
    }

    private ImageIcon getResizedImageIcon(ImageIcon originalImageIcon)
    {
        Image scaledInstance = originalImageIcon.getImage().getScaledInstance(DIAGRAM_ICON_WIDTH, DIAGRAM_ICON_HEIGHT,
                java.awt.Image.SCALE_SMOOTH);
        return new ImageIcon(scaledInstance);
    }

    private JScrollPane scrollableOptionPanel;

    private JPanel optionPanel;

    private JPanel footTextPanel;

    private FileMenu fileMenu;

    private static final int DIAGRAM_ICON_HEIGHT = 240;

    private static final int DIAGRAM_ICON_WIDTH = 240;

    @ResourceBundleBean(key = "welcomepanel.foot_text")
    private String footText;

    /** Plugin registry */
    @InjectedBean
    private PluginRegistry pluginRegistry;

}

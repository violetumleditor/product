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

package com.horstmann.violet.application.help;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;

import com.horstmann.violet.application.swingextension.VerticalAutoScrollPane;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.BeanInjector;
import com.horstmann.violet.framework.injection.bean.ManiocFramework.InjectedBean;
import com.horstmann.violet.framework.injection.resources.ResourceBundleInjector;
import com.horstmann.violet.framework.injection.resources.annotation.ResourceBundleBean;
import com.horstmann.violet.framework.util.VersionChecker;

/**
 * The About dialog box
 */
public class AboutDialog extends JDialog
{

    public AboutDialog(JFrame parent)
    {
        super(parent);
        ResourceBundleInjector.getInjector().inject(this);
        BeanInjector.getInjector().inject(this);
        this.setTitle(this.dialogTitle);
        this.setLocationRelativeTo(null);
        this.setModal(true);
        this.setResizable(false);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());

        JPanel buttonPanel = getButtonPanel();
        this.getContentPane().add(getVersionPanel(), BorderLayout.CENTER);
        this.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        this.pack();
        setLocation(parent);

    }

    private JPanel getButtonPanel()
    {
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        buttonPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        buttonPanel.add(this.nextButton, c);
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        c.weightx = 1;
        c.gridx = 1;
        c.gridy = 0;
        JButton closeButton = new JButton(this.closeButtonLabel);
        closeButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                dispose();
            }
        });
        this.getRootPane().setDefaultButton(closeButton);
        buttonPanel.add(closeButton, c);
        return buttonPanel;
    }

    private void setLocation(JFrame parent)
    {
        Point point = parent.getLocationOnScreen();
        int x = (int) point.getX() + parent.getWidth() / 2;
        int y = (int) point.getY() + parent.getHeight() / 2;
        setLocation(x - getWidth() / 2, y - getHeight() / 2);
    }

    private JPanel getSystemInfoPanel()
    {
        if (this.systemInfoPanel == null)
        {
            JTable table = new JTable();
            AboutTableModel tableModel = new AboutTableModel(new String[]
            {
                    tableSysInfoCol1,
                    tableSysInfoCol2
            });
            try
            {
                Enumeration<?> e = System.getProperties().propertyNames();
                while (e.hasMoreElements())
                {
                    String prop = (String) e.nextElement();
                    String value = System.getProperty(prop);
                    tableModel.addEntry(new String[]
                    {
                            prop,
                            value
                    });
                }
            }
            catch (Exception e)
            {
                // Well, we tried...
            }
            table.setModel(tableModel);
            table.getColumnModel().getColumn(0).setPreferredWidth(170);
            table.getColumnModel().getColumn(1).setPreferredWidth(380);
            JScrollPane pane = new JScrollPane();
            pane.getViewport().add(table);
            this.systemInfoPanel = new JPanel(new BorderLayout());
            this.systemInfoPanel.add(pane);
        }
        this.nextButton.setText(this.showVersionButtonLabel);
        removeActionListener(this.nextButton);
        this.nextButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                getContentPane().remove(getSystemInfoPanel());
                getContentPane().add(getVersionPanel(), BorderLayout.CENTER);
                getContentPane().repaint();
            }
        });
        return this.systemInfoPanel;
    }

    private JPanel getAuthorsPanel()
    {
        if (this.authorsPanel == null)
        {
            JLabel image = new JLabel(this.violetBanner);
            JTextPane tp = new JTextPane();
            try
            {
                URL url = getClass().getResource(this.authorsFilePath);
                tp.setPage(url);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            tp.setBorder(new EmptyBorder(0,0,0,0));
            authorsScrollPane = new VerticalAutoScrollPane();
            authorsScrollPane.setBackground(tp.getBackground());
            authorsScrollPane.setForeground(tp.getForeground());
            authorsScrollPane.setOpaque(true);
            authorsScrollPane.setBorder(new EmptyBorder(20, 20, 20, 20));
            authorsScrollPane.getViewport().add(tp);
            this.authorsPanel = new JPanel(new BorderLayout());
            this.authorsPanel.add(image, BorderLayout.NORTH);
            this.authorsPanel.add(authorsScrollPane, BorderLayout.CENTER);
        }
        this.nextButton.setText(this.showLicenceButtonLabel);
        removeActionListener(this.nextButton);
        this.nextButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                getContentPane().remove(getAuthorsPanel());
                authorsScrollPane.reset();
                getContentPane().add(getLicensePanel(), BorderLayout.CENTER);
                getContentPane().repaint();
            }
        });
        this.authorsScrollPane.animate();
        return this.authorsPanel;
    }

    private JPanel getLicensePanel()
    {
        if (this.licensePanel == null)
        {
            JTextPane tp = new JTextPane();
            JScrollPane js = new JScrollPane();
            js.getViewport().add(tp);

            try
            {
                URL url = getClass().getResource(this.licenceFilePath);
                tp.setPage(url);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            this.licensePanel = new JPanel(new BorderLayout());
            this.licensePanel.add(js);

        }
        this.nextButton.setText(this.showSystemInfoButtonLabel);
        removeActionListener(this.nextButton);
        this.nextButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                getContentPane().remove(getLicensePanel());
                getContentPane().add(getSystemInfoPanel(), BorderLayout.CENTER);
                getContentPane().repaint();
            }
        });
        return this.licensePanel;
    }

    private JPanel getVersionPanel()
    {
        if (this.versionPanel == null)
        {
            String mergedText = this.versionText;
            mergedText = mergedText.replace("${version}", this.versionChecker.getAppVersionNumber());
            mergedText = mergedText.replace("${date}", this.versionChecker.getAppReleaseDate());
            JLabel image = new JLabel(this.image);
            JLabel text = new JLabel(mergedText);
            text.setBorder(new EmptyBorder(0, 0, 0, 4));
            this.versionPanel = new JPanel();
            this.versionPanel.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 1;
            c.gridx = 0;
            c.gridy = 0;
            this.versionPanel.add(image, c);
            c = new GridBagConstraints();
            c.anchor = GridBagConstraints.EAST;
            c.weightx = 1;
            c.gridx = 0;
            c.gridy = 1;
            this.versionPanel.add(text, c);
        }
        this.nextButton.setText(this.showAuthorsButtonLabel);
        removeActionListener(this.nextButton);
        this.nextButton.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                getContentPane().remove(getVersionPanel());
                getContentPane().add(getAuthorsPanel(), BorderLayout.CENTER);
                getContentPane().repaint();
            }
        });
        return this.versionPanel;
    }

    private void removeActionListener(JButton button)
    {
        ActionListener[] listeners = button.getActionListeners();
        for (int i = 0; i < listeners.length; i++)
        {
            button.removeActionListener(listeners[i]);
        }
    }

    private class AboutTableModel extends AbstractTableModel
    {

        public AboutTableModel(String[] columnNames)
        {
            this.columnNames = columnNames;
        }

        public void addEntry(String[] entry)
        {
            data.add(entry);
            fireTableRowsInserted(data.size() - 1, data.size() - 1);
        }

        public int getColumnCount()
        {
            return columnNames.length;
        }

        public int getRowCount()
        {
            return data.size();
        }

        public String getColumnName(int col)
        {
            return columnNames[col];
        }

        public Class<?> getColumnClass(int c)
        {
            return String.class;
        }

        public Object getValueAt(int row, int col)
        {
            String[] entry = (String[]) data.get(row);
            return entry[col];
        }

        public boolean isCellEditable(int row, int col)
        {
            return false;
        }

        private String[] columnNames;
        private List<String[]> data = new ArrayList<String[]>();
    }

    private JPanel versionPanel;

    private JPanel authorsPanel;

    private JPanel licensePanel;

    private JPanel systemInfoPanel;

    private VerticalAutoScrollPane authorsScrollPane;

    private JButton nextButton = new JButton();
    
    @ResourceBundleBean(key="dialog.title")
    private String dialogTitle;
    
    @ResourceBundleBean(key="dialog.button.label")
    private String closeButtonLabel;
    
    @ResourceBundleBean(key="systeminfo.col1")
    private String tableSysInfoCol1;
    
    @ResourceBundleBean(key="systeminfo.col2")
    private String tableSysInfoCol2;
    
    @ResourceBundleBean(key="dialog.button.show_version")
    private String showVersionButtonLabel;
    
    @ResourceBundleBean(key="authors.banner")
    private ImageIcon violetBanner;
    
    @ResourceBundleBean(key="authors.file")
    private String authorsFilePath;
    
    @ResourceBundleBean(key="dialog.button.show_license")
    private String showLicenceButtonLabel;
    
    @ResourceBundleBean(key="license.file")
    private String licenceFilePath;
    
    @ResourceBundleBean(key="dialog.button.show_systeminfo")
    private String showSystemInfoButtonLabel;
    
    @ResourceBundleBean(key="dialog.about.image")
    private ImageIcon image;
    
    @ResourceBundleBean(key="app.version.text")
    private String versionText;
    
    @ResourceBundleBean(key="dialog.button.show_authors")
    private String showAuthorsButtonLabel;

    @InjectedBean
    private VersionChecker versionChecker;

    
}

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

package com.horstmann.violet.framework.propertyeditor;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import com.formdev.flatlaf.FlatClientProperties;
import com.horstmann.violet.framework.injection.resources.ResourceBundleConstant;
import com.horstmann.violet.framework.propertyeditor.customeditor.AbstractDiagramLinkEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.AngleStyleEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.ArrowHeadEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.BentStyleEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.ChoiceListEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.ColorEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.ImageEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.ImageIconEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.LineStyleEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.MultiLineStringEditor;
import com.horstmann.violet.framework.propertyeditor.customeditor.StringEditor;
import com.horstmann.violet.framework.util.SerializableEnumeration;
import com.horstmann.violet.product.diagram.abstracts.property.AngleStyle;
import com.horstmann.violet.product.diagram.abstracts.property.ArrowHead;
import com.horstmann.violet.product.diagram.abstracts.property.BentStyle;
import com.horstmann.violet.product.diagram.abstracts.property.ChoiceList;
import com.horstmann.violet.product.diagram.abstracts.property.LineStyle;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.common.DiagramLink;

/**
 * A component filled with editors for all editable properties of an object.
 */
public class CustomPropertyEditor implements ICustomPropertyEditor
{
    /**
     * Constructs a property sheet that shows the editable properties of a given object.
     * 
     * @param object the object whose properties are being edited
     * @param parent the parent component
     */
    public CustomPropertyEditor(Object bean)
    {
        panel = new JPanel();
        try
        {
            Introspector.flushFromCaches(bean.getClass());
            BeanInfo info = Introspector.getBeanInfo(bean.getClass());
            PropertyDescriptor[] descriptors = (PropertyDescriptor[]) info.getPropertyDescriptors().clone();
            Arrays.sort(descriptors, new Comparator<PropertyDescriptor>()
            {
                public int compare(PropertyDescriptor d1, PropertyDescriptor d2)
                {
                    Integer p1 = (Integer) d1.getValue("priority");
                    Integer p2 = (Integer) d2.getValue("priority");
                    if (p1 == null && p2 == null) return 0;
                    if (p1 == null) return 1;
                    if (p2 == null) return -1;
                    return p1.intValue() - p2.intValue();
                }
            });

            List<Property> properties = new ArrayList<Property>();
            for (int i = 0; i < descriptors.length; i++)
            {
                PropertyEditor editor = getEditor(bean, descriptors[i]);
                if (editor != null)
                {
                    String name = descriptors[i].getName();
                    properties.add(new Property(name, getTitle(name), editor, getEditorComponent(editor)));
                    this.isEditable = true;
                }
            }
            layoutProperties(properties);
        }
        catch (IntrospectionException exception)
        {
            exception.printStackTrace();
        }
    }

    /**
     * Lays out the properties compactly on a grid: a column of names, aligned to the left, and a column of editors. Related
     * properties share a row (see {@link #GROUPS}). Each rich text keeps its own formatting toolbar, with its name centered on it.
     *
     * @param properties to show, in their display order
     */
    private void layoutProperties(List<Property> properties)
    {
        panel.setLayout(new GridBagLayout());
        int row = 0;
        Set<String> laidOut = new HashSet<String>();
        for (Property property : properties)
        {
            if (laidOut.contains(property.name))
            {
                continue;
            }
            List<Property> members = getGroupMembers(property, properties);
            String title = property.title;
            Component editorComponent = property.component;
            boolean fill = isStretchable(property);
            if (members.size() > 1)
            {
                String[] group = getGroup(property.name);
                title = getTitle("group." + group[0]);
                editorComponent = createGroupComponent(group[0], members);
                fill = GROUP_LABELS.equals(group[0]);
                for (Property member : members)
                {
                    laidOut.add(member.name);
                }
            }
            boolean isText = property.editor instanceof MultiLineStringEditor;
            JLabel label = createNameLabel(title);
            int labelTop = 0;
            if (isText)
            {
                MultiLineStringEditor textEditor = (MultiLineStringEditor) property.editor;
                // Centered on the toolbar, inside the one pixel border of the text box
                labelTop = Math.max(0, 1 + (textEditor.getToolbar().getPreferredSize().height - label.getPreferredSize().height) / 2);
                highlightOnFocus(label, textEditor.getTextComponent());
            }

            GridBagConstraints labelConstraints = new GridBagConstraints();
            labelConstraints.gridx = 0;
            labelConstraints.gridy = row;
            labelConstraints.anchor = isText ? GridBagConstraints.NORTHWEST : GridBagConstraints.WEST;
            labelConstraints.insets = new Insets(labelTop, 0, ROW_GAP, COLUMN_GAP);
            panel.add(label, labelConstraints);

            GridBagConstraints editorConstraints = new GridBagConstraints();
            editorConstraints.gridx = 1;
            editorConstraints.gridy = row;
            editorConstraints.weightx = 1;
            editorConstraints.weighty = isText ? 1 : 0;
            editorConstraints.anchor = GridBagConstraints.WEST;
            editorConstraints.fill = isText ? GridBagConstraints.BOTH : (fill ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE);
            editorConstraints.insets = new Insets(0, 0, ROW_GAP, 0);
            panel.add(editorComponent, editorConstraints);
            row++;
        }
    }

    /**
     * Shows the name of a property in the accent color while its editor has the focus, as the border of the editor.
     *
     * @param label name of the property
     * @param editor component which gets the focus
     */
    private static void highlightOnFocus(final JLabel label, JComponent editor)
    {
        final Color normal = label.getForeground();
        editor.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusGained(FocusEvent e)
            {
                Color accent = UIManager.getColor("Component.focusColor");
                label.setForeground(accent != null ? accent : normal);
            }

            @Override
            public void focusLost(FocusEvent e)
            {
                label.setForeground(normal);
            }
        });
    }

    /**
     * @param property a property
     * @param properties all the properties
     * @return the properties of the group of the given property, in display order, or only this property if it has no group
     */
    private static List<Property> getGroupMembers(Property property, List<Property> properties)
    {
        List<Property> members = new ArrayList<Property>();
        String[] group = getGroup(property.name);
        if (group == null)
        {
            members.add(property);
            return members;
        }
        List<String> names = Arrays.asList(group).subList(1, group.length);
        for (Property aProperty : properties)
        {
            if (names.contains(aProperty.name))
            {
                members.add(aProperty);
            }
        }
        return members;
    }

    /**
     * @param propertyName name of a property
     * @return its group (key, then property names), or null
     */
    private static String[] getGroup(String propertyName)
    {
        for (String[] group : GROUPS)
        {
            for (int i = 1; i < group.length; i++)
            {
                if (group[i].equals(propertyName)) return group;
            }
        }
        return null;
    }

    /**
     * @param groupKey key of the group
     * @param members the properties of the group shown on this row
     * @return a component holding the editors of the members
     */
    private Component createGroupComponent(String groupKey, List<Property> members)
    {
        if (GROUP_LABELS.equals(groupKey))
        {
            JPanel fields = new JPanel(new GridLayout(1, members.size(), INNER_GAP, 0));
            fields.setOpaque(false);
            for (Property member : members)
            {
                if (member.component instanceof JTextField)
                {
                    JTextField field = (JTextField) member.component;
                    field.setColumns(5);
                    field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, getShortTitle(member));
                    field.setToolTipText(member.title);
                    field.getAccessibleContext().setAccessibleName(member.title);
                }
                fields.add(member.component);
            }
            return fields;
        }
        JPanel line = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        line.setOpaque(false);
        for (int i = 0; i < members.size(); i++)
        {
            Property member = members.get(i);
            if (i > 0)
            {
                line.add(Box.createHorizontalStrut(COLUMN_GAP));
                if (GROUP_LINE.equals(groupKey))
                {
                    line.add(createNameLabel(getShortTitle(member)));
                    line.add(Box.createHorizontalStrut(INNER_GAP + 2));
                }
            }
            if (member.component instanceof JComponent)
            {
                ((JComponent) member.component).setToolTipText(member.title);
            }
            line.add(member.component);
        }
        return line;
    }

    /**
     * @param property a property
     * @return true if its editor should take the width of the column
     */
    private static boolean isStretchable(Property property)
    {
        return property.component instanceof JTextField;
    }

    /**
     * @param text name of a property
     * @return a label drawn a little lighter than the text, so that the values stand out
     */
    private static JLabel createNameLabel(String text)
    {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        Color foreground = UIManager.getColor("Label.foreground");
        Color background = UIManager.getColor("Panel.background");
        if (foreground != null && background != null)
        {
            float ratio = 0.68f;
            label.setForeground(new Color(Math.round(foreground.getRed() * ratio + background.getRed() * (1 - ratio)),
                    Math.round(foreground.getGreen() * ratio + background.getGreen() * (1 - ratio)),
                    Math.round(foreground.getBlue() * ratio + background.getBlue() * (1 - ratio))));
        }
        return label;
    }

    /**
     * @param key of a property name or a group
     * @return its title in the user's language, with an upper case first letter
     */
    private static String getTitle(String key)
    {
        String title = getString(key.toLowerCase(Locale.ROOT));
        if (title == null) title = key;
        return title.substring(0, Math.min(1, title.length())).toUpperCase() + title.substring(Math.min(1, title.length()));
    }

    /**
     * @param property a property shown after another one on a shared row
     * @return its short title
     */
    private static String getShortTitle(Property property)
    {
        String shortTitle = getString(property.name.toLowerCase(Locale.ROOT) + ".short");
        return shortTitle != null ? shortTitle : property.title;
    }

    /**
     * @param key of a node and edge string
     * @return the string, or null if missing
     */
    private static String getString(String key)
    {
        try
        {
            return ResourceBundle.getBundle(ResourceBundleConstant.NODE_AND_EDGE_STRINGS, Locale.getDefault()).getString(key);
        }
        catch (MissingResourceException e)
        {
            return null;
        }
    }

    /**
     * An editable property, as shown in the sheet
     */
    private static class Property
    {
        Property(String name, String title, PropertyEditor editor, Component component)
        {
            this.name = name;
            this.title = title;
            this.editor = editor;
            this.component = component;
        }

        final String name;
        final String title;
        final PropertyEditor editor;
        final Component component;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.horstmann.violet.framework.display.clipboard.IPropertyEditor#getAWTComponent()
     */
    public JComponent getAWTComponent()
    {
        return panel;
    }

    /**
     * Gets the property editor for a given property, and wires it so that it updates the given object.
     * 
     * @param bean the object whose properties are being edited
     * @param descriptor the descriptor of the property to be edited
     * @return a property editor that edits the property with the given descriptor and updates the given object
     */
    private PropertyEditor getEditor(final Object bean, final PropertyDescriptor descriptor)
    {
        try
        {
            Method getter = descriptor.getReadMethod();
            if (getter == null) return null;
            final Method setter = descriptor.getWriteMethod();
            if (setter == null) return null;
            Class<?> type = descriptor.getPropertyType();
            final PropertyEditor editor;
            Class<?> editorClass = descriptor.getPropertyEditorClass();
            if (editorClass == null && editors.containsKey(type)) editorClass = (Class<?>) editors.get(type);
            if (editorClass != null) editor = (PropertyEditor) editorClass.getDeclaredConstructor().newInstance();
            else editor = PropertyEditorManager.findEditor(type);
            if (editor == null) return null;
            if (editor instanceof CustomPropertyEditorSupport)
            {
                ((CustomPropertyEditorSupport) editor).setPropertyName(descriptor.getName());
            }

            Object value = getter.invoke(bean);
            editor.setValue(value);

            if (!isKnownImmutable(type))
            {
                try
                {
                    value = value.getClass().getMethod("clone").invoke(value);
                }
                catch (Throwable t)
                {
                    // we tried
                }
            }
            final Object oldValue = value;
            editor.addPropertyChangeListener(new PropertyChangeListener()
            {
                public void propertyChange(PropertyChangeEvent event)
                {
                    try
                    {
                        Object newValue = editor.getValue();
                        setter.invoke(bean, newValue);
                        firePropertyStateChanged(new PropertyChangeEvent(bean, descriptor.getName(), oldValue, newValue));
                    }
                    catch (IllegalAccessException exception)
                    {
                        exception.printStackTrace();
                    }
                    catch (InvocationTargetException exception)
                    {
                        exception.printStackTrace();
                    }
                }
            });
            return editor;
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException exception)
        {
            exception.printStackTrace();
            return null;
        }
    }

    private static boolean isKnownImmutable(Class<?> type)
    {
        if (type.isPrimitive()) return true;
        if (knownImmutables.contains(type)) return true;
        if (SerializableEnumeration.class.isAssignableFrom(type)) return true;
        return false;
    }

    /**
     * Wraps a property editor into a component.
     * 
     * @param editor the editor to wrap
     * @return a button (if there is a custom editor), combo box (if the editor has tags), or text field (otherwise)
     */
    private Component getEditorComponent(final PropertyEditor editor)
    {
        String[] tags = editor.getTags();
        String text = editor.getAsText();
        if (editor.supportsCustomEditor())
        {
            return editor.getCustomEditor();
            /*
             * // Make a button that pops up the custom editor final JButton button = new JButton(); // if the editor is paintable,
             * have it paint an icon if (editor.isPaintable()) { button.setIcon(new Icon() { public int getIconWidth() { return
             * WIDTH - 8; } public int getIconHeight() { return HEIGHT - 8; }
             * 
             * public void paintIcon(Component c, Graphics g, int x, int y) { g.translate(x, y); Rectangle r = new Rectangle(0, 0,
             * getIconWidth(), getIconHeight()); Color oldColor = g.getColor(); g.setColor(Color.BLACK); editor.paintValue(g, r);
             * g.setColor(oldColor); g.translate(-x, -y); } }); } else button.setText(buttonText(text)); // pop up custom editor
             * when button is clicked button.addActionListener(new ActionListener() { public void actionPerformed(ActionEvent event)
             * { final Component customEditor = editor.getCustomEditor();
             * 
             * JOptionPane.showMessageDialog(parent, customEditor); // This should really be showInternalMessageDialog, // but then
             * you get awful focus behavior with JDK 5.0 // (i.e. the property sheet retains focus). In // particular, the color
             * dialog never works.
             * 
             * if (editor.isPaintable()) button.repaint(); else button.setText(buttonText(editor.getAsText())); } }); return button;
             */
        }
        else if (editor instanceof CustomPropertyEditorSupport && ((CustomPropertyEditorSupport) editor).getChoiceCount() > 0
                && ((CustomPropertyEditorSupport) editor).getIcon(0) != null)
        {
            return new ChoiceButtonGroup((CustomPropertyEditorSupport) editor);
        }
        else if (tags != null)
        {
            // make a combo box that shows all tags
            final JComboBox<String> comboBox = new JComboBox<String>(tags);
            comboBox.setSelectedItem(text);
            comboBox.addItemListener(new ItemListener()
            {
                public void itemStateChanged(ItemEvent event)
                {
                    if (event.getStateChange() == ItemEvent.SELECTED) editor.setAsText((String) comboBox.getSelectedItem());
                }
            });
            return comboBox;
        }
        else
        {
            final JTextField textField = new JTextField(text, 10);
            textField.getDocument().addDocumentListener(new DocumentListener()
            {
                public void insertUpdate(DocumentEvent e)
                {
                    try
                    {
                        editor.setAsText(textField.getText());
                    }
                    catch (IllegalArgumentException exception)
                    {
                    }
                }

                public void removeUpdate(DocumentEvent e)
                {
                    try
                    {
                        editor.setAsText(textField.getText());
                    }
                    catch (IllegalArgumentException exception)
                    {
                    }
                }

                public void changedUpdate(DocumentEvent e)
                {
                }
            });
            return textField;
        }
    }

    /*
     * Formats text for the button that pops up a custom editor.
     * 
     * @param text the property value as text @return the text to put on the button private static String buttonText(String text) {
     * if (text == null || text.equals("")) return " "; if (text.length() > MAX_TEXT_LENGTH) return text.substring(0,
     * MAX_TEXT_LENGTH) + "..."; return text; }
     */

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.horstmann.violet.framework.display.clipboard.IPropertyEditor#addPropertyChangeListener(java.beans.PropertyChangeListener)
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        synchronized (listeners)
        {
            listeners.add(listener);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.horstmann.violet.framework.display.clipboard.IPropertyEditor#removePropertyChangeListener(java.beans.PropertyChangeListener
     * )
     */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        synchronized (listeners)
        {
            listeners.remove(listener);
        }
    }

    /**
     * Notifies all listeners of a state change.
     * 
     * @param event the event to propagate
     */
    private void firePropertyStateChanged(PropertyChangeEvent event)
    {
        synchronized (listeners)
        {
            for (PropertyChangeListener listener : listeners)
                listener.propertyChange(event);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.horstmann.violet.framework.display.clipboard.IPropertyEditor#isEditable()
     */
    public boolean isEditable()
    {
        return this.isEditable;
    }

    private ArrayList<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();

    /**
     * Flag indicating that the bean has a minimum of one editable property
     */
    private boolean isEditable = false;

    private JPanel panel;

    private static Map<Class<?>, Class<? extends PropertyEditor>> editors;

    static
    {
        editors = new HashMap<Class<?>, Class<? extends PropertyEditor>>();
        editors.put(AngleStyle.class, AngleStyleEditor.class);
        editors.put(ArrowHead.class, ArrowHeadEditor.class);
        editors.put(BentStyle.class, BentStyleEditor.class);
        editors.put(ChoiceList.class, ChoiceListEditor.class);
        editors.put(java.awt.Color.class, ColorEditor.class);
        editors.put(DiagramLink.class, AbstractDiagramLinkEditor.class);
        editors.put(LineStyle.class, LineStyleEditor.class);
        editors.put(MultiLineString.class, MultiLineStringEditor.class);
        editors.put(String.class, StringEditor.class);
        editors.put(ImageIcon.class, ImageIconEditor.class);
        editors.put(java.awt.Image.class, ImageEditor.class);
    }

    private static final String GROUP_LABELS = "labels";

    private static final String GROUP_LINE = "line";

    /**
     * Properties shown on one row when at least two of them are present: key of the row title (group.&lt;key&gt; in the node and
     * edge strings), then property names, in the order of the row.
     */
    private static final String[][] GROUPS =
    {
            { GROUP_LABELS, "startLabel", "middleLabel", "endLabel" },
            { "arrows", "startArrowHead", "endArrowHead" },
            { GROUP_LINE, "lineStyle", "angleStyle" }
    };

    private static final int ROW_GAP = 6;

    private static final int COLUMN_GAP = 10;

    private static final int INNER_GAP = 4;

    private static Set<Class<?>> knownImmutables = new HashSet<Class<?>>();

    static
    {
        knownImmutables.add(String.class);
        knownImmutables.add(Integer.class);
        knownImmutables.add(Boolean.class);
        knownImmutables.add(Double.class);
    }
}

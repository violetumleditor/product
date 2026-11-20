package com.horstmann.violet.product.diagram.bpmn.editor;

import java.awt.Graphics2D;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.swing.Icon;

import com.horstmann.violet.framework.propertyeditor.CustomPropertyEditorSupport;
import com.horstmann.violet.framework.propertyeditor.customeditor.ChoiceIcons;
import com.horstmann.violet.product.diagram.bpmn.ProcessDiagramConstant;

/**
 * Edits a BPMN choice (event trigger, task type...) with chips (icon + name from the BPMN strings).
 */
abstract class ProcessChoiceEditor extends CustomPropertyEditorSupport
{

    /**
     * @param choices the values offered, in display order
     * @param keyPrefix prefix of their names in the BPMN strings, followed by a dot and the value in lower case
     */
    ProcessChoiceEditor(Enum<?>[] choices, String keyPrefix)
    {
        super(getNames(choices), choices);
        this.choices = choices;
        this.keyPrefix = keyPrefix;
    }

    private static String[] getNames(Enum<?>[] choices)
    {
        String[] names = new String[choices.length];
        for (int i = 0; i < choices.length; i++)
        {
            names[i] = choices[i].name();
        }
        return names;
    }

    @Override
    public Icon getIcon(int index)
    {
        final Enum<?> choice = this.choices[index];
        return ChoiceIcons.custom(g2 -> paintChoice(g2, choice));
    }

    /**
     * Paints the icon of a choice
     *
     * @param g2 graphics with the color and the stroke set, in a 24 x 14 area at the origin
     * @param choice the value to show
     */
    protected abstract void paintChoice(Graphics2D g2, Enum<?> choice);

    /**
     * BPMN markers mean nothing to who doesn't know BPMN: each value is named beside its icon
     */
    @Override
    public boolean isShownAsChips()
    {
        return true;
    }

    @Override
    public String getDisplayName(int index)
    {
        String key = this.keyPrefix + "." + this.choices[index].name().toLowerCase(Locale.ROOT);
        try
        {
            return ResourceBundle.getBundle(ProcessDiagramConstant.PROCESS_DIAGRAM_STRINGS, Locale.getDefault()).getString(key);
        }
        catch (MissingResourceException e)
        {
            return this.choices[index].name();
        }
    }

    private final Enum<?>[] choices;

    private final String keyPrefix;

}

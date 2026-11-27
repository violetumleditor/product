package com.horstmann.violet.framework.propertyeditor.customeditor;

import java.awt.Component;
import java.awt.FlowLayout;
import java.beans.PropertyEditorSupport;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Edits the angle of a turned node ({@code IRotatableNode}), in degrees: a spinner going round by steps of 15 degrees (from 345 to 0
 * and back), any angle typed. The value is a Double, null for 0.
 */
public class RotationEditor extends PropertyEditorSupport
{
    @Override
    public boolean supportsCustomEditor()
    {
        return true;
    }

    @Override
    public Component getCustomEditor()
    {
        Object value = getValue();
        double degrees = value instanceof Number ? ((Number) value).doubleValue() : 0;
        JSpinner spinner = new JSpinner(new CircularModel(degrees));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0.##"));
        ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField().setColumns(4);
        spinner.addChangeListener(e -> {
            double newDegrees = ((Number) spinner.getValue()).doubleValue();
            setValue(newDegrees == 0 ? null : Double.valueOf(newDegrees));
        });
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.add(spinner);
        panel.add(new JLabel(" °"));
        return panel;
    }

    /**
     * Steps of {@link #STEP} degrees, from one multiple to the next, going round from 0 to 360
     */
    private static class CircularModel extends SpinnerNumberModel
    {
        CircularModel(double degrees)
        {
            super(Double.valueOf(degrees), Double.valueOf(0), Double.valueOf(MAXIMUM), Double.valueOf(STEP));
        }

        @Override
        public Object getNextValue()
        {
            double value = getNumber().doubleValue();
            return Double.valueOf(wrap(Math.floor(value / STEP + 1e-9) * STEP + STEP));
        }

        @Override
        public Object getPreviousValue()
        {
            double value = getNumber().doubleValue();
            return Double.valueOf(wrap(Math.ceil(value / STEP - 1e-9) * STEP - STEP));
        }

        private double wrap(double degrees)
        {
            return ((degrees % 360) + 360) % 360;
        }
    }

    private static final double STEP = 15;

    /** Just below a whole turn, which is 0 */
    private static final double MAXIMUM = 359.99;
}

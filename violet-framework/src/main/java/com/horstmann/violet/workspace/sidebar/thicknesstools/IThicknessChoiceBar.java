package com.horstmann.violet.workspace.sidebar.thicknesstools;

import com.horstmann.violet.workspace.sidebar.ISideBarElement;

public interface IThicknessChoiceBar extends ISideBarElement
{

    public void addThicknessChangeListener(IThicknessChangeListener listener);

    /**
     * Shows a thickness as the current one, without applying it to anything: the one of the selected element
     *
     * @param thickness
     */
    public void setShownThickness(int thickness);

    /**
     * @return the thickness shown as the current one: the one chosen last, or the one of the selected element
     */
    public int getShownThickness();

    /**
     * @param listener run each time the thickness shown as the current one changes
     */
    public void addShownThicknessListener(Runnable listener);

}

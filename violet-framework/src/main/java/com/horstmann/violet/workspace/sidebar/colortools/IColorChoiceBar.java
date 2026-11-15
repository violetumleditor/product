package com.horstmann.violet.workspace.sidebar.colortools;

import com.horstmann.violet.workspace.sidebar.ISideBarElement;

public interface IColorChoiceBar extends ISideBarElement
{

    public void addColorChoiceChangeListener(IColorChoiceChangeListener listener);

    /**
     * Shows a color choice as the current one, without applying it to anything: the one of the selected element
     *
     * @param colorChoice
     */
    public void setShownColorChoice(ColorChoice colorChoice);

    /**
     * @return the color choice shown as the current one: the one chosen last, or the one of the selected element
     */
    public ColorChoice getShownColorChoice();

    /**
     * @param listener run each time the color choice shown as the current one changes
     */
    public void addShownColorChoiceListener(Runnable listener);

}

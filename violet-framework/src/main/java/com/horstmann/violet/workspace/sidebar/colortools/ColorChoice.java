package com.horstmann.violet.workspace.sidebar.colortools;

import java.awt.Color;

public class ColorChoice
{

    private Color backgroundColor;

    private Color borderColor;

    public ColorChoice(Color backgroundColor, Color borderColor)
    {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
    }

    public Color getBackgroundColor()
    {
        return backgroundColor;
    }

    public Color getBorderColor()
    {
        return borderColor;
    }

}

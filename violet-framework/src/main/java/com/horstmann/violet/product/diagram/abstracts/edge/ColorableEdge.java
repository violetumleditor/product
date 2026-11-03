package com.horstmann.violet.product.diagram.abstracts.edge;

import java.awt.Color;

import com.horstmann.violet.product.diagram.abstracts.IColorable;
import com.horstmann.violet.workspace.sidebar.colortools.ColorToolsBarPanel;

public abstract class ColorableEdge extends AbstractEdge implements IColorable {


    @Override
    public void setBackgroundColor(Color bgColor)
    {
        backgroundColor = bgColor;
    }

    @Override
    public final Color getBackgroundColor()
    {
        if(null == backgroundColor)
        {
            return ColorToolsBarPanel.DEFAULT_COLOR.getBackgroundColor();
        }
        return backgroundColor;
    }

    @Override
    public void setBorderColor(Color borderColor)
    {
        this.borderColor = borderColor;
    }

    @Override
    public final Color getBorderColor()
    {
        if(null == borderColor)
        {
            return ColorToolsBarPanel.DEFAULT_COLOR.getBorderColor();
        }
        return borderColor;
    }

    @Override
    public void setTextColor(Color textColor)
    {
        this.textColor = textColor;
    }

    @Override
    public final Color getTextColor()
    {
    	if (null == textColor)
        {
            return ColorToolsBarPanel.DEFAULT_TEXT_COLOR;
        }
        return textColor;
    }

    
    private Color backgroundColor;
    private Color borderColor;
    private Color textColor;

}

package com.horstmann.violet.workspace.editorpart;

import java.awt.Graphics2D;

import com.horstmann.violet.product.diagram.abstracts.IGridSticker;


public interface IGrid
{

    /**
     * Draws this grid inside a rectangle.
     * 
     * @param g2 the graphics context
     * @param bounds the bounding rectangle
     */
    public abstract void paint(Graphics2D g2);

    /**
     * 
     * @return the corrector able to stick points on this grid
     */
    public abstract IGridSticker getGridSticker();

    /**
     * @return width of a grid portion (depending on the grid global size)
     */
    public abstract double getSnappingWidth();

    /**
     * @return height of a grid portion (depending on the grid global size)
     */
    public abstract double getSnappingHeight();

}
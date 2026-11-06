package com.horstmann.violet.workspace.editorpart;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import com.horstmann.violet.product.diagram.abstracts.IGridSticker;


public class EmptyGrid implements IGrid
{

    @Override
    public double getSnappingHeight()
    {
        return 0;
    }

    @Override
    public double getSnappingWidth()
    {
        return 0;
    }

    @Override
    public void paint(Graphics2D g2)
    {
        // TODO Auto-generated method stub

    }

    @Override
    public IGridSticker getGridSticker()
    {
        // TODO Auto-generated method stub
        return new IGridSticker()
        {
            
            @Override
            public Rectangle2D snap(Rectangle2D r)
            {
                return r;
            }
            
            @Override
            public Point2D snap(Point2D p)
            {
                return p;
            }
        };
    }
    
}

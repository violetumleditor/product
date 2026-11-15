package com.horstmann.violet.workspace.sidebar.graphtools;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Grid of tiles on a fixed number of columns of equal width. Unlike GridLayout, it leaves no hole for a hidden component (the
 * tiles a search filters out) and each row is as high as its highest tile (a tool name may take two lines).
 */
class TileGridLayout implements LayoutManager
{

    /**
     * @param columns number of columns
     * @param gap between tiles, in pixels
     */
    TileGridLayout(int columns, int gap)
    {
        this.columns = columns;
        this.gap = gap;
    }

    @Override
    public void addLayoutComponent(String name, Component comp)
    {
    }

    @Override
    public void removeLayoutComponent(Component comp)
    {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent)
    {
        List<Component> tiles = getVisibleTiles(parent);
        Insets insets = parent.getInsets();
        int tileWidth = 0;
        for (Component aTile : tiles)
        {
            tileWidth = Math.max(tileWidth, aTile.getPreferredSize().width);
        }
        int height = 0;
        for (int first = 0; first < tiles.size(); first += this.columns)
        {
            height += getRowHeight(tiles, first) + (first > 0 ? this.gap : 0);
        }
        int width = this.columns * tileWidth + (this.columns - 1) * this.gap;
        return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent)
    {
        return preferredLayoutSize(parent);
    }

    @Override
    public void layoutContainer(Container parent)
    {
        List<Component> tiles = getVisibleTiles(parent);
        Insets insets = parent.getInsets();
        int availableWidth = parent.getWidth() - insets.left - insets.right - (this.columns - 1) * this.gap;
        int tileWidth = Math.max(0, availableWidth / this.columns);
        int y = insets.top;
        for (int first = 0; first < tiles.size(); first += this.columns)
        {
            int rowHeight = getRowHeight(tiles, first);
            for (int i = first; i < Math.min(first + this.columns, tiles.size()); i++)
            {
                int x = insets.left + (i - first) * (tileWidth + this.gap);
                tiles.get(i).setBounds(x, y, tileWidth, rowHeight);
            }
            y += rowHeight + this.gap;
        }
    }

    private int getRowHeight(List<Component> tiles, int first)
    {
        int rowHeight = 0;
        for (int i = first; i < Math.min(first + this.columns, tiles.size()); i++)
        {
            rowHeight = Math.max(rowHeight, tiles.get(i).getPreferredSize().height);
        }
        return rowHeight;
    }

    private List<Component> getVisibleTiles(Container parent)
    {
        List<Component> tiles = new ArrayList<Component>();
        for (Component aComponent : parent.getComponents())
        {
            if (aComponent.isVisible())
            {
                tiles.add(aComponent);
            }
        }
        return tiles;
    }

    private final int columns;
    private final int gap;

}

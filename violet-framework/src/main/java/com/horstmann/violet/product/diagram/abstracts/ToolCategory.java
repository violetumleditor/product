package com.horstmann.violet.product.diagram.abstracts;

import java.util.List;

/**
 * A named group of node and edge prototypes of a type of diagram, for the diagrams which have many of them: the side bar shows
 * one collapsible group per category, and one button per category once reduced. Declared by a diagram, see
 * {@link IGraph#getToolCategories()}.
 *
 * @param name shown to the user, already translated
 * @param prototypes the node and edge prototypes of the category, among those of the diagram, in display order
 */
public record ToolCategory(String name, List<Object> prototypes)
{

    /**
     * Category giving its prototypes one after the other
     */
    public ToolCategory(String name, Object... prototypes)
    {
        this(name, List.of(prototypes));
    }

}

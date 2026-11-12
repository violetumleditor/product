package com.horstmann.violet.product.diagram.abstracts;

import java.util.List;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * A rule of a type of diagram: the kinds of edges that can go from a kind of node to another one. Declared by each diagram, see
 * {@link AbstractGraph#getConnectionRules()}.
 *
 * @param startNodeType the kind of the start node (its subclasses too)
 * @param endNodeType the kind of the end node (its subclasses too)
 * @param edgeTypes the kinds of edges that can go from the first to the second
 * @param edgePrototypes edge prototypes that can go from the first to the second, for the diagrams whose prototypes share the
 *            same class and differ by their properties only (use case diagram)
 */
public record ConnectionRule(Class<? extends INode> startNodeType, Class<? extends INode> endNodeType,
        List<Class<? extends IEdge>> edgeTypes, List<IEdge> edgePrototypes)
{

    /**
     * Rule giving kinds of edges, one after the other
     */
    @SafeVarargs
    public ConnectionRule(Class<? extends INode> startNodeType, Class<? extends INode> endNodeType,
            Class<? extends IEdge>... edgeTypes)
    {
        this(startNodeType, endNodeType, List.of(edgeTypes), List.of());
    }

    /**
     * Rule giving edge prototypes, one after the other
     */
    public ConnectionRule(Class<? extends INode> startNodeType, Class<? extends INode> endNodeType, IEdge... edgePrototypes)
    {
        this(startNodeType, endNodeType, List.of(), List.of(edgePrototypes));
    }

    /**
     * @param prototype one of the edge prototypes of the diagram
     * @return true if this rule lets this prototype go from the start node to the end node
     */
    public boolean allows(IEdge prototype, INode start, INode end)
    {
        if (!this.startNodeType.isInstance(start) || !this.endNodeType.isInstance(end))
        {
            return false;
        }
        if (this.edgePrototypes.contains(prototype))
        {
            return true;
        }
        for (Class<? extends IEdge> anEdgeType : this.edgeTypes)
        {
            if (anEdgeType.isInstance(prototype))
            {
                return true;
            }
        }
        return false;
    }

}

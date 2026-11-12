package com.horstmann.violet.product.diagram.classes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.ConnectionRule;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.classes.edges.AggregationEdge;
import com.horstmann.violet.product.diagram.classes.edges.AssociationEdge;
import com.horstmann.violet.product.diagram.classes.edges.CompositionEdge;
import com.horstmann.violet.product.diagram.classes.edges.DependencyEdge;
import com.horstmann.violet.product.diagram.classes.edges.InheritanceEdge;
import com.horstmann.violet.product.diagram.classes.edges.InterfaceInheritanceEdge;
import com.horstmann.violet.product.diagram.classes.nodes.ClassNode;
import com.horstmann.violet.product.diagram.classes.nodes.InterfaceNode;
import com.horstmann.violet.product.diagram.classes.nodes.PackageNode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.NoteNode;

/**
 * A UML class diagram.
 */
public class ClassDiagramGraph extends AbstractGraph
{

    public List<INode> getNodePrototypes()
    {
        return NODE_PROTOTYPES;
    }

    public List<IEdge> getEdgePrototypes()
    {
        return EDGE_PROTOTYPES;
    }

    @Override
    protected List<ConnectionRule> getConnectionRules()
    {
        return CONNECTION_RULES;
    }

    private static final List<INode> NODE_PROTOTYPES = new ArrayList<INode>();

    private static final List<IEdge> EDGE_PROTOTYPES = new ArrayList<IEdge>();

    /**
     * Start node type, end node type, edge types that can go from the first to the second, as in UML: a class inherits from a
     * class and is made of classes (aggregation, composition); it implements an interface, and may refer to it (association) or
     * use it (dependency), but neither inherits from it nor is made of it. An interface only extends interfaces, and only uses
     * classes. Packages only depend on each other or on what they hold.
     */
    private static final List<ConnectionRule> CONNECTION_RULES = List.of(
            new ConnectionRule(ClassNode.class, ClassNode.class, DependencyEdge.class, InheritanceEdge.class, AssociationEdge.class,
                    AggregationEdge.class, CompositionEdge.class),
            new ConnectionRule(ClassNode.class, InterfaceNode.class, DependencyEdge.class, InterfaceInheritanceEdge.class,
                    AssociationEdge.class),
            new ConnectionRule(InterfaceNode.class, InterfaceNode.class, DependencyEdge.class, InheritanceEdge.class),
            new ConnectionRule(InterfaceNode.class, ClassNode.class, DependencyEdge.class),
            new ConnectionRule(PackageNode.class, PackageNode.class, DependencyEdge.class),
            new ConnectionRule(PackageNode.class, ClassNode.class, DependencyEdge.class),
            new ConnectionRule(PackageNode.class, InterfaceNode.class, DependencyEdge.class),
            new ConnectionRule(ClassNode.class, PackageNode.class, DependencyEdge.class),
            new ConnectionRule(InterfaceNode.class, PackageNode.class, DependencyEdge.class));

    static
    {
        ResourceBundle rs = ResourceBundle.getBundle(ClassDiagramConstant.CLASS_DIAGRAM_STRINGS, Locale.getDefault());

        ClassNode node0 = new ClassNode();
        node0.setToolTip(rs.getString("node0.tooltip"));
        NODE_PROTOTYPES.add(node0);

        InterfaceNode node1 = new InterfaceNode();
        node1.setToolTip(rs.getString("node1.tooltip"));
        NODE_PROTOTYPES.add(node1);

        PackageNode node2 = new PackageNode();
        node2.setToolTip(rs.getString("node2.tooltip"));
        NODE_PROTOTYPES.add(node2);

        NoteNode node3 = new NoteNode();
        node3.setToolTip(rs.getString("node3.tooltip"));
        NODE_PROTOTYPES.add(node3);

        ImageNode node4 = new ImageNode();
        node4.setToolTip(rs.getString("node4.tooltip"));
        NODE_PROTOTYPES.add(node4);

        ContainerNode node5 = new ContainerNode();
        node5.setToolTip(rs.getString("node5.tooltip"));
        node5.enableToolIcon();
        NODE_PROTOTYPES.add(node5);

        FreeTextNode freeTextNode = new FreeTextNode();
        freeTextNode.setToolTip(rs.getString("freetext.tooltip"));
        freeTextNode.enableToolIcon();
        NODE_PROTOTYPES.add(freeTextNode);

        DependencyEdge dependency = new DependencyEdge();
        dependency.setToolTip(rs.getString("edge0.tooltip"));
        EDGE_PROTOTYPES.add(dependency);

        InheritanceEdge inheritance = new InheritanceEdge();
        inheritance.setToolTip(rs.getString("edge1.tooltip"));
        EDGE_PROTOTYPES.add(inheritance);

        InterfaceInheritanceEdge interfaceInheritance = new InterfaceInheritanceEdge();
        interfaceInheritance.setToolTip(rs.getString("edge2.tooltip"));
        EDGE_PROTOTYPES.add(interfaceInheritance);

        AssociationEdge association = new AssociationEdge();
        association.setToolTip(rs.getString("edge3.tooltip"));
        EDGE_PROTOTYPES.add(association);

        AggregationEdge aggregation = new AggregationEdge();
        aggregation.setToolTip(rs.getString("edge4.tooltip"));
        EDGE_PROTOTYPES.add(aggregation);

        CompositionEdge composition = new CompositionEdge();
        composition.setToolTip(rs.getString("edge5.tooltip"));
        EDGE_PROTOTYPES.add(composition);

        GenericEdge genericEdge = new GenericEdge();
        genericEdge.setToolTip(rs.getString("edge6.tooltip"));
        EDGE_PROTOTYPES.add(genericEdge);
    }

}

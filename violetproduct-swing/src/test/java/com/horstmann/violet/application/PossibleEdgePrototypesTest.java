package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.activity.ActivityNode;
import com.horstmann.violet.product.diagram.activity.ActivityTransitionEdge;
import com.horstmann.violet.product.diagram.activity.DecisionNode;
import com.horstmann.violet.product.diagram.activity.ScenarioEndNode;
import com.horstmann.violet.product.diagram.activity.ScenarioStartNode;
import com.horstmann.violet.product.diagram.activity.SynchronizationBarNode;
import com.horstmann.violet.product.diagram.classes.ClassDiagramGraph;
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
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.NoteNode;
import com.horstmann.violet.product.diagram.sequence.ActivationBarNode;
import com.horstmann.violet.product.diagram.sequence.CallEdge;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.ReturnEdge;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.product.diagram.state.CircularFinalStateNode;
import com.horstmann.violet.product.diagram.state.CircularInitialStateNode;
import com.horstmann.violet.product.diagram.state.StateDiagramGraph;
import com.horstmann.violet.product.diagram.state.StateNode;
import com.horstmann.violet.product.diagram.state.StateTransitionEdge;
import com.horstmann.violet.product.diagram.usecase.ActorNode;
import com.horstmann.violet.product.diagram.usecase.UseCaseDiagramGraph;
import com.horstmann.violet.product.diagram.usecase.UseCaseNode;

/**
 * Each type of diagram declares its connection rules (start node type, end node type, edge types): the link handle offers the edges
 * they allow, and the generic edge only when there is none, or for an element common to all diagrams
 */
class PossibleEdgePrototypesTest
{

    @Test
    void shouldNotLinkADecisionToASynchronizationBar()
    {
        IGraph graph = new ActivityDiagramGraph();
        INode decision = add(graph, new DecisionNode());
        INode bar = add(graph, new SynchronizationBarNode());
        assertEquals(COMMON_LINKS, possible(graph, decision, bar));
        assertFalse(graph.connect(new ActivityTransitionEdge(), decision, new Point2D.Double(), bar, new Point2D.Double()));
        assertEquals(0, graph.getAllEdges().size());
        assertEquals(List.of(ActivityTransitionEdge.class), possible(graph, bar, decision));
        ActivityTransitionEdge oldEdge = new ActivityTransitionEdge();
        oldEdge.setStartNode(decision);
        oldEdge.setEndNode(bar);
        assertTrue(graph.connect(oldEdge, decision, new Point2D.Double(), bar, new Point2D.Double()));
    }

    @Test
    void shouldOnlyOfferATransitionFromAStartNodeToAnActivity()
    {
        IGraph graph = new ActivityDiagramGraph();
        INode start = add(graph, new ScenarioStartNode());
        INode activity = add(graph, new ActivityNode());

        assertEquals(List.of(ActivityTransitionEdge.class), possible(graph, start, activity));
        // Nothing goes into the start node, nor straight from the start node to the end node
        assertEquals(COMMON_LINKS, possible(graph, activity, start));
        assertEquals(COMMON_LINKS, possible(graph, start, add(graph, new ScenarioEndNode())));
    }

    @Test
    void shouldFollowTheDirectionOfTheClassDiagramRules()
    {
        IGraph graph = new ClassDiagramGraph();
        INode aClass = add(graph, new ClassNode());
        INode anotherClass = add(graph, new ClassNode());
        INode anInterface = add(graph, new InterfaceNode());
        INode anotherInterface = add(graph, new InterfaceNode());
        INode aPackage = add(graph, new PackageNode());

        assertEquals(List.of(DependencyEdge.class, InheritanceEdge.class, AssociationEdge.class, AggregationEdge.class,
                CompositionEdge.class), possible(graph, aClass, anotherClass));
        // A class implements an interface: it neither inherits from it nor is made of it
        assertEquals(List.of(DependencyEdge.class, InterfaceInheritanceEdge.class, AssociationEdge.class),
                possible(graph, aClass, anInterface));
        assertEquals(List.of(DependencyEdge.class, InheritanceEdge.class), possible(graph, anInterface, anotherInterface));
        assertEquals(List.of(DependencyEdge.class), possible(graph, anInterface, aClass));
        assertEquals(List.of(DependencyEdge.class), possible(graph, aPackage, aClass));
    }

    @Test
    void shouldOnlyOfferTheGenericEdgeForACommonElement()
    {
        IGraph graph = new ClassDiagramGraph();
        INode aClass = add(graph, new ClassNode());
        INode note = add(graph, new NoteNode());
        INode container = add(graph, new ContainerNode());

        assertEquals(COMMON_LINKS, possible(graph, note, aClass));
        assertEquals(COMMON_LINKS, possible(graph, aClass, note));
        assertEquals(COMMON_LINKS, possible(graph, container, aClass));
    }

    @Test
    void shouldOnlyOfferACallTowardsALifeline()
    {
        IGraph graph = new SequenceDiagramGraph();
        LifelineNode caller = (LifelineNode) add(graph, new LifelineNode());
        LifelineNode callee = (LifelineNode) add(graph, new LifelineNode());
        ActivationBarNode callerBar = new ActivationBarNode();
        caller.addChild(callerBar, new Point2D.Double(32, 80));
        ActivationBarNode calleeBar = new ActivationBarNode();
        callee.addChild(calleeBar, new Point2D.Double(32, 100));

        assertEquals(List.of(CallEdge.class), possible(graph, callerBar, callee));
        // No call yet between both bars: a call, either way
        assertEquals(List.of(CallEdge.class), possible(graph, calleeBar, callerBar));
        assertEquals(List.of(CallEdge.class), possible(graph, callerBar, calleeBar));
        // Nothing leaves a lifeline itself
        assertEquals(COMMON_LINKS, possible(graph, caller, callee));
    }

    @Test
    void shouldOnlyOfferAReturnTowardsTheCaller()
    {
        IGraph graph = new SequenceDiagramGraph();
        LifelineNode caller = (LifelineNode) add(graph, new LifelineNode());
        LifelineNode callee = (LifelineNode) add(graph, new LifelineNode());
        ActivationBarNode callerBar = new ActivationBarNode();
        caller.addChild(callerBar, new Point2D.Double(32, 80));
        ActivationBarNode calleeBar = new ActivationBarNode();
        callee.addChild(calleeBar, new Point2D.Double(32, 100));
        assertTrue(graph.connect(new CallEdge(), callerBar, new Point2D.Double(0, 0), calleeBar, new Point2D.Double(0, 0)));

        assertEquals(List.of(ReturnEdge.class), possible(graph, calleeBar, callerBar));
        // A bar is only called once
        assertEquals(COMMON_LINKS, possible(graph, callerBar, calleeBar));

        // Even drawn with its tool, a return only goes back to the caller
        ActivationBarNode otherBar = new ActivationBarNode();
        assertTrue(callerBar.getParent().addChild(otherBar, new Point2D.Double(0, 300)));
        assertFalse(graph.connect(new ReturnEdge(), calleeBar, new Point2D.Double(0, 0), otherBar, new Point2D.Double(0, 0)));
        assertFalse(graph.connect(new ReturnEdge(), callerBar, new Point2D.Double(0, 0), calleeBar, new Point2D.Double(0, 0)));
        assertTrue(graph.connect(new ReturnEdge(), calleeBar, new Point2D.Double(0, 0), callerBar, new Point2D.Double(0, 0)));
    }

    @Test
    void shouldTellTheUseCaseEdgesApart()
    {
        IGraph graph = new UseCaseDiagramGraph();
        INode actor = add(graph, new ActorNode());
        INode anotherActor = add(graph, new ActorNode());
        INode useCase = add(graph, new UseCaseNode());
        INode anotherUseCase = add(graph, new UseCaseNode());
        List<IEdge> prototypes = graph.getEdgePrototypes();
        IEdge communication = prototypes.get(0);
        IEdge extend = prototypes.get(1);
        IEdge include = prototypes.get(2);
        IEdge generalization = prototypes.get(3);

        assertEquals(List.of(communication), graph.getEdgePrototypes(actor, useCase));
        assertEquals(List.of(communication), graph.getEdgePrototypes(useCase, actor));
        assertEquals(List.of(extend, include, generalization), graph.getEdgePrototypes(useCase, anotherUseCase));
        assertEquals(List.of(generalization), graph.getEdgePrototypes(actor, anotherActor));
    }

    @Test
    void shouldNotGoStraightFromTheInitialStateToTheFinalOne()
    {
        IGraph graph = new StateDiagramGraph();
        INode initial = add(graph, new CircularInitialStateNode());
        INode state = add(graph, new StateNode());
        INode end = add(graph, new CircularFinalStateNode());

        assertEquals(List.of(StateTransitionEdge.class), possible(graph, initial, state));
        assertEquals(List.of(StateTransitionEdge.class), possible(graph, state, end));
        assertEquals(COMMON_LINKS, possible(graph, initial, end));
        assertEquals(COMMON_LINKS, possible(graph, end, state));
    }

    private INode add(IGraph graph, INode node)
    {
        graph.addNode(node, new Point2D.Double(300 * graph.getAllNodes().size(), 0));
        return node;
    }


    /** The links of the common tools: generic edges, in several looks */
    private static final List<Class<?>> COMMON_LINKS = Collections.nCopies(GenericEdge.createToolPrototypes().size(),
            GenericEdge.class);
    private List<Class<?>> possible(IGraph graph, INode start, INode end)
    {
        List<Class<?>> classes = new ArrayList<Class<?>>();
        for (IEdge anEdge : graph.getEdgePrototypes(start, end))
        {
            classes.add(anEdge.getClass());
        }
        return classes;
    }

}

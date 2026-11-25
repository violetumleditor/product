package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.NoteNode;
import com.horstmann.violet.product.diagram.sequence.ActivationBarNode;
import com.horstmann.violet.product.diagram.sequence.CallEdge;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;

/**
 * Calls make neither loops nor bars of a lifeline running side by side
 */
class CallRulesTest
{
    private SequenceDiagramGraph graph;

    private LifelineNode lifelineA;
    private LifelineNode lifelineB;
    private LifelineNode lifelineC;

    private ActivationBarNode a;
    private ActivationBarNode b;
    private ActivationBarNode c;

    @BeforeEach
    void createDiagram()
    {
        this.graph = new SequenceDiagramGraph();
        this.lifelineA = addLifeline(100);
        this.lifelineB = addLifeline(300);
        this.lifelineC = addLifeline(500);
        this.a = addBar(this.lifelineA, 100);
        this.b = addBar(this.lifelineB, 120);
        this.c = addBar(this.lifelineC, 140);
    }

    /** A loop would never end here */
    @AfterEach
    void shouldDraw()
    {
        this.graph.draw(new BufferedImage(800, 800, BufferedImage.TYPE_INT_ARGB).createGraphics());
    }

    @Test
    void shouldRefuseLoops()
    {
        assertTrue(call(this.a, this.b));
        assertFalse(call(this.b, this.a));
        assertTrue(call(this.b, this.c));
        assertFalse(call(this.c, this.a));
        assertFalse(call(this.c, this.b));
        assertFalse(call(this.a, this.a));
        assertTrue(this.graph.getEdgePrototypes(this.c, this.a).isEmpty());
    }

    @Test
    void shouldRefuseLoopsThroughNestedBars()
    {
        ActivationBarNode nested = addBar(this.a, 20);
        assertFalse(call(nested, this.a));
        assertTrue(call(nested, this.b));
        assertFalse(call(this.b, this.a));
        assertFalse(call(this.b, nested));
    }

    @Test
    void shouldRefuseASecondCaller()
    {
        assertTrue(call(this.a, this.b));
        assertFalse(call(this.a, this.b));
        assertFalse(call(this.c, this.b));
    }

    @Test
    void shouldOnlyCallBackANestedBar()
    {
        ActivationBarNode sibling = addBar(this.lifelineA, 300);
        ActivationBarNode nested = addBar(this.a, 60);
        assertFalse(call(this.a, sibling));
        assertTrue(call(this.a, this.b));
        assertFalse(call(this.b, sibling));
        assertTrue(call(this.b, nested));
    }

    @Test
    void shouldRefuseACalleeThatRunsDuringABarOfTheCallersLifeline()
    {
        ActivationBarNode sibling = addBar(this.lifelineA, 300);
        assertTrue(call(this.b, sibling));
        assertFalse(call(this.a, this.b));
    }

    @Test
    void shouldNestTheNewBarOfACallBackToALifeline()
    {
        assertTrue(call(this.a, this.b));
        shouldDraw();
        CallEdge edge = new CallEdge();
        assertTrue(this.graph.connect(edge, this.b, new Point2D.Double(), this.lifelineA, new Point2D.Double(40, 150)));
        assertSame(this.a, edge.getEndNode().getParent());
        shouldDraw();
        assertEquals(150, edge.getEndNode().getLocationOnGraph().getY(), 0.001);

        CallEdge otherEdge = new CallEdge();
        assertTrue(this.graph.connect(otherEdge, this.b, new Point2D.Double(), this.lifelineC, new Point2D.Double(40, 250)));
        assertSame(this.lifelineC, otherEdge.getEndNode().getParent());
    }

    @Test
    void shouldCreateALifelineOnceAndNotFromItself()
    {
        assertTrue(create(this.a, this.lifelineB));
        assertFalse(create(this.c, this.lifelineB));
        assertFalse(create(this.b, this.lifelineA));
        assertTrue(call(this.b, this.c));
        assertFalse(create(this.c, this.lifelineA));
    }

    @Test
    void shouldRefuseACallToABarOfTheCreatorOfTheLifeline()
    {
        assertTrue(create(this.a, this.lifelineB));
        assertFalse(call(this.b, this.a));
    }

    @Test
    void shouldOnlyLinkLifelinesAndBarsWithMessages()
    {
        INode[] nodes = { this.lifelineA, this.lifelineB, this.a, this.b };
        for (INode start : nodes)
        {
            for (INode end : nodes)
            {
                assertFalse(link(start, end));
                assertTrue(this.graph.getEdgePrototypes(start, end).stream().noneMatch(e -> e instanceof GenericEdge));
            }
        }
        NoteNode note = new NoteNode();
        assertTrue(this.graph.addNode(note, new Point2D.Double(700, 0)));
        assertTrue(link(note, this.a));
        assertTrue(link(this.lifelineA, note));
    }

    private boolean link(INode start, INode end)
    {
        return this.graph.connect(new GenericEdge(), start, new Point2D.Double(), end, new Point2D.Double());
    }

    private boolean call(INode start, INode end)
    {
        return this.graph.connect(new CallEdge(), start, new Point2D.Double(), end, new Point2D.Double());
    }

    /** A call to the head of a lifeline */
    private boolean create(INode start, LifelineNode lifeline)
    {
        return this.graph.connect(new CallEdge(), start, new Point2D.Double(), lifeline, new Point2D.Double(10, 10));
    }

    private LifelineNode addLifeline(double x)
    {
        LifelineNode lifeline = new LifelineNode();
        assertTrue(this.graph.addNode(lifeline, new Point2D.Double(x, 0)));
        return lifeline;
    }

    private ActivationBarNode addBar(INode parent, double y)
    {
        ActivationBarNode bar = new ActivationBarNode();
        assertTrue(parent.addChild(bar, new Point2D.Double(0, y)));
        return bar;
    }
}

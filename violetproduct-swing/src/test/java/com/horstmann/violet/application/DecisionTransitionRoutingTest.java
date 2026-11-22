package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.activity.ActivityDiagramGraph;
import com.horstmann.violet.product.diagram.activity.ActivityNode;
import com.horstmann.violet.product.diagram.activity.ActivityTransitionEdge;
import com.horstmann.violet.product.diagram.activity.DecisionNode;

/**
 * A transition leaving a decision chooses the vertex it leaves from as in the user's screenshot: a side vertex toward an
 * activity clearly on that side, the bottom vertex toward an activity below the diamond or when an incoming transition already
 * takes the side vertex.
 */
class DecisionTransitionRoutingTest
{

    private ActivityDiagramGraph graph;

    private DecisionNode decision;

    @BeforeEach
    void createDiagram()
    {
        this.graph = new ActivityDiagramGraph();
        this.decision = new DecisionNode();
        assertTrue(this.graph.addNode(this.decision, new Point2D.Double(400, 50)));
    }

    @Test
    void shouldLeaveFromTheBottomVertexTowardAnActivityBelowTheDiamond()
    {
        Rectangle2D d = this.decision.getBounds();
        ActivityNode below = addActivity(d.getCenterX() - 40, d.getMaxY() + 100);
        List<Point2D> points = connect(this.decision, below).getPoints();
        Point2D first = points.get(0);
        assertEquals(d.getCenterX(), first.getX(), 0.5);
        assertEquals(d.getMaxY(), first.getY(), 0.5);
        for (Point2D p : points)
        {
            assertTrue(p.getY() >= d.getMaxY() - 0.5, "no segment goes back over the diamond: " + points);
        }
    }

    @Test
    void shouldLeaveFromTheSideVertexTowardAnActivityClearlyOnTheSide()
    {
        // Reached from above, as the decisions of a diagram read from top to bottom
        Rectangle2D d = this.decision.getBounds();
        ActivityNode previous = addActivity(d.getCenterX() - 50, d.getMinY() - 200);
        connect(previous, this.decision);
        ActivityNode right = addActivity(d.getMaxX() + 200, d.getMaxY() + 40);
        Point2D first = connect(this.decision, right).getPoints().get(0);
        assertEquals(d.getMaxX(), first.getX(), 0.5);
        assertEquals(d.getCenterY(), first.getY(), 0.5);
        ActivityNode left = addActivity(d.getMinX() - 300, d.getMaxY() + 40);
        first = connect(this.decision, left).getPoints().get(0);
        assertEquals(d.getMinX(), first.getX(), 0.5);
        assertEquals(d.getCenterY(), first.getY(), 0.5);
    }

    @Test
    void shouldLeaveFromTheBottomVertexWhenAnIncomingTransitionTakesTheSideVertex()
    {
        Rectangle2D d = this.decision.getBounds();
        ActivityNode previous = addActivity(d.getMinX() - 300, d.getCenterY() - 30);
        connect(previous, this.decision);
        ActivityNode leftBelow = addActivity(d.getMinX() - 200, d.getMaxY() + 100);
        Point2D first = connect(this.decision, leftBelow).getPoints().get(0);
        assertEquals(d.getCenterX(), first.getX(), 0.5);
        assertEquals(d.getMaxY(), first.getY(), 0.5);
    }

    @Test
    void shouldGiveTwoBranchesOnTheSameSideTheirOwnVertex()
    {
        // The user's second screenshot: fed from the left, two activities below on the right
        Rectangle2D d = this.decision.getBounds();
        ActivityNode previous = addActivity(d.getMinX() - 300, d.getCenterY() - 30);
        connect(previous, this.decision);
        ActivityNode near = addActivity(d.getMaxX() + 40, d.getMaxY() + 100);
        ActivityNode far = addActivity(d.getMaxX() + 230, d.getMaxY() + 100);
        ActivityTransitionEdge toNear = connect(this.decision, near);
        ActivityTransitionEdge toFar = connect(this.decision, far);
        Point2D nearStart = toNear.getPoints().get(0);
        Point2D farStart = toFar.getPoints().get(0);
        assertEquals(d.getCenterX(), nearStart.getX(), 0.5, "the nearest branch leaves from the bottom vertex");
        assertEquals(d.getMaxY(), nearStart.getY(), 0.5);
        assertEquals(d.getMaxX(), farStart.getX(), 0.5, "the farthest branch keeps the right vertex, so they don't cross");
        assertEquals(d.getCenterY(), farStart.getY(), 0.5);
    }

    @Test
    void shouldSpreadThreeBranchesOverThreeVertices()
    {
        Rectangle2D d = this.decision.getBounds();
        ActivityNode previous = addActivity(d.getCenterX() - 50, d.getMinY() - 200);
        connect(previous, this.decision);
        ActivityTransitionEdge toLeft = connect(this.decision, addActivity(d.getMinX() - 250, d.getMaxY() + 100));
        ActivityTransitionEdge toMiddle = connect(this.decision, addActivity(d.getCenterX() - 30, d.getMaxY() + 100));
        ActivityTransitionEdge toRight = connect(this.decision, addActivity(d.getMaxX() + 150, d.getMaxY() + 100));
        assertEquals(d.getMinX(), toLeft.getPoints().get(0).getX(), 0.5);
        assertEquals(d.getMaxY(), toMiddle.getPoints().get(0).getY(), 0.5);
        assertEquals(d.getMaxX(), toRight.getPoints().get(0).getX(), 0.5);
    }

    @Test
    void shouldPreferTwoSegmentPathsThatGoAroundTheOtherNodes()
    {
        // The user's third screenshot: fed from the left, three activities in a column on the right, above, below, further below
        Rectangle2D d = this.decision.getBounds();
        ActivityNode previous = addActivity(d.getMinX() - 300, d.getCenterY() - 30);
        connect(previous, this.decision);
        ActivityNode above = addActivity(d.getMaxX() + 225, d.getMinY() - 160);
        ActivityNode below = addActivity(d.getMaxX() + 225, d.getMaxY() + 100);
        ActivityNode furtherBelow = addActivity(d.getMaxX() + 225, d.getMaxY() + 290);
        ActivityTransitionEdge toAbove = connect(this.decision, above);
        ActivityTransitionEdge toBelow = connect(this.decision, below);
        ActivityTransitionEdge toFurtherBelow = connect(this.decision, furtherBelow);
        assertEquals(d.getMinY(), toAbove.getPoints().get(0).getY(), 0.5, "up from the top vertex");
        assertEquals(d.getMaxX(), toBelow.getPoints().get(0).getX(), 0.5, "from the right vertex");
        assertEquals(d.getMaxY(), toFurtherBelow.getPoints().get(0).getY(), 0.5, "down from the bottom vertex");
        Rectangle2D belowBounds = below.getBounds();
        for (ActivityTransitionEdge anEdge : List.of(toAbove, toBelow, toFurtherBelow))
        {
            List<Point2D> points = anEdge.getPoints();
            assertEquals(3, points.size(), "HV or VH: " + points);
            if (anEdge != toBelow)
            {
                for (int i = 1; i < points.size(); i++)
                {
                    assertTrue(!belowBounds.intersectsLine(new Line2D.Double(points.get(i - 1), points.get(i))),
                            "doesn't go through the activity in the middle: " + points);
                }
            }
        }
    }

    @Test
    void shouldStopWhenTwoDecisionsLeadToEachOther()
    {
        DecisionNode other = new DecisionNode();
        assertTrue(this.graph.addNode(other, new Point2D.Double(100, 300)));
        ActivityTransitionEdge there = connect(this.decision, other);
        ActivityTransitionEdge back = connect(other, this.decision);
        assertTrue(there.getPoints().size() >= 2);
        assertTrue(back.getPoints().size() >= 2);
    }

    @Test
    void shouldKeepSingleBendBranchesWhenAnActivityIsWidenedByItsText()
    {
        // The user's screenshot: a long text widened the right activity, and both branches switched from HV to HVH
        Rectangle2D d = this.decision.getBounds();
        ActivityNode previous = addActivity(d.getCenterX() - 50, d.getMinY() - 160);
        connect(previous, this.decision);
        ActivityNode left = addActivity(d.getMinX() - 140, d.getMaxY() + 125);
        ActivityNode wide = addActivity(d.getMaxX() + 75, d.getMaxY() + 115);
        wide.getName().setText("gfhfghfghfghfghfghfghfghfghfghfghfghfghfghfgh");
        assertTrue(wide.getBounds().getWidth() > 250, "widened by its text: " + wide.getBounds());
        ActivityTransitionEdge toLeft = connect(this.decision, left);
        ActivityTransitionEdge toWide = connect(this.decision, wide);
        assertEquals(3, toLeft.getPoints().size(), "HV: " + toLeft.getPoints());
        assertEquals(3, toWide.getPoints().size(), "HV: " + toWide.getPoints());
        assertEquals(d.getMinX(), toLeft.getPoints().get(0).getX(), 0.5, "from the left vertex");
        assertEquals(d.getMaxX(), toWide.getPoints().get(0).getX(), 0.5, "from the right vertex");
    }

    @Test
    void shouldJoinTwoOffsetActivitiesWithASingleBend()
    {
        ActivityNode first = addActivity(100, 300);
        ActivityNode second = addActivity(400, 500);
        List<Point2D> points = connect(first, second).getPoints();
        assertEquals(3, points.size(), "HV or VH: " + points);
    }

    @Test
    void shouldBringTwoMergingBranchesByDistinctSides()
    {
        ActivityNode left = addActivity(100, 300);
        ActivityNode right = addActivity(500, 300);
        ActivityNode merge = addActivity(300, 500);
        ActivityTransitionEdge fromLeft = connect(left, merge);
        ActivityTransitionEdge fromRight = connect(right, merge);
        assertNotEquals(fromLeft.getDirection(merge), fromRight.getDirection(merge), "not along one line to the same side");
    }

    private ActivityNode addActivity(double x, double y)
    {
        ActivityNode activity = new ActivityNode();
        assertTrue(this.graph.addNode(activity, new Point2D.Double(x, y)));
        return activity;
    }

    private ActivityTransitionEdge connect(INode start, INode end)
    {
        ActivityTransitionEdge edge = new ActivityTransitionEdge();
        Rectangle2D s = start.getBounds();
        Rectangle2D e = end.getBounds();
        assertTrue(this.graph.connect(edge, start, new Point2D.Double(s.getCenterX(), s.getCenterY()), end,
                new Point2D.Double(e.getCenterX(), e.getCenterY())));
        return edge;
    }
}

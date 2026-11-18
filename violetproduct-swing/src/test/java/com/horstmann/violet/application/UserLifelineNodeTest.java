package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.geom.Point2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.sequence.ActivationBarNode;
import com.horstmann.violet.product.diagram.sequence.CallEdge;
import com.horstmann.violet.product.diagram.sequence.LifelineNode;
import com.horstmann.violet.product.diagram.sequence.SequenceDiagramGraph;
import com.horstmann.violet.product.diagram.sequence.UserLifelineNode;

/**
 * A user lifeline is an object lifeline with a stick man as its head: it must behave like one.
 */
class UserLifelineNodeTest
{
    @Test
    void isOfferedByTheSequenceDiagram()
    {
        assertTrue(new SequenceDiagramGraph().getNodePrototypes().stream().anyMatch(UserLifelineNode.class::isInstance));
    }

    @Test
    void headIsTallerThanTheObjectBoxToHoldTheStickMan()
    {
        IGraph graph = new SequenceDiagramGraph();
        UserLifelineNode user = new UserLifelineNode();
        user.getName().setText("customer");
        graph.addNode(user, new Point2D.Double(0, 0));

        assertFalse(user.getName().isUnderlined());
        assertTrue(user.getTopRectangle().getHeight() > user.getName().getBounds().getHeight() + 40);
    }

    @Test
    void callBelowTheHeadCreatesAnActivationBarOnIt()
    {
        IGraph graph = new SequenceDiagramGraph();
        LifelineNode caller = new LifelineNode();
        graph.addNode(caller, new Point2D.Double(0, 0));
        ActivationBarNode callerBar = new ActivationBarNode();
        caller.addChild(callerBar, new Point2D.Double(32, 120));
        UserLifelineNode user = new UserLifelineNode();
        graph.addNode(user, new Point2D.Double(300, 0));

        CallEdge call = new CallEdge();
        Point2D below = new Point2D.Double(user.getBounds().getCenterX(), 200);
        assertTrue(graph.connect(call, callerBar, new Point2D.Double(), user, below));

        ActivationBarNode userBar = assertInstanceOf(ActivationBarNode.class, call.getEndNode());
        assertSame(user, userBar.getParent());
        assertSame(user, userBar.getImplicitParameter());
    }

    @Test
    void staysAUserLifelineAfterReload() throws IOException
    {
        IGraph graph = new SequenceDiagramGraph();
        UserLifelineNode user = new UserLifelineNode();
        user.getName().setText("customer");
        graph.addNode(user, new Point2D.Double(20, 0));
        user.addChild(new ActivationBarNode(), new Point2D.Double(32, 120));

        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));

        INode node = reloaded.getAllNodes().stream().filter(n -> n.getParent() == null).findFirst().orElseThrow();
        UserLifelineNode reloadedUser = assertInstanceOf(UserLifelineNode.class, node);
        assertEquals(user.getName().getText(), reloadedUser.getName().getText());
        assertFalse(reloadedUser.getName().isUnderlined());
        assertEquals(1, reloadedUser.getChildren().size());
    }
}

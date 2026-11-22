package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.horstmann.violet.framework.file.persistence.IFilePersistenceService;
import com.horstmann.violet.framework.file.persistence.XMLPersistenceService;
import com.horstmann.violet.framework.plugin.IDiagramPlugin;
import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.Direction;
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ToolCategory;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.bpmn.ProcessDiagramGraph;
import com.horstmann.violet.product.diagram.bpmn.ProcessEndEventNode;
import com.horstmann.violet.product.diagram.bpmn.ProcessGatewayNode;
import com.horstmann.violet.product.diagram.bpmn.ProcessMessageFlowEdge;
import com.horstmann.violet.product.diagram.bpmn.ProcessPoolNode;
import com.horstmann.violet.product.diagram.bpmn.ProcessSequenceFlowEdge;
import com.horstmann.violet.product.diagram.bpmn.ProcessStartEventNode;
import com.horstmann.violet.product.diagram.bpmn.ProcessTaskNode;
import com.horstmann.violet.product.diagram.bpmn.editor.TaskTypeEditor;
import com.horstmann.violet.product.diagram.bpmn.type.EventResultType;
import com.horstmann.violet.product.diagram.bpmn.type.EventTriggerType;
import com.horstmann.violet.product.diagram.bpmn.type.FlowType;
import com.horstmann.violet.product.diagram.bpmn.type.GatewayType;
import com.horstmann.violet.product.diagram.bpmn.type.TaskType;
import com.horstmann.violet.product.diagram.common.GenericEdge;

class ProcessDiagramTest
{

    @Test
    void shouldBeDiscoveredAsADiagramPlugin()
    {
        boolean found = false;
        for (IDiagramPlugin aPlugin : ServiceLoader.load(IDiagramPlugin.class))
        {
            found |= ProcessDiagramGraph.class.equals(aPlugin.getGraphClass());
        }
        assertTrue(found);
    }

    @Test
    void shouldSaveAndReloadItsElements() throws IOException
    {
        IGraph graph = new ProcessDiagramGraph();
        ProcessPoolNode pool = new ProcessPoolNode();
        pool.setPreferredSize(new Rectangle2D.Double(0, 0, 700, 300));
        pool.getName().setText("Shop");
        pool.getLanes().setText("Seller\nWarehouse");
        graph.addNode(pool, new Point2D.Double(0, 0));
        ProcessStartEventNode start = new ProcessStartEventNode();
        start.setTrigger(EventTriggerType.MESSAGE);
        start.getName().setText("Order received");
        graph.addNode(start, new Point2D.Double(60, 40));
        ProcessTaskNode task = new ProcessTaskNode();
        task.setTaskType(TaskType.USER);
        task.getName().setText("Check stock");
        graph.addNode(task, new Point2D.Double(160, 30));
        ProcessGatewayNode gateway = new ProcessGatewayNode();
        gateway.setGatewayType(GatewayType.PARALLEL);
        graph.addNode(gateway, new Point2D.Double(320, 40));
        ProcessEndEventNode end = new ProcessEndEventNode();
        end.setResult(EventResultType.ERROR);
        graph.addNode(end, new Point2D.Double(500, 40));
        ProcessSequenceFlowEdge flow = new ProcessSequenceFlowEdge();
        flow.setFlowType(FlowType.TIMER);
        flow.setMiddleLabel("ok");
        assertTrue(graph.connect(flow, task, new Point2D.Double(), gateway, new Point2D.Double()));

        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));

        ProcessPoolNode reloadedPool = find(reloaded, ProcessPoolNode.class);
        assertEquals(new Rectangle2D.Double(0, 0, 700, 300), reloadedPool.getBounds());
        assertEquals(pool.getName().getText(), reloadedPool.getName().getText());
        assertEquals(pool.getLanes().getText(), reloadedPool.getLanes().getText());
        assertSame(EventTriggerType.MESSAGE, find(reloaded, ProcessStartEventNode.class).getTrigger());
        assertSame(EventResultType.ERROR, find(reloaded, ProcessEndEventNode.class).getResult());
        assertSame(TaskType.USER, find(reloaded, ProcessTaskNode.class).getTaskType());
        assertSame(GatewayType.PARALLEL, find(reloaded, ProcessGatewayNode.class).getGatewayType());
        ProcessSequenceFlowEdge reloadedFlow = (ProcessSequenceFlowEdge) reloaded.getAllEdges().iterator().next();
        assertSame(FlowType.TIMER, reloadedFlow.getFlowType());
        assertEquals("ok", reloadedFlow.getMiddleLabel());
        assertInstanceOf(ProcessTaskNode.class, reloadedFlow.getStartNode());
    }

    @Test
    void shouldResizeLanesByTheirSeparatorAndKeepTheirHeights() throws IOException
    {
        IGraph graph = new ProcessDiagramGraph();
        ProcessPoolNode pool = new ProcessPoolNode();
        pool.setPreferredSize(new Rectangle2D.Double(0, 0, 600, 300));
        pool.getLanes().setText("A\nB\nC");
        graph.addNode(pool, new Point2D.Double(0, 0));
        assertSeparatorsAt(pool, 100, 200);

        pool.moveSeparator(0, new Point2D.Double(300, 160));
        assertSeparatorsAt(pool, 160, 200);
        // The lane below keeps a minimum height
        pool.moveSeparator(1, new Point2D.Double(300, 500));
        assertSeparatorsAt(pool, 160, 270);

        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);
        ProcessPoolNode reloadedPool = find(persistenceService.read(new ByteArrayInputStream(out.toByteArray())),
                ProcessPoolNode.class);
        assertSeparatorsAt(reloadedPool, 160, 270);

        // Resizing the pool keeps the proportions, a new lane gets the mean height
        reloadedPool.setPreferredSize(new Rectangle2D.Double(0, 0, 600, 600));
        assertSeparatorsAt(reloadedPool, 320, 540);
        reloadedPool.getLanes().setText("A\nB\nC\nD");
        assertSeparatorsAt(reloadedPool, 240, 405, 450);
    }

    private void assertSeparatorsAt(ProcessPoolNode pool, double... expectedYs)
    {
        List<Line2D> separators = pool.getSeparators();
        assertEquals(expectedYs.length, separators.size());
        for (int i = 0; i < expectedYs.length; i++)
        {
            assertEquals(expectedYs[i], separators.get(i).getY1(), 0.001);
        }
    }

    @Test
    void shouldOfferASequenceFlowInsideAPoolAndAMessageFlowBetweenPools()
    {
        IGraph graph = new ProcessDiagramGraph();
        ProcessPoolNode customer = new ProcessPoolNode();
        customer.setPreferredSize(new Rectangle2D.Double(0, 0, 600, 200));
        graph.addNode(customer, new Point2D.Double(0, 0));
        ProcessPoolNode shop = new ProcessPoolNode();
        shop.setPreferredSize(new Rectangle2D.Double(0, 0, 600, 200));
        graph.addNode(shop, new Point2D.Double(0, 300));
        ProcessTaskNode order = addTask(graph, 100, 50);
        ProcessTaskNode pay = addTask(graph, 300, 50);
        ProcessTaskNode ship = addTask(graph, 100, 350);

        assertEdgeTypes(graph.getEdgePrototypes(order, pay), ProcessSequenceFlowEdge.class);
        assertEdgeTypes(graph.getEdgePrototypes(order, ship), ProcessMessageFlowEdge.class);

        ProcessStartEventNode start = new ProcessStartEventNode();
        graph.addNode(start, new Point2D.Double(40, 60));
        assertEdgeTypes(graph.getEdgePrototypes(start, order), ProcessSequenceFlowEdge.class);
        // A start event receives messages but no sequence flow
        assertEdgeTypes(graph.getEdgePrototypes(ship, start), ProcessMessageFlowEdge.class);
        assertFalse(graph.connect(new ProcessSequenceFlowEdge(), order, new Point2D.Double(), start, new Point2D.Double()));

        ProcessEndEventNode end = new ProcessEndEventNode();
        graph.addNode(end, new Point2D.Double(500, 60));
        assertFalse(graph.connect(new ProcessSequenceFlowEdge(), end, new Point2D.Double(), pay, new Point2D.Double()));
        assertFalse(graph.connect(new ProcessMessageFlowEdge(), ship, new Point2D.Double(), end, new Point2D.Double()));

        // Pools are only linked by generic edges, as grouping areas
        assertEdgeTypes(graph.getEdgePrototypes(customer, shop), GenericEdge.class, GenericEdge.class, GenericEdge.class,
                GenericEdge.class, GenericEdge.class);
    }

    @Test
    void shouldGroupAllItsOwnToolsInCategories()
    {
        IGraph graph = new ProcessDiagramGraph();
        Set<Object> categorized = new HashSet<Object>();
        for (ToolCategory aCategory : graph.getToolCategories())
        {
            categorized.addAll(aCategory.prototypes());
        }
        for (INode aNode : graph.getNodePrototypes())
        {
            if (!AbstractGraph.COMMON_NODE_CLASSES.contains(aNode.getClass()))
            {
                assertTrue(categorized.contains(aNode), aNode.getToolTip());
            }
            assertFalse(aNode.getToolTip().isEmpty());
        }
        for (IEdge anEdge : graph.getEdgePrototypes())
        {
            if (!(anEdge instanceof GenericEdge))
            {
                assertTrue(categorized.contains(anEdge), anEdge.getToolTip());
            }
        }
    }

    @Test
    void shouldDrawEveryToolWithoutError()
    {
        IGraph graph = new ProcessDiagramGraph();
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        List<INode> nodes = new ArrayList<INode>();
        for (INode aPrototype : graph.getNodePrototypes())
        {
            INode node = aPrototype.clone();
            aPrototype.draw(g2);
            if (node instanceof ProcessPoolNode)
            {
                ((ProcessPoolNode) node).getLanes().setText("A\nB\nC");
            }
            nodes.add(node);
            node.draw(g2);
        }
        for (EventTriggerType aTrigger : EventTriggerType.values())
        {
            ProcessStartEventNode start = new ProcessStartEventNode();
            start.setTrigger(aTrigger);
            start.getName().setText(aTrigger.name());
            graph.addNode(start, new Point2D.Double(40 * aTrigger.ordinal(), 200));
            start.draw(g2);
        }
        for (EventResultType aResult : EventResultType.values())
        {
            ProcessEndEventNode end = new ProcessEndEventNode();
            end.setResult(aResult);
            end.getName().setText(aResult.name());
            graph.addNode(end, new Point2D.Double(40 * aResult.ordinal(), 300));
            end.draw(g2);
        }
        for (TaskType aType : TaskType.values())
        {
            ProcessTaskNode task = new ProcessTaskNode();
            task.setTaskType(aType);
            task.draw(g2);
        }
        ProcessTaskNode first = addTask(graph, 100, 500);
        ProcessTaskNode second = addTask(graph, 400, 500);
        graph.connect(new ProcessSequenceFlowEdge(), first, new Point2D.Double(), second, new Point2D.Double());
        graph.connect(new ProcessMessageFlowEdge(), first, new Point2D.Double(), second, new Point2D.Double());
        ProcessSequenceFlowEdge timerFlow = new ProcessSequenceFlowEdge();
        timerFlow.setFlowType(FlowType.TIMER);
        graph.connect(timerFlow, first, new Point2D.Double(), second, new Point2D.Double());
        graph.draw(g2);
        g2.dispose();
    }

    @Test
    void shouldEditTheTaskTypeWithItsChoiceButtons() throws Exception
    {
        PropertyDescriptor taskType = null;
        for (PropertyDescriptor aDescriptor : Introspector.getBeanInfo(ProcessTaskNode.class).getPropertyDescriptors())
        {
            if ("taskType".equals(aDescriptor.getName()))
            {
                taskType = aDescriptor;
            }
        }
        assertSame(TaskTypeEditor.class, taskType.getPropertyEditorClass());
        TaskTypeEditor editor = new TaskTypeEditor();
        assertEquals(TaskType.values().length, editor.getChoiceCount());
        editor.select(1);
        assertSame(TaskType.USER, editor.getValue());
        assertTrue(editor.getIcon(1).getIconWidth() > 0);
    }

    @Test
    void shouldTakeTheTypedTextAsTheNameOfATask()
    {
        // The first text field declared, as the typing behavior looks for it
        assertSame(MultiLineString.class, findFirstTextFieldType(ProcessTaskNode.class));
    }

    private Class<?> findFirstTextFieldType(Class<?> type)
    {
        for (Class<?> aClass = type; aClass != null; aClass = aClass.getSuperclass())
        {
            for (java.lang.reflect.Field aField : aClass.getDeclaredFields())
            {
                if (MultiLineString.class.equals(aField.getType()))
                {
                    assertEquals("name", aField.getName());
                    return aField.getType();
                }
            }
        }
        return null;
    }

    @Test
    void shouldBendFlowsOnceWhenTheEndLiesDiagonally()
    {
        IGraph graph = new ProcessDiagramGraph();
        ProcessTaskNode receive = addTask(graph, 100, 160);
        ProcessGatewayNode split = new ProcessGatewayNode();
        graph.addNode(split, new Point2D.Double(260, 170));
        ProcessTaskNode accept = addTask(graph, 360, 60);
        ProcessGatewayNode merge = new ProcessGatewayNode();
        graph.addNode(merge, new Point2D.Double(540, 170));
        ProcessTaskNode archive = addTask(graph, 100, 360);

        // A branch leaves the top vertex of the gateway and reaches the left side of the task: VH
        ProcessSequenceFlowEdge branch = connect(graph, split, accept);
        assertEquals(Direction.SOUTH, branch.getDirection(split));
        assertEquals(Direction.EAST, branch.getDirection(accept));
        // It leaves the right side of the task and reaches the top vertex of the merging gateway: HV
        ProcessSequenceFlowEdge merging = connect(graph, accept, merge);
        assertEquals(Direction.WEST, merging.getDirection(accept));
        assertEquals(Direction.SOUTH, merging.getDirection(merge));
        // Side by side: straight, by the facing sides
        ProcessSequenceFlowEdge straight = connect(graph, receive, split);
        assertEquals(Direction.WEST, straight.getDirection(receive));
        assertEquals(Direction.EAST, straight.getDirection(split));
        // One above the other: by the bottom and top sides
        ProcessSequenceFlowEdge down = connect(graph, receive, archive);
        assertEquals(Direction.NORTH, down.getDirection(receive));
        assertEquals(Direction.SOUTH, down.getDirection(archive));
    }

    @Test
    void shouldSendTheBranchesOfASplitAcrossItsIncomingFlow()
    {
        IGraph graph = new ProcessDiagramGraph();
        ProcessTaskNode enter = addTask(graph, 50, 50);
        ProcessGatewayNode split = new ProcessGatewayNode();
        graph.addNode(split, new Point2D.Double(287, 262));
        ProcessTaskNode left = addTask(graph, 0, 410);
        ProcessTaskNode right = addTask(graph, 440, 425);
        ProcessSequenceFlowEdge incoming = connect(graph, enter, split);
        ProcessSequenceFlowEdge toLeft = connect(graph, split, left);
        ProcessSequenceFlowEdge toRight = connect(graph, split, right);
        // Reached by its top vertex, so the branches leave by the left and right ones, then go down: HV, never sharing a vertex
        assertEquals(Direction.SOUTH, incoming.getDirection(split));
        assertEquals(Direction.EAST, toLeft.getDirection(split));
        assertEquals(Direction.SOUTH, toLeft.getDirection(left));
        assertEquals(Direction.WEST, toRight.getDirection(split));
        assertEquals(Direction.SOUTH, toRight.getDirection(right));
    }

    @Test
    void shouldJoinTwoParallelTasksByTwoSidesOfTheNextOne()
    {
        IGraph graph = new ProcessDiagramGraph();
        ProcessTaskNode left = addTask(graph, 0, 410);
        ProcessTaskNode right = addTask(graph, 440, 425);
        ProcessTaskNode next = addTask(graph, 215, 620);
        ProcessSequenceFlowEdge fromLeft = connect(graph, left, next);
        ProcessSequenceFlowEdge fromRight = connect(graph, right, next);
        // Down from each task, then into the left and right sides of the next one (VH both), not along one line to its top
        assertEquals(Direction.NORTH, fromLeft.getDirection(left));
        assertEquals(Direction.EAST, fromLeft.getDirection(next));
        assertEquals(Direction.NORTH, fromRight.getDirection(right));
        assertEquals(Direction.WEST, fromRight.getDirection(next));
    }

    @Test
    void shouldKeepTheSymmetryOfAMergeWhenTheNextTaskWidensWithItsName()
    {
        IGraph graph = new ProcessDiagramGraph();
        ProcessTaskNode upper = addTask(graph, 250, 60);
        ProcessTaskNode lower = addTask(graph, 290, 420);
        ProcessTaskNode next = addTask(graph, 565, 232);
        ProcessSequenceFlowEdge fromUpper = connect(graph, upper, next);
        ProcessSequenceFlowEdge fromLower = connect(graph, lower, next);
        // Into its top and its bottom (HV both), before and after its name widens it to the right
        assertEquals(Direction.SOUTH, fromUpper.getDirection(next));
        assertEquals(Direction.NORTH, fromLower.getDirection(next));
        next.getName().setText("sdfsdfsdfsdfsdfsdfsdfsfsdfsdfs");
        assertTrue(next.getBounds().getWidth() > 200, "the name widens the task");
        assertEquals(Direction.SOUTH, fromUpper.getDirection(next));
        assertEquals(Direction.NORTH, fromLower.getDirection(next));
    }

    private ProcessSequenceFlowEdge connect(IGraph graph, INode start, INode end)
    {
        ProcessSequenceFlowEdge flow = new ProcessSequenceFlowEdge();
        assertTrue(graph.connect(flow, start, new Point2D.Double(), end, new Point2D.Double()));
        return flow;
    }

    private ProcessTaskNode addTask(IGraph graph, double x, double y)
    {
        ProcessTaskNode task = new ProcessTaskNode();
        graph.addNode(task, new Point2D.Double(x, y));
        return task;
    }

    private void assertEdgeTypes(List<IEdge> edges, Class<?>... expectedTypes)
    {
        assertEquals(expectedTypes.length, edges.size(), edges.toString());
        for (int i = 0; i < expectedTypes.length; i++)
        {
            assertInstanceOf(expectedTypes[i], edges.get(i));
        }
    }

    private <T> T find(IGraph graph, Class<T> type)
    {
        for (INode aNode : graph.getAllNodes())
        {
            if (type.isInstance(aNode))
            {
                return type.cast(aNode);
            }
        }
        throw new AssertionError("No " + type.getSimpleName());
    }

}

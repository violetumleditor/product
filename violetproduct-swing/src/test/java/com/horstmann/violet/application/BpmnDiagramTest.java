package com.horstmann.violet.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
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
import com.horstmann.violet.product.diagram.abstracts.IGraph;
import com.horstmann.violet.product.diagram.abstracts.ToolCategory;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.abstracts.property.MultiLineString;
import com.horstmann.violet.product.diagram.bpmn.BpmnAssociationEdge;
import com.horstmann.violet.product.diagram.bpmn.BpmnDataObjectNode;
import com.horstmann.violet.product.diagram.bpmn.BpmnDiagramGraph;
import com.horstmann.violet.product.diagram.bpmn.BpmnEndEventNode;
import com.horstmann.violet.product.diagram.bpmn.BpmnGatewayNode;
import com.horstmann.violet.product.diagram.bpmn.BpmnMessageFlowEdge;
import com.horstmann.violet.product.diagram.bpmn.BpmnPoolNode;
import com.horstmann.violet.product.diagram.bpmn.BpmnSequenceFlowEdge;
import com.horstmann.violet.product.diagram.bpmn.BpmnStartEventNode;
import com.horstmann.violet.product.diagram.bpmn.BpmnTaskNode;
import com.horstmann.violet.product.diagram.bpmn.EventTrigger;
import com.horstmann.violet.product.diagram.bpmn.FlowType;
import com.horstmann.violet.product.diagram.bpmn.GatewayType;
import com.horstmann.violet.product.diagram.bpmn.TaskType;
import com.horstmann.violet.product.diagram.bpmn.TaskTypeEditor;
import com.horstmann.violet.product.diagram.common.GenericEdge;

class BpmnDiagramTest
{

    @Test
    void shouldBeDiscoveredAsADiagramPlugin()
    {
        boolean found = false;
        for (IDiagramPlugin aPlugin : ServiceLoader.load(IDiagramPlugin.class))
        {
            found |= BpmnDiagramGraph.class.equals(aPlugin.getGraphClass());
        }
        assertTrue(found);
    }

    @Test
    void shouldSaveAndReloadItsElements() throws IOException
    {
        IGraph graph = new BpmnDiagramGraph();
        BpmnPoolNode pool = new BpmnPoolNode();
        pool.setPreferredSize(new Rectangle2D.Double(0, 0, 700, 300));
        pool.getName().setText("Shop");
        pool.getLanes().setText("Seller\nWarehouse");
        graph.addNode(pool, new Point2D.Double(0, 0));
        BpmnStartEventNode start = new BpmnStartEventNode();
        start.setTrigger(EventTrigger.MESSAGE);
        start.getName().setText("Order received");
        graph.addNode(start, new Point2D.Double(60, 40));
        BpmnTaskNode task = new BpmnTaskNode();
        task.setTaskType(TaskType.USER);
        task.getName().setText("Check stock");
        graph.addNode(task, new Point2D.Double(160, 30));
        BpmnGatewayNode gateway = new BpmnGatewayNode();
        gateway.setGatewayType(GatewayType.PARALLEL);
        graph.addNode(gateway, new Point2D.Double(320, 40));
        BpmnSequenceFlowEdge flow = new BpmnSequenceFlowEdge();
        flow.setFlowType(FlowType.DEFAULT);
        flow.setMiddleLabel("ok");
        assertTrue(graph.connect(flow, task, new Point2D.Double(), gateway, new Point2D.Double()));

        IFilePersistenceService persistenceService = new XMLPersistenceService();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        persistenceService.write(graph, out);
        IGraph reloaded = persistenceService.read(new ByteArrayInputStream(out.toByteArray()));

        BpmnPoolNode reloadedPool = find(reloaded, BpmnPoolNode.class);
        assertEquals(new Rectangle2D.Double(0, 0, 700, 300), reloadedPool.getBounds());
        assertEquals(pool.getName().getText(), reloadedPool.getName().getText());
        assertEquals(pool.getLanes().getText(), reloadedPool.getLanes().getText());
        assertSame(EventTrigger.MESSAGE, find(reloaded, BpmnStartEventNode.class).getTrigger());
        assertSame(TaskType.USER, find(reloaded, BpmnTaskNode.class).getTaskType());
        assertSame(GatewayType.PARALLEL, find(reloaded, BpmnGatewayNode.class).getGatewayType());
        BpmnSequenceFlowEdge reloadedFlow = (BpmnSequenceFlowEdge) reloaded.getAllEdges().iterator().next();
        assertSame(FlowType.DEFAULT, reloadedFlow.getFlowType());
        assertEquals("ok", reloadedFlow.getMiddleLabel());
        assertInstanceOf(BpmnTaskNode.class, reloadedFlow.getStartNode());
    }

    @Test
    void shouldOfferASequenceFlowInsideAPoolAndAMessageFlowBetweenPools()
    {
        IGraph graph = new BpmnDiagramGraph();
        BpmnPoolNode customer = new BpmnPoolNode();
        customer.setPreferredSize(new Rectangle2D.Double(0, 0, 600, 200));
        graph.addNode(customer, new Point2D.Double(0, 0));
        BpmnPoolNode shop = new BpmnPoolNode();
        shop.setPreferredSize(new Rectangle2D.Double(0, 0, 600, 200));
        graph.addNode(shop, new Point2D.Double(0, 300));
        BpmnTaskNode order = addTask(graph, 100, 50);
        BpmnTaskNode pay = addTask(graph, 300, 50);
        BpmnTaskNode ship = addTask(graph, 100, 350);

        assertEdgeTypes(graph.getEdgePrototypes(order, pay), BpmnSequenceFlowEdge.class);
        assertEdgeTypes(graph.getEdgePrototypes(order, ship), BpmnMessageFlowEdge.class);

        BpmnStartEventNode start = new BpmnStartEventNode();
        graph.addNode(start, new Point2D.Double(40, 60));
        assertEdgeTypes(graph.getEdgePrototypes(start, order), BpmnSequenceFlowEdge.class);
        // A start event receives messages but no sequence flow
        assertEdgeTypes(graph.getEdgePrototypes(ship, start), BpmnMessageFlowEdge.class);
        assertFalse(graph.connect(new BpmnSequenceFlowEdge(), order, new Point2D.Double(), start, new Point2D.Double()));

        BpmnEndEventNode end = new BpmnEndEventNode();
        graph.addNode(end, new Point2D.Double(500, 60));
        assertFalse(graph.connect(new BpmnSequenceFlowEdge(), end, new Point2D.Double(), pay, new Point2D.Double()));
        assertFalse(graph.connect(new BpmnMessageFlowEdge(), ship, new Point2D.Double(), end, new Point2D.Double()));

        BpmnDataObjectNode invoice = new BpmnDataObjectNode();
        graph.addNode(invoice, new Point2D.Double(300, 150));
        assertEdgeTypes(graph.getEdgePrototypes(pay, invoice), BpmnAssociationEdge.class);
        // Pools are only linked by generic edges, as grouping areas
        assertEdgeTypes(graph.getEdgePrototypes(customer, shop), GenericEdge.class, GenericEdge.class, GenericEdge.class,
                GenericEdge.class, GenericEdge.class);
    }

    @Test
    void shouldGroupAllItsOwnToolsInCategories()
    {
        IGraph graph = new BpmnDiagramGraph();
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
        IGraph graph = new BpmnDiagramGraph();
        BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        List<INode> nodes = new ArrayList<INode>();
        for (INode aPrototype : graph.getNodePrototypes())
        {
            INode node = aPrototype.clone();
            aPrototype.draw(g2);
            if (node instanceof BpmnPoolNode)
            {
                ((BpmnPoolNode) node).getLanes().setText("A\nB\nC");
            }
            nodes.add(node);
            node.draw(g2);
        }
        for (EventTrigger aTrigger : EventTrigger.values())
        {
            BpmnEndEventNode end = new BpmnEndEventNode();
            end.setTrigger(aTrigger);
            end.getName().setText(aTrigger.name());
            end.draw(g2);
        }
        for (TaskType aType : TaskType.values())
        {
            BpmnTaskNode task = new BpmnTaskNode();
            task.setTaskType(aType);
            task.draw(g2);
        }
        BpmnSequenceFlowEdge conditional = new BpmnSequenceFlowEdge();
        conditional.setFlowType(FlowType.CONDITIONAL);
        BpmnTaskNode first = addTask(graph, 100, 500);
        BpmnTaskNode second = addTask(graph, 400, 500);
        graph.connect(conditional, first, new Point2D.Double(), second, new Point2D.Double());
        BpmnSequenceFlowEdge defaultFlow = new BpmnSequenceFlowEdge();
        defaultFlow.setFlowType(FlowType.DEFAULT);
        graph.connect(defaultFlow, second, new Point2D.Double(), first, new Point2D.Double());
        graph.connect(new BpmnMessageFlowEdge(), first, new Point2D.Double(), second, new Point2D.Double());
        graph.draw(g2);
        g2.dispose();
    }

    @Test
    void shouldEditTheTaskTypeWithItsChoiceButtons() throws Exception
    {
        PropertyDescriptor taskType = null;
        for (PropertyDescriptor aDescriptor : Introspector.getBeanInfo(BpmnTaskNode.class).getPropertyDescriptors())
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
        assertSame(MultiLineString.class, findFirstTextFieldType(BpmnTaskNode.class));
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

    private BpmnTaskNode addTask(IGraph graph, double x, double y)
    {
        BpmnTaskNode task = new BpmnTaskNode();
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

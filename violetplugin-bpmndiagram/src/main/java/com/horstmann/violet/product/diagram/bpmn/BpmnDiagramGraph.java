package com.horstmann.violet.product.diagram.bpmn;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import com.horstmann.violet.product.diagram.abstracts.AbstractGraph;
import com.horstmann.violet.product.diagram.abstracts.ConnectionRule;
import com.horstmann.violet.product.diagram.abstracts.ToolCategory;
import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;
import com.horstmann.violet.product.diagram.common.ContainerNode;
import com.horstmann.violet.product.diagram.common.FreeTextNode;
import com.horstmann.violet.product.diagram.common.GenericEdge;
import com.horstmann.violet.product.diagram.common.ImageNode;
import com.horstmann.violet.product.diagram.common.NoteNode;

/**
 * A BPMN (Business Process Model and Notation) diagram, kept simple on purpose, as the usual BPMN modelers do: a few tools for
 * the elements used in most processes (events, tasks, gateways, data, pools), the less common variants being a property of
 * these elements (the trigger of an event, the kind of a task or a gateway, the lanes of a pool, the kind of a sequence flow).
 */
public class BpmnDiagramGraph extends AbstractGraph
{

    @Override
    public List<INode> getNodePrototypes()
    {
        return NODE_PROTOTYPES;
    }

    @Override
    public List<IEdge> getEdgePrototypes()
    {
        return EDGE_PROTOTYPES;
    }

    @Override
    public List<ToolCategory> getToolCategories()
    {
        return TOOL_CATEGORIES;
    }

    @Override
    protected List<ConnectionRule> getConnectionRules()
    {
        return CONNECTION_RULES;
    }

    /**
     * Nothing flows into a start event, and no message reaches an end event, whatever the tool
     */
    @Override
    public boolean connect(IEdge e, INode start, Point2D startLocation, INode end, Point2D endLocation)
    {
        if (e instanceof BpmnSequenceFlowEdge && end instanceof BpmnStartEventNode)
        {
            return false;
        }
        if (e instanceof BpmnMessageFlowEdge && end instanceof BpmnEndEventNode)
        {
            return false;
        }
        return super.connect(e, start, startLocation, end, endLocation);
    }

    /**
     * Where both a sequence flow and a message flow fit (two tasks...), BPMN tells which one: a sequence flow inside a pool, a
     * message flow between two pools. So the link is drawn without a choice.
     */
    @Override
    public List<IEdge> getEdgePrototypes(INode start, INode end)
    {
        List<IEdge> possibleEdges = super.getEdgePrototypes(start, end);
        boolean isSequenceFlowPossible = false;
        boolean isMessageFlowPossible = false;
        for (IEdge aPrototype : possibleEdges)
        {
            isSequenceFlowPossible |= aPrototype instanceof BpmnSequenceFlowEdge;
            isMessageFlowPossible |= aPrototype instanceof BpmnMessageFlowEdge;
        }
        if (!isSequenceFlowPossible || !isMessageFlowPossible)
        {
            return possibleEdges;
        }
        boolean isSamePool = findPool(start) == findPool(end);
        List<IEdge> result = new ArrayList<IEdge>();
        for (IEdge aPrototype : possibleEdges)
        {
            if ((aPrototype instanceof BpmnMessageFlowEdge && isSamePool)
                    || (aPrototype instanceof BpmnSequenceFlowEdge && !isSamePool))
            {
                continue;
            }
            result.add(aPrototype);
        }
        return result;
    }

    /**
     * @return the smallest pool holding the center of the node, or null if the node lies in none
     */
    private BpmnPoolNode findPool(INode node)
    {
        Point2D location = node.getLocationOnGraph();
        Rectangle2D bounds = node.getBounds();
        Point2D center = new Point2D.Double(location.getX() + bounds.getWidth() / 2, location.getY() + bounds.getHeight() / 2);
        BpmnPoolNode result = null;
        for (INode aNode : getAllNodes())
        {
            if (!(aNode instanceof BpmnPoolNode) || aNode == node)
            {
                continue;
            }
            BpmnPoolNode aPool = (BpmnPoolNode) aNode;
            if (aPool.getBounds().contains(center) && (result == null || aPool.getArea() < result.getArea()))
            {
                result = aPool;
            }
        }
        return result;
    }

    private static final List<INode> NODE_PROTOTYPES = new ArrayList<INode>();

    private static final List<IEdge> EDGE_PROTOTYPES = new ArrayList<IEdge>();

    /**
     * Start node type, end node type, edge types that can go from the first to the second, as in BPMN: sequence flows join the
     * events, activities and gateways of a pool (nothing flows into a start event nor out of an end event); message flows join
     * activities and events (no message leaves a start event nor reaches an end event); associations link data and annotations.
     */
    private static final List<ConnectionRule> CONNECTION_RULES = createConnectionRules();

    private static final List<ToolCategory> TOOL_CATEGORIES = new ArrayList<ToolCategory>();

    private static List<ConnectionRule> createConnectionRules()
    {
        List<Class<? extends INode>> flowNodes = List.of(BpmnStartEventNode.class, BpmnIntermediateEventNode.class,
                BpmnEndEventNode.class, BpmnActivityNode.class, BpmnGatewayNode.class);
        List<Class<? extends INode>> dataNodes = List.of(BpmnDataObjectNode.class, BpmnDataStoreNode.class);
        List<Class<? extends INode>> annotatedNodes = new ArrayList<Class<? extends INode>>(flowNodes);
        annotatedNodes.addAll(dataNodes);
        List<Class<? extends INode>> dataAndAnnotations = new ArrayList<Class<? extends INode>>(dataNodes);
        dataAndAnnotations.add(BpmnTextAnnotationNode.class);

        Map<List<Class<? extends INode>>, List<Class<? extends IEdge>>> edgeTypesByPair = new LinkedHashMap<List<Class<? extends INode>>, List<Class<? extends IEdge>>>();
        addRules(edgeTypesByPair, List.of(BpmnStartEventNode.class, BpmnIntermediateEventNode.class, BpmnActivityNode.class,
                BpmnGatewayNode.class), List.of(BpmnIntermediateEventNode.class, BpmnEndEventNode.class, BpmnActivityNode.class,
                BpmnGatewayNode.class), BpmnSequenceFlowEdge.class);
        addRules(edgeTypesByPair, List.of(BpmnActivityNode.class, BpmnIntermediateEventNode.class, BpmnEndEventNode.class),
                List.of(BpmnActivityNode.class, BpmnIntermediateEventNode.class, BpmnStartEventNode.class),
                BpmnMessageFlowEdge.class);
        addRules(edgeTypesByPair, flowNodes, dataAndAnnotations, BpmnAssociationEdge.class);
        addRules(edgeTypesByPair, dataNodes, List.of(BpmnActivityNode.class, BpmnIntermediateEventNode.class,
                BpmnEndEventNode.class), BpmnAssociationEdge.class);
        addRules(edgeTypesByPair, List.of(BpmnTextAnnotationNode.class), annotatedNodes, BpmnAssociationEdge.class);

        List<ConnectionRule> rules = new ArrayList<ConnectionRule>();
        for (Map.Entry<List<Class<? extends INode>>, List<Class<? extends IEdge>>> anEntry : edgeTypesByPair.entrySet())
        {
            rules.add(new ConnectionRule(anEntry.getKey().get(0), anEntry.getKey().get(1), List.copyOf(anEntry.getValue()),
                    List.of()));
        }
        return List.copyOf(rules);
    }

    /**
     * Lets the edge type go from each start node type to each end node type, one rule per pair of node types
     */
    private static void addRules(Map<List<Class<? extends INode>>, List<Class<? extends IEdge>>> edgeTypesByPair,
            List<Class<? extends INode>> startNodeTypes, List<Class<? extends INode>> endNodeTypes,
            Class<? extends IEdge> edgeType)
    {
        for (Class<? extends INode> aStartNodeType : startNodeTypes)
        {
            for (Class<? extends INode> anEndNodeType : endNodeTypes)
            {
                List<Class<? extends INode>> pair = List.of(aStartNodeType, anEndNodeType);
                edgeTypesByPair.computeIfAbsent(pair, key -> new ArrayList<Class<? extends IEdge>>()).add(edgeType);
            }
        }
    }

    static
    {
        ResourceBundle rs = ResourceBundle.getBundle(BpmnDiagramConstant.BPMN_DIAGRAM_STRINGS, Locale.getDefault());

        BpmnStartEventNode startEvent = new BpmnStartEventNode();
        startEvent.setToolTip(rs.getString("startevent.tooltip"));
        NODE_PROTOTYPES.add(startEvent);

        BpmnEndEventNode endEvent = new BpmnEndEventNode();
        endEvent.setToolTip(rs.getString("endevent.tooltip"));
        NODE_PROTOTYPES.add(endEvent);

        BpmnTaskNode task = new BpmnTaskNode();
        task.setToolTip(rs.getString("task.tooltip"));
        task.enableToolIcon();
        NODE_PROTOTYPES.add(task);

        BpmnTaskNode userTask = new BpmnTaskNode();
        userTask.setTaskType(TaskType.USER);
        userTask.setToolTip(rs.getString("task.user.tooltip"));
        userTask.enableToolIcon();
        NODE_PROTOTYPES.add(userTask);

        BpmnTaskNode serviceTask = new BpmnTaskNode();
        serviceTask.setTaskType(TaskType.SERVICE);
        serviceTask.setToolTip(rs.getString("task.service.tooltip"));
        serviceTask.enableToolIcon();
        NODE_PROTOTYPES.add(serviceTask);

        BpmnSubProcessNode subProcess = new BpmnSubProcessNode();
        subProcess.setToolTip(rs.getString("subprocess.tooltip"));
        subProcess.enableToolIcon();
        NODE_PROTOTYPES.add(subProcess);

        List<BpmnGatewayNode> gateways = new ArrayList<BpmnGatewayNode>();
        for (GatewayType aType : GatewayType.values())
        {
            BpmnGatewayNode gateway = new BpmnGatewayNode();
            gateway.setGatewayType(aType);
            gateway.setToolTip(rs.getString("gateway." + aType.name().toLowerCase(Locale.ROOT) + ".tooltip"));
            NODE_PROTOTYPES.add(gateway);
            gateways.add(gateway);
        }

        BpmnIntermediateEventNode timerEvent = new BpmnIntermediateEventNode();
        timerEvent.setTrigger(EventTrigger.TIMER);
        timerEvent.setToolTip(rs.getString("intermediateevent.timer.tooltip"));
        NODE_PROTOTYPES.add(timerEvent);

        BpmnIntermediateEventNode messageEvent = new BpmnIntermediateEventNode();
        messageEvent.setTrigger(EventTrigger.MESSAGE);
        messageEvent.setToolTip(rs.getString("intermediateevent.message.tooltip"));
        NODE_PROTOTYPES.add(messageEvent);

        BpmnStartEventNode messageStartEvent = new BpmnStartEventNode();
        messageStartEvent.setTrigger(EventTrigger.MESSAGE);
        messageStartEvent.setToolTip(rs.getString("startevent.message.tooltip"));
        NODE_PROTOTYPES.add(messageStartEvent);

        BpmnStartEventNode timerStartEvent = new BpmnStartEventNode();
        timerStartEvent.setTrigger(EventTrigger.TIMER);
        timerStartEvent.setToolTip(rs.getString("startevent.timer.tooltip"));
        NODE_PROTOTYPES.add(timerStartEvent);

        BpmnDataObjectNode dataObject = new BpmnDataObjectNode();
        dataObject.setToolTip(rs.getString("dataobject.tooltip"));
        NODE_PROTOTYPES.add(dataObject);

        BpmnDataStoreNode dataStore = new BpmnDataStoreNode();
        dataStore.setToolTip(rs.getString("datastore.tooltip"));
        NODE_PROTOTYPES.add(dataStore);

        BpmnTextAnnotationNode annotation = new BpmnTextAnnotationNode();
        annotation.setToolTip(rs.getString("annotation.tooltip"));
        annotation.enableToolIcon();
        NODE_PROTOTYPES.add(annotation);

        BpmnPoolNode pool = new BpmnPoolNode();
        pool.setToolTip(rs.getString("pool.tooltip"));
        pool.enableToolIcon();
        NODE_PROTOTYPES.add(pool);

        NoteNode note = new NoteNode();
        note.setToolTip(rs.getString("note.tooltip"));
        NODE_PROTOTYPES.add(note);

        ImageNode imageNode = new ImageNode();
        imageNode.setToolTip(rs.getString("image.tooltip"));
        NODE_PROTOTYPES.add(imageNode);

        ContainerNode container = new ContainerNode();
        container.setToolTip(rs.getString("container.tooltip"));
        container.enableToolIcon();
        NODE_PROTOTYPES.add(container);

        FreeTextNode freeTextNode = new FreeTextNode();
        freeTextNode.setToolTip(rs.getString("freetext.tooltip"));
        freeTextNode.enableToolIcon();
        NODE_PROTOTYPES.add(freeTextNode);

        BpmnSequenceFlowEdge sequenceFlow = new BpmnSequenceFlowEdge();
        sequenceFlow.setToolTip(rs.getString("sequenceflow.tooltip"));
        EDGE_PROTOTYPES.add(sequenceFlow);

        BpmnMessageFlowEdge messageFlow = new BpmnMessageFlowEdge();
        messageFlow.setToolTip(rs.getString("messageflow.tooltip"));
        EDGE_PROTOTYPES.add(messageFlow);

        BpmnAssociationEdge association = new BpmnAssociationEdge();
        association.setToolTip(rs.getString("association.tooltip"));
        EDGE_PROTOTYPES.add(association);

        EDGE_PROTOTYPES.addAll(GenericEdge.createToolPrototypes());

        TOOL_CATEGORIES.add(new ToolCategory(rs.getString("category.events"), startEvent, messageStartEvent, timerStartEvent,
                timerEvent, messageEvent, endEvent));
        TOOL_CATEGORIES.add(new ToolCategory(rs.getString("category.activities"), task, userTask, serviceTask, subProcess));
        TOOL_CATEGORIES.add(new ToolCategory(rs.getString("category.gateways"), gateways.toArray()));
        TOOL_CATEGORIES.add(new ToolCategory(rs.getString("category.data"), dataObject, dataStore, annotation));
        TOOL_CATEGORIES.add(new ToolCategory(rs.getString("category.participants"), pool));
        TOOL_CATEGORIES.add(new ToolCategory(rs.getString("category.flows"), sequenceFlow, messageFlow, association));
    }

}

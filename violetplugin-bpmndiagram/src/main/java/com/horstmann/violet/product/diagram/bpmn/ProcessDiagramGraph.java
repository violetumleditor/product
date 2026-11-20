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
 * A BPMN (Business Process Model and Notation) diagram, kept as simple as possible for people who don't know BPMN (user's
 * choice): only the basic elements of most processes, with plain names (start, task, decision, end, participant; flow and
 * message), each in its most common variant (no trigger, standard task, exclusive gateway, normal flow), the other variants
 * being a property of the element (the trigger of an event, the kind of a task or a gateway, the lanes of a pool, the kind
 * of a sequence flow).
 */
public class ProcessDiagramGraph extends AbstractGraph
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
        if (e instanceof ProcessSequenceFlowEdge && end instanceof ProcessStartEventNode)
        {
            return false;
        }
        if (e instanceof ProcessMessageFlowEdge && end instanceof ProcessEndEventNode)
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
            isSequenceFlowPossible |= aPrototype instanceof ProcessSequenceFlowEdge;
            isMessageFlowPossible |= aPrototype instanceof ProcessMessageFlowEdge;
        }
        if (!isSequenceFlowPossible || !isMessageFlowPossible)
        {
            return possibleEdges;
        }
        boolean isSamePool = findPool(start) == findPool(end);
        List<IEdge> result = new ArrayList<IEdge>();
        for (IEdge aPrototype : possibleEdges)
        {
            if ((aPrototype instanceof ProcessMessageFlowEdge && isSamePool)
                    || (aPrototype instanceof ProcessSequenceFlowEdge && !isSamePool))
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
    private ProcessPoolNode findPool(INode node)
    {
        Point2D location = node.getLocationOnGraph();
        Rectangle2D bounds = node.getBounds();
        Point2D center = new Point2D.Double(location.getX() + bounds.getWidth() / 2, location.getY() + bounds.getHeight() / 2);
        ProcessPoolNode result = null;
        for (INode aNode : getAllNodes())
        {
            if (!(aNode instanceof ProcessPoolNode) || aNode == node)
            {
                continue;
            }
            ProcessPoolNode aPool = (ProcessPoolNode) aNode;
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
     * events, tasks and gateways of a pool (nothing flows into a start event nor out of an end event); message flows join tasks
     * and events (no message leaves a start event nor reaches an end event).
     */
    private static final List<ConnectionRule> CONNECTION_RULES = createConnectionRules();

    private static final List<ToolCategory> TOOL_CATEGORIES = new ArrayList<ToolCategory>();

    private static List<ConnectionRule> createConnectionRules()
    {
        Map<List<Class<? extends INode>>, List<Class<? extends IEdge>>> edgeTypesByPair = new LinkedHashMap<List<Class<? extends INode>>, List<Class<? extends IEdge>>>();
        addRules(edgeTypesByPair, List.of(ProcessStartEventNode.class, ProcessTaskNode.class, ProcessGatewayNode.class), List.of(
                ProcessEndEventNode.class, ProcessTaskNode.class, ProcessGatewayNode.class), ProcessSequenceFlowEdge.class);
        addRules(edgeTypesByPair, List.of(ProcessTaskNode.class, ProcessEndEventNode.class), List.of(ProcessTaskNode.class,
                ProcessStartEventNode.class), ProcessMessageFlowEdge.class);

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
        ResourceBundle rs = ResourceBundle.getBundle(ProcessDiagramConstant.PROCESS_DIAGRAM_STRINGS, Locale.getDefault());

        ProcessStartEventNode startEvent = new ProcessStartEventNode();
        startEvent.setToolTip(rs.getString("startevent.tooltip"));
        NODE_PROTOTYPES.add(startEvent);

        ProcessTaskNode task = new ProcessTaskNode();
        task.setToolTip(rs.getString("task.tooltip"));
        task.enableToolIcon();
        NODE_PROTOTYPES.add(task);

        ProcessGatewayNode gateway = new ProcessGatewayNode();
        gateway.setToolTip(rs.getString("gateway.tooltip"));
        NODE_PROTOTYPES.add(gateway);

        ProcessEndEventNode endEvent = new ProcessEndEventNode();
        endEvent.setToolTip(rs.getString("endevent.tooltip"));
        NODE_PROTOTYPES.add(endEvent);

        ProcessPoolNode pool = new ProcessPoolNode();
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

        ProcessSequenceFlowEdge sequenceFlow = new ProcessSequenceFlowEdge();
        sequenceFlow.setToolTip(rs.getString("sequenceflow.tooltip"));
        EDGE_PROTOTYPES.add(sequenceFlow);

        ProcessMessageFlowEdge messageFlow = new ProcessMessageFlowEdge();
        messageFlow.setToolTip(rs.getString("messageflow.tooltip"));
        EDGE_PROTOTYPES.add(messageFlow);

        EDGE_PROTOTYPES.addAll(GenericEdge.createToolPrototypes());

        TOOL_CATEGORIES.add(new ToolCategory(rs.getString("category.process"), startEvent, task, gateway, endEvent, pool));
        TOOL_CATEGORIES.add(new ToolCategory(rs.getString("category.flows"), sequenceFlow, messageFlow));
    }

}

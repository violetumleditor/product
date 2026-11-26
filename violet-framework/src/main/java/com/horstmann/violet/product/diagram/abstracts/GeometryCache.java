package com.horstmann.violet.product.diagram.abstracts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.horstmann.violet.product.diagram.abstracts.edge.IEdge;
import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * Remembers what the elements compute again and again during one pass over a graph (drawing it, measuring it, looking for
 * the element under the mouse): the edges of each node, the corner points of each edge, the height a node gets from its
 * links. Nothing changes in the graph during such a pass, so what is computed once stays right until its end, then
 * everything is forgotten: the next pass starts from the model as it is then. No invalidation to maintain.
 * <p>
 * Without this, an edge computed its points about ten times per paint, each time looking through every edge of the graph
 * for the ones sharing its sides: a cost growing with the square of the number of edges, far too slow in the browser.
 * <p>
 * One pass per thread (an export may draw while the screen is painted). Passes nest: only the outermost one forgets.
 */
public final class GeometryCache
{
    private GeometryCache()
    {
    }

    /**
     * Starts a pass, to be ended by {@link #end()} in a finally block
     */
    public static void begin()
    {
        Pass pass = CURRENT.get();
        if (pass == null)
        {
            pass = new Pass();
            CURRENT.set(pass);
        }
        pass.depth++;
    }

    /**
     * Ends a pass started by {@link #begin()}; the outermost one forgets everything
     */
    public static void end()
    {
        Pass pass = CURRENT.get();
        if (pass == null)
        {
            return;
        }
        pass.depth--;
        if (pass.depth <= 0)
        {
            CURRENT.remove();
        }
    }

    /**
     * @return true while a pass runs on this thread and the cache is enabled
     */
    public static boolean isActive()
    {
        return getPass() != null;
    }

    /**
     * @param graph the graph of the node
     * @param node a node of this graph
     * @return the edges starting or ending at this node, in the order of the graph's edges (unmodifiable), or null when no
     *         pass runs: the caller then looks for them itself
     */
    public static List<IEdge> getConnectedEdges(IGraph graph, INode node)
    {
        Pass pass = getPass();
        if (pass == null || graph == null)
        {
            return null;
        }
        Map<INode, List<IEdge>> edgesByNode = pass.edgesByGraph.get(graph);
        if (edgesByNode == null)
        {
            edgesByNode = new IdentityHashMap<INode, List<IEdge>>();
            for (IEdge anEdge : graph.getAllEdges())
            {
                INode start = anEdge.getStartNode();
                INode end = anEdge.getEndNode();
                addEdge(edgesByNode, start, anEdge);
                if (end != start)
                {
                    addEdge(edgesByNode, end, anEdge);
                }
            }
            pass.edgesByGraph.put(graph, edgesByNode);
        }
        List<IEdge> edges = edgesByNode.get(node);
        return edges == null ? Collections.<IEdge> emptyList() : Collections.unmodifiableList(edges);
    }

    private static void addEdge(Map<INode, List<IEdge>> edgesByNode, INode node, IEdge edge)
    {
        if (node == null)
        {
            return;
        }
        List<IEdge> edges = edgesByNode.get(node);
        if (edges == null)
        {
            edges = new ArrayList<IEdge>(4);
            edgesByNode.put(node, edges);
        }
        edges.add(edge);
    }

    /**
     * @param element the element which computed the value
     * @param key what was computed (a constant of the caller)
     * @return the value remembered during this pass, or null (none, or no pass)
     */
    public static Object get(Object element, Object key)
    {
        Pass pass = getPass();
        if (pass == null)
        {
            return null;
        }
        Map<Object, Object> values = pass.values.get(key);
        return values == null ? null : values.get(element);
    }

    /**
     * Remembers a value until the end of the pass; does nothing when no pass runs
     *
     * @param element the element which computed the value
     * @param key what was computed (a constant of the caller)
     * @param value never null; must not be changed afterwards (give copies to the callers)
     */
    public static void put(Object element, Object key, Object value)
    {
        Pass pass = getPass();
        if (pass == null || value == null)
        {
            return;
        }
        Map<Object, Object> values = pass.values.get(key);
        if (values == null)
        {
            values = new IdentityHashMap<Object, Object>();
            pass.values.put(key, values);
        }
        values.put(element, value);
    }

    private static Pass getPass()
    {
        if (!enabled)
        {
            return null;
        }
        Pass pass = CURRENT.get();
        return pass == null || pass.suspended > 0 ? null : pass;
    }

    /**
     * Forgets what the current pass remembered, without ending it: to call after something changed the elements during the
     * pass (a diagram moving its elements just before drawing them)
     */
    public static void forget()
    {
        Pass pass = CURRENT.get();
        if (pass != null)
        {
            pass.edgesByGraph.clear();
            pass.values.clear();
        }
    }

    /**
     * Suspends the pass of this thread while an element computes something in a state of its own, changed for a moment
     * (an edge measuring its path without the user's adaptation): what is computed then is neither remembered nor taken from
     * what was remembered. To be followed by {@link #resume()} in a finally block.
     */
    public static void suspend()
    {
        Pass pass = CURRENT.get();
        if (pass != null)
        {
            pass.suspended++;
        }
    }

    /**
     * Ends a {@link #suspend()}
     */
    public static void resume()
    {
        Pass pass = CURRENT.get();
        if (pass != null && pass.suspended > 0)
        {
            pass.suspended--;
        }
    }

    /**
     * Turns the cache (and the other optimizations of the drawing that rely on it) on or off, for tests comparing both
     * renderings
     */
    public static void setEnabled(boolean isEnabled)
    {
        enabled = isEnabled;
    }

    /**
     * @return false when turned off by {@link #setEnabled(boolean)}
     */
    public static boolean isEnabled()
    {
        return enabled;
    }

    private static final class Pass
    {
        private int depth;
        private int suspended;
        private final Map<IGraph, Map<INode, List<IEdge>>> edgesByGraph = new IdentityHashMap<IGraph, Map<INode, List<IEdge>>>();
        private final Map<Object, Map<Object, Object>> values = new IdentityHashMap<Object, Map<Object, Object>>();
    }

    private static final ThreadLocal<Pass> CURRENT = new ThreadLocal<Pass>();

    private static volatile boolean enabled = true;
}

package com.horstmann.violet.product.diagram.sequence;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.horstmann.violet.product.diagram.abstracts.node.INode;

/**
 * Lives as long as the drag of one bar. When a called activation bar is moved, the bar of the other lifeline calling it moves too,
 * instead of being resized, and so on up to the first caller. A nested caller moves with the whole bar it is in. A caller that has
 * other callees only moves as far as it keeps its length without moving them, then it is resized.
 * <p>
 * Upwards, the bars called by the dragged one move with it too, else it would get longer: if one of them can't go further up (head of
 * its lifeline, bar above), the dragged one is blocked too. A caller that can't go further up gets shorter instead, and only blocks
 * the dragged bar once its callee reaches its top.
 */
class LinkedBarsMover
{
    /**
     * @param graph the graph
     * @param draggedBar the bar being dragged
     * @param drawnBounds the bounds on graph of all bars and lifelines, as they were just before the drag
     */
    LinkedBarsMover(SequenceDiagramGraph graph, ActivationBarNode draggedBar, Map<INode, Rectangle2D> drawnBounds)
    {
        this.draggedBar = draggedBar;
        this.startBounds = new HashMap<>(drawnBounds);
        this.messages = new Messages(graph);
        for (CallEdge aCall : this.messages.getCalls())
        {
            if (isDrawn(aCall.getStartNode()) && isDrawn(aCall.getEndNode()))
            {
                ActivationBarNode caller = (ActivationBarNode) aCall.getStartNode();
                if (aCall.getEndNode() instanceof ActivationBarNode)
                {
                    this.calls.add(new Call(caller, (ActivationBarNode) aCall.getEndNode()));
                }
                else
                {
                    this.creators.put(aCall.getEndNode(), caller);
                }
            }
        }
        for (Call aCall : this.calls)
        {
            ActivationBarNode root = getRoot(aCall.caller);
            INode creator = this.creators.get(aCall.callee.getImplicitParameter());
            if (aCall.caller.getImplicitParameter() != aCall.callee.getImplicitParameter() && !this.messages.isInside(draggedBar, root)
                    && !this.messages.isInside(aCall.caller, draggedBar) && (creator == null || getRoot(creator) != root))
            {
                this.callsByRoot.computeIfAbsent(root, k -> new ArrayList<>()).add(aCall);
            }
        }
        this.callees.addAll(findCallees());
    }

    ActivationBarNode getDraggedBar()
    {
        return this.draggedBar;
    }

    /**
     * Places the linked bars again, from where they were at the beginning of the drag, and holds the dragged one back if they are
     * blocked
     */
    void follow()
    {
        double askedDy = this.draggedBar.getAskedLocation().getY() - getStartY(this.draggedBar);
        double dy = askedDy;
        Map<INode, Double> moves = getMoves(dy);
        while (dy < 0 && !isPossible(moves))
        {
            dy = Math.min(0, dy + STEP);
            moves = getMoves(dy);
        }
        if (dy != askedDy)
        {
            place(this.draggedBar, dy);
        }
        Set<ActivationBarNode> linkedBars = new LinkedHashSet<>(this.callsByRoot.keySet());
        linkedBars.addAll(this.callees);
        this.calls.stream().map(c -> c.caller).filter(b -> b != this.draggedBar).forEach(linkedBars::add);
        for (ActivationBarNode aBar : linkedBars)
        {
            double move = moves.getOrDefault(aBar, 0d);
            if (move != this.appliedMoves.getOrDefault(aBar, 0d))
            {
                place(aBar, move);
                this.appliedMoves.put(aBar, move);
            }
        }
    }

    private void place(ActivationBarNode bar, double dy)
    {
        bar.setLocation(new Point2D.Double(bar.getAskedLocation().getX(), getStartY(bar) + dy));
    }

    /**
     * @return how much each bar moves by itself when the dragged one moves by dy
     */
    private Map<INode, Double> getMoves(double dy)
    {
        Map<INode, Double> moves = new HashMap<>();
        moves.put(this.draggedBar, dy);
        if (dy < 0)
        {
            this.callees.forEach(c -> moves.put(c, dy));
        }
        // A caller may be called too: again until nothing moves
        boolean isChanged = true;
        for (int i = 0; isChanged && i <= 2 * this.calls.size() + 2; i++)
        {
            isChanged = false;
            for (Map.Entry<ActivationBarNode, List<Call>> entry : this.callsByRoot.entrySet())
            {
                ActivationBarNode root = entry.getKey();
                if (dy < 0 && this.callees.stream().anyMatch(c -> this.messages.isInside(c, root) || this.messages.isInside(root, c)))
                {
                    continue;
                }
                double move = getCallerMove(root, entry.getValue(), moves);
                isChanged |= setMove(root, move, moves);
                if (dy < 0)
                {
                    isChanged |= moveNestedCallers(root, entry.getValue(), moves);
                }
            }
        }
        return moves;
    }

    private boolean setMove(ActivationBarNode bar, double move, Map<INode, Double> moves)
    {
        Double formerMove = moves.put(bar, move);
        return move != (formerMove == null ? 0 : formerMove);
    }

    /**
     * Once the whole bar can't go further up, a nested caller reached by its callee goes up inside it
     */
    private boolean moveNestedCallers(ActivationBarNode root, List<Call> callerCalls, Map<INode, Double> moves)
    {
        Map<ActivationBarNode, Double> nestedMoves = new LinkedHashMap<>();
        for (Call aCall : callerCalls)
        {
            if (aCall.caller != root)
            {
                double carried = getMove(aCall.caller, moves) - moves.getOrDefault(aCall.caller, 0d);
                double need = Math.max(0, getSlack(aCall)) + getMove(aCall.callee, moves) - carried;
                nestedMoves.merge(aCall.caller, Math.min(0, need), Math::min);
            }
        }
        boolean isChanged = false;
        for (Map.Entry<ActivationBarNode, Double> entry : nestedMoves.entrySet())
        {
            double room = getRoom(entry.getKey(), moves);
            isChanged |= setMove(entry.getKey(), Math.max(entry.getValue(), -room), moves);
        }
        return isChanged;
    }

    /**
     * The caller follows its bottom, i.e. its lowest callee, but stays above each of its callees. It goes up as far as it can, and
     * not if that would make one of its nested bars longer.
     */
    private double getCallerMove(ActivationBarNode root, List<Call> callerCalls, Map<INode, Double> moves)
    {
        double formerBottom = Double.NEGATIVE_INFINITY;
        double newBottom = Double.NEGATIVE_INFINITY;
        double maxMove = Double.POSITIVE_INFINITY;
        double leastNestedMove = Double.NEGATIVE_INFINITY;
        for (Call aCall : callerCalls)
        {
            double dy = getMove(aCall.callee, moves);
            double bottom = this.startBounds.get(aCall.callee).getMaxY();
            formerBottom = Math.max(formerBottom, bottom);
            newBottom = Math.max(newBottom, bottom + dy);
            maxMove = Math.min(maxMove, Math.max(0, getSlack(aCall)) + dy);
            if (aCall.caller != root)
            {
                leastNestedMove = Math.max(leastNestedMove, dy);
            }
        }
        double move = newBottom - formerBottom;
        if (move < 0)
        {
            move = Math.max(move, Math.min(0, leastNestedMove));
        }
        move = Math.min(move, maxMove);
        return move < 0 ? Math.max(move, -getRoomAbove(root, moves, new LinkedHashSet<>())) : move;
    }

    /**
     * @return how much a caller may go up: below the head of its lifeline, the bars above and its own callers, which go up too
     */
    private double getRoomAbove(ActivationBarNode root, Map<INode, Double> moves, Set<INode> visited)
    {
        visited.add(root);
        double room = getRoom(root, moves);
        for (Call aCall : this.calls)
        {
            ActivationBarNode callerRoot = getRoot(aCall.caller);
            if (this.messages.isInside(aCall.callee, root) && this.callsByRoot.containsKey(callerRoot) && !visited.contains(callerRoot))
            {
                room = Math.min(room, Math.max(0, getSlack(aCall)) + getRoomAbove(callerRoot, moves, visited));
            }
        }
        return room;
    }

    /**
     * @return how much a bar may go up in its parent, under the bars above
     */
    private double getRoom(ActivationBarNode bar, Map<INode, Double> moves)
    {
        return Math.min(getRoomInParent(bar), getRoomUnderSiblings(bar, moves));
    }

    private double getRoomUnderSiblings(ActivationBarNode bar, Map<INode, Double> moves)
    {
        double result = Double.POSITIVE_INFINITY;
        for (ActivationBarNode aSibling : this.messages.getBars(bar.getParent().getChildren()))
        {
            if (aSibling != bar && isDrawn(aSibling) && !this.messages.isRunningTogether(bar, aSibling))
            {
                double room = this.startBounds.get(bar).getY() - this.startBounds.get(aSibling).getMaxY();
                if (room >= 0)
                {
                    double siblingMove = Math.min(0, moves.getOrDefault(aSibling, 0d));
                    result = Math.min(result, Math.max(0, room - ActivationBarSpacing.GAP) - siblingMove);
                }
            }
        }
        return result;
    }

    /**
     * @return how much closer to its caller's top the callee may come
     */
    private double getSlack(Call call)
    {
        return this.startBounds.get(call.callee).getY() - this.startBounds.get(call.caller).getY() - call.callee.getGapBelowCaller(call.caller);
    }

    /** A bar moves with the bars it is in, and with the creator of its lifeline */
    private double getMove(INode bar, Map<INode, Double> moves)
    {
        double dy = 0;
        Set<INode> visited = new LinkedHashSet<>();
        for (INode aNode = bar; aNode != null && visited.add(aNode);)
        {
            dy += moves.getOrDefault(aNode, 0d);
            aNode = aNode.getParent() == null ? this.creators.get(aNode) : aNode.getParent();
        }
        return dy;
    }

    /**
     * A bar moved alone may always move, and cross the ones above. Else no bar moved up comes too close to what is above it.
     */
    private boolean isPossible(Map<INode, Double> moves)
    {
        List<ActivationBarNode> movedBars = new ArrayList<>();
        for (Map.Entry<INode, Double> entry : moves.entrySet())
        {
            if (entry.getValue() != 0)
            {
                movedBars.add((ActivationBarNode) entry.getKey());
            }
        }
        if (movedBars.size() < 2)
        {
            return true;
        }
        for (ActivationBarNode aBar : movedBars)
        {
            double move = moves.get(aBar);
            if (move < 0 && move < -getRoom(aBar, moves))
            {
                return false;
            }
        }
        for (Call aCall : this.calls)
        {
            double dy = getMove(aCall.callee, moves) - getMove(aCall.caller, moves);
            if (dy < 0 && dy < -Math.max(0, getSlack(aCall)))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * @return how much the bar may go up in its parent
     */
    private double getRoomInParent(ActivationBarNode bar)
    {
        double minY = ActivationBarNode.CALL_YGAP;
        if (bar.getParent() instanceof LifelineNode)
        {
            minY += ((LifelineNode) bar.getParent()).getTopRectangle().getHeight();
        }
        return Math.max(0, getStartY(bar) - minY);
    }

    /**
     * @return the bars called by the dragged one, directly or not, that don't already move with a bar they are in, a caller of the
     *         dragged one or the creator of their lifeline
     */
    private Set<ActivationBarNode> findCallees()
    {
        Set<ActivationBarNode> innerBars = this.messages.getInnerBars(this.draggedBar);
        Set<ActivationBarNode> result = new LinkedHashSet<>();
        for (Call aCall : this.calls)
        {
            ActivationBarNode callee = aCall.callee;
            boolean isMovedOtherwise = innerBars.contains(callee.getParent()) || this.messages.isInside(callee, this.draggedBar)
                    || innerBars.contains(this.creators.get(callee.getImplicitParameter()))
                    || this.messages.getInnerBars(getRoot(callee)).contains(this.draggedBar);
            if (innerBars.contains(aCall.caller) && !isMovedOtherwise)
            {
                result.add(callee);
            }
        }
        return result;
    }

    private boolean isDrawn(INode node)
    {
        return this.startBounds.containsKey(node);
    }

    /**
     * @return y in the parent at the beginning of the drag
     */
    private double getStartY(ActivationBarNode bar)
    {
        return this.startBounds.get(bar).getY() - this.startBounds.get(bar.getParent()).getY();
    }

    private ActivationBarNode getRoot(INode bar)
    {
        INode root = bar;
        while (root.getParent() instanceof ActivationBarNode)
        {
            root = root.getParent();
        }
        return (ActivationBarNode) root;
    }

    private static class Call
    {
        Call(ActivationBarNode caller, ActivationBarNode callee)
        {
            this.caller = caller;
            this.callee = callee;
        }

        private final ActivationBarNode caller;
        private final ActivationBarNode callee;
    }

    /** A grid step: how the move asked is reduced until it is possible */
    private static final double STEP = 10;

    private final ActivationBarNode draggedBar;

    private final Messages messages;

    /** On the graph, at the beginning of the drag */
    private final Map<INode, Rectangle2D> startBounds;

    private final List<Call> calls = new ArrayList<>();

    /** Bar that creates each created lifeline */
    private final Map<INode, ActivationBarNode> creators = new HashMap<>();

    /** Calls to other lifelines of each bar put on a lifeline, its nested bars included */
    private final Map<ActivationBarNode, List<Call>> callsByRoot = new LinkedHashMap<>();

    /** Bars that move up with the dragged one */
    private final Set<ActivationBarNode> callees = new LinkedHashSet<>();

    private final Map<ActivationBarNode, Double> appliedMoves = new HashMap<>();
}

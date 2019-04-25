package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.determinize.AllPlayerDeterminiser;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.LegalActionFilter;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StatsSummary;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.BasicStats;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Created by webpigeon on 22/08/16.
 */
public class MCTSNode {

    public static final double DEFAULT_EXP_CONST = Math.sqrt(2);
    public static final AtomicLong idFountain = new AtomicLong(1);

    protected static final int MAX_SCORE = 25;
    protected static final double EPSILON = 1e-6;
    protected static final boolean DISCOUNT_ENABLED = false;

    public final double expConst;
    private final long uniqueID = idFountain.getAndIncrement();
    private GameState referenceState;
    protected final Action moveToState;
    protected final int agentId;
    protected final MCTSNode parent;
    protected final List<MCTSNode> children;
    protected final Collection<Action> allUnexpandedActions;
    protected final Random random;
    protected final int depth;
    protected final Logger logger = LoggerFactory.getLogger(MCTSNode.class);

    protected double score;
    protected int visits;
    protected Map<Action, Integer> parentWasVisitedAndIWasLegal = new HashMap<>();

    public final StatsSummary rolloutScores;
    public final StatsSummary rolloutMoves;

    public MCTSNode(Collection<Action> allUnexpandedActions) {
        this(null, -1, null, DEFAULT_EXP_CONST, allUnexpandedActions);
    }

    public MCTSNode(double expConst, Collection<Action> allUnexpandedActions) {
        this(null, -1, null, expConst, allUnexpandedActions);
    }

    public MCTSNode(int agentID, Action moveToState, Collection<Action> allUnexpandedActions) {
        this(null, agentID, moveToState, DEFAULT_EXP_CONST, allUnexpandedActions);
    }

    public MCTSNode(int agentID, Action moveToState, double expConst, Collection<Action> allUnexpandedActions) {
        this(null, agentID, moveToState, expConst, allUnexpandedActions);
    }

    public MCTSNode(MCTSNode parent, int agentId, Action moveToState, Collection<Action> allUnexpandedActions) {
        this(parent, agentId, moveToState, DEFAULT_EXP_CONST, allUnexpandedActions);
    }

    public MCTSNode(MCTSNode parent, int agentId, Action moveToState, double expConst, Collection<Action> allUnexpandedActions) {
        this(parent, agentId, moveToState, DEFAULT_EXP_CONST, allUnexpandedActions, 0, 0);
    }

    public MCTSNode(MCTSNode parent, int agentId, Action moveToState, double expConst, Collection<Action> allUnexpandedActions,
                    int initialVisits, double initialScore) {
        this.expConst = expConst;
        this.parent = parent;
        this.agentId = agentId;
        this.moveToState = moveToState;
        this.score = initialScore * initialVisits;
        this.visits = initialVisits;
        if (initialVisits > 0) parentWasVisitedAndIWasLegal.put(moveToState, initialVisits);
        this.children = new ArrayList<>();
        this.allUnexpandedActions = new ArrayList<>(allUnexpandedActions);
        this.random = new Random();
        this.depth = (parent == null) ? 0 : parent.depth + 1;

        this.rolloutScores = new BasicStats();
        this.rolloutMoves = new BasicStats();

        assert (parent != null && moveToState != null) || (parent == null && moveToState == null);
    }


    public void addChild(MCTSNode node) {
        allUnexpandedActions.remove(node.getAction());
        children.add(node);
    }

    public double getUCTValue() {
        if (parent == null) {
            return 0;
        }

        int parentalVisits = parent.parentWasVisitedAndIWasLegal.get(this.moveToState);
        return ((score / MAX_SCORE) / visits) + (expConst * Math.sqrt(Math.log(parentalVisits) / visits));
    }

    public List<MCTSNode> getChildren() {
        return children;
    }

    public void backup(double score, MCTSNode triggerNode, MCTSNode stopNode) {
        MCTSNode current = this;
        int iterations = 0;
        MCTSNode last = null;
        while (current != null) {
            if (stopNode != null && stopNode == current) {
                current = null;
                continue;
                // stop back-propagation
            } else if (triggerNode == null || triggerNode == current) {
                triggerNode = null;
                if (DISCOUNT_ENABLED) {
                    current.score += score * Math.pow(0.95, current.getDepth() - 1.0);
                } else {
                    current.score += score;
                }
                iterations++;
                current.visits++;
            }
            last = current;
            current = current.parent;
        }
        logger.debug(String.format("Stopping back-prop after %d nodes stopping at %s", iterations, last.toString()));
    }


    /*
    Called when we descend the tree in select()
     */
    public MCTSNode getUCTNode(GameState state, boolean trial) {
        double bestScore = -Double.MAX_VALUE;
        MCTSNode bestChild = null;

        for (MCTSNode child : children) {
            //XXX Hack to check if the move is legal in this version
            Action moveToMake = child.moveToState;
            if (!LegalActionFilter.isLegal(child.agentId, state).test(moveToMake))
                continue;

            double childScore = child.getUCTValue() + (random.nextDouble() * EPSILON);
            if (logger.isDebugEnabled())
                logger.debug(String.format("\tUCT: %.2f from base %.2f (%d/%d complete/eligible visits) for %s", childScore, child.score / child.visits,
                        child.visits, parentWasVisitedAndIWasLegal.get(moveToMake), moveToMake));

            if (childScore > bestScore) {
                bestScore = childScore;
                bestChild = child;
            }
        }
        if (!trial) incrementParentVisitsForAllEligibleActions(state);

        if (logger.isDebugEnabled()) logger.debug(String.format("\tChosen Action is %s", bestChild.moveToState));
        return bestChild;
    }


    protected void incrementParentVisitsForAllEligibleActions(GameState state) {
        for (MCTSNode child : children) {
            if (LegalActionFilter.isLegal(child.agentId, state).test(child.moveToState))
                incrementParentVisit(child.moveToState);
        }
        for (Action unexpandedAction : getLegalUnexpandedMoves(state, (getAgentId() + 1) % state.getPlayerCount())) {
            incrementParentVisit(unexpandedAction);
            // we still need to increment the count for this, even though it is not yet expanded
        }
    }

    private void incrementParentVisit(Action a) {
        if (!parentWasVisitedAndIWasLegal.containsKey(a)) {
            parentWasVisitedAndIWasLegal.put(a, 1);
        } else {
            parentWasVisitedAndIWasLegal.put(a, parentWasVisitedAndIWasLegal.get(a) + 1);
        }
    }

    public Action getAction() {
        return moveToState;
    }

    public MCTSNode getBestNode() {
        double bestScore = -Double.MAX_VALUE;
        MCTSNode bestChild = null;

        for (MCTSNode child : children) {
            //       double childScore = child.score / child.visits + (random.nextDouble() * EPSILON);
            double childScore = child.visits + child.score / 1000.0 + (random.nextDouble() * EPSILON);
            if (childScore > bestScore) {
                bestScore = childScore;
                bestChild = child;
            }
        }

        assert bestChild != null;
        return bestChild;
    }

    public int getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return String.format("NODE %d (%d: %s %f)", uniqueID, getDepth(), moveToState, score);
    }

    public boolean containsChild(Action moveToChild) {
        for (MCTSNode child : children) {
            if (child.moveToState.equals(moveToChild)) {
                return true;
            }
        }
        return false;
    }

    public MCTSNode getChild(Action action) {
        for (MCTSNode child : children) {
            if (child.moveToState.equals(action)) {
                return child;
            }
        }
        return null;
    }

    public boolean fullyExpanded(GameState state) {
        return fullyExpanded(state, (agentId + 1) % state.getPlayerCount());
    }

    public boolean fullyExpanded(GameState state, int nextId) {
        return getLegalUnexpandedMoves(state, nextId).isEmpty();
    }

    public Collection<Action> getLegalUnexpandedMoves(GameState state, int nextId) {
        return allUnexpandedActions.stream()
                .filter(LegalActionFilter.isLegal(nextId, state))
                .collect(Collectors.toList());
    }

    public Collection<Action> getAllActionsExpandedAlready() {
        ArrayList<Action> actions = new ArrayList<>();
        children.forEach(node -> actions.add(node.getAction()));
        return actions;
    }

    public void printChildren() {
        logger.trace("\t {}\t {}\t {}\t {}", "action", "visits", "score", "avg");
        for (MCTSNode child : children) {
            logger.trace("\t{}\t{}\t{}\t{}", child.getAction(), child.visits, child.score, child.score / child.visits);
        }
    }

    public String printD3() {
        StringBuilder buffer = new StringBuilder();
        printD3Internal(buffer);
        return buffer.toString();
    }

    private void printD3Internal(StringBuilder buffer) {
        buffer.append("{\"name\": \"\"");
        if (!children.isEmpty()) {
            buffer.append(",\"children\":[");
            for (int i = 0; i < children.size(); i++) {
                if (i != 0) {
                    buffer.append(",");
                }
                children.get(i).printD3Internal(buffer);
            }
            buffer.append("]");
        }
        buffer.append("}");
    }

    /**
     * Keep track of stats for rollouts.
     *
     * @param moves The number of moves made for a given rollout
     * @param score The total score achieved at the end of the rollout
     */
    public void backupRollout(int moves, int score) {
        rolloutMoves.add(moves);
        rolloutScores.add(score);
        if (parent != null) {
            parent.backupRollout(moves + 1, score);
        }
    }

    public GameState getReferenceState() {
        return referenceState;
    }

    /* it is the caller's responsibility to pass a copy of a state in
    so that it is not mutated by any other actor. The reference state is the state
    that applies after the action is taken - so we do not know this on Node creation
     */
    public void setReferenceState(GameState refState) {
        // We also ensure that the next player (agentID + 1)
        // has their hand set to null as Unknown
        Hand activehand = refState.getHand((agentId + 1) % refState.getPlayerCount());
        Card[] cards = new Card[activehand.getSize()];
        for (int i = 0; i < activehand.getSize(); i++) {
            if (activehand.hasCard(i)) {
                cards[i] = activehand.getCard(i);
                activehand.bindCard(i, null);
                if (cards[i] != null) {
                    refState.getDeck().add(cards[i]);
                }
            }
        }
        referenceState = refState;
    }

    public int getVisits() {
        return visits;
    }

    public double getMeanScore() {
        return score / visits;
    }

    public int getAgentId() {
        return agentId;
    }
}

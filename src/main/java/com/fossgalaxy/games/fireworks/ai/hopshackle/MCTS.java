package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.ai.mcts.IterationObject;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.annotations.AgentBuilderStatic;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import com.fossgalaxy.games.fireworks.utils.DebugUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by WebPigeon on 09/08/2016.
 */
public class MCTS implements Agent, HasGameOverProcessing {
    public static final int DEFAULT_ITERATIONS = 50_000;
    public static final int DEFAULT_ROLLOUT_DEPTH = 18;
    public static final int DEFAULT_TREE_DEPTH_MUL = 1;
    public static final int DEFAULT_TIME_LIMIT = 1000;
    public static final int NO_LIMIT = 100;

    //   protected final int roundLength;
    protected final int rolloutDepth;
    protected final int treeDepthMul;
    protected final int timeLimit;
    public final double C;
    protected final Random random;
    protected final Logger logger = LoggerFactory.getLogger(MCTS.class);
    protected StateGatherer stateGatherer;
    protected HasGameOverProcessing endGameProcessor;
    protected final boolean calcTree = false;

    /**
     * Create a default MCTS implementation.
     * <p>
     * This creates an MCTS agent that has a default roll-out length of 50_000 iterations, a depth of 18 and a tree
     * multiplier of 1.
     */
    public MCTS() {
        this(MCTSNode.DEFAULT_EXP_CONST, DEFAULT_ROLLOUT_DEPTH, DEFAULT_TREE_DEPTH_MUL, DEFAULT_TIME_LIMIT);
    }

    public MCTS(double expConst) {
        this(expConst, DEFAULT_ROLLOUT_DEPTH, DEFAULT_TREE_DEPTH_MUL, DEFAULT_TIME_LIMIT);
    }

    /**
     * Create an MCTS agent which has the parameters.
     *
     * @param explorationC
     * @param rolloutDepth
     * @param treeDepthMul
     * @param timeLimit    in ms
     */
    @AgentConstructor("mcts")
    public MCTS(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
//        this.roundLength = roundLength;
        this.rolloutDepth = rolloutDepth;
        this.treeDepthMul = treeDepthMul;
        this.timeLimit = timeLimit;
        this.C = explorationC;
        this.random = new Random();
    }

    @AgentBuilderStatic("mctsND")
    public static MCTS buildMCTSND() {
        return new MCTS(MCTSNode.DEFAULT_EXP_CONST, MCTS.NO_LIMIT, MCTS.NO_LIMIT, DEFAULT_TIME_LIMIT);
    }

    protected MCTSNode createNode(MCTSNode parent, int previousAgentID, Action moveTo, GameState state) {
        MCTSNode root = new MCTSNode(
                parent,
                previousAgentID,
                moveTo, C,
                Utils.generateAllActions((previousAgentID + 1) % state.getPlayerCount(), state.getPlayerCount())
        );
        return root;
    }

    public void setStateGatherer(StateGatherer sg) {
        stateGatherer = sg;
    }
    public void setEndGameProcessor(HasGameOverProcessing egp) {
        endGameProcessor = egp;
    }

    @Override
    public Action doMove(int agentID, GameState state) {
        long finishTime = System.currentTimeMillis() + timeLimit;

        MCTSNode root = createNode(null, (agentID - 1 + state.getPlayerCount()) % state.getPlayerCount(), null, state);
        Map<Integer, List<Card>> possibleCards = DeckUtils.bindCard(agentID, state.getHand(agentID), state.getDeck().toList());
        List<Integer> bindOrder = DeckUtils.bindOrder(possibleCards);

        logDebugGameState(state, agentID);

//        for (int round = 0; round < roundLength; round++) {
        while (System.currentTimeMillis() < finishTime) {
            //find a leaf node
            GameState currentState = state.getCopy();
            IterationObject iterationObject = new IterationObject(agentID);

            Map<Integer, Card> myHandCards = DeckUtils.bindCards(bindOrder, possibleCards);

            Deck deck = currentState.getDeck();
            Hand myHand = currentState.getHand(agentID);
            for (int slot = 0; slot < myHand.getSize(); slot++) {
                Card hand = myHandCards.get(slot);
                myHand.bindCard(slot, hand);
                deck.remove(hand);
            }
            deck.shuffle();

            MCTSNode current = select(root, currentState, iterationObject);
            int score = rollout(currentState, current);
            current.backup(score);
            if (calcTree) {
                System.out.println(root.printD3());
            }
        }

        if (logger.isInfoEnabled()) {
            for (MCTSNode level1 : root.getChildren()) {
                logger.info("rollout {} moves: max: {}, min: {}, avg: {}, N: {} ", level1.getAction(), level1.rolloutMoves.getMax(), level1.rolloutMoves.getMin(), level1.rolloutMoves.getMean(), level1.rolloutMoves.getN());
                logger.info("rollout {} scores: max: {}, min: {}, avg: {}, N: {} ", level1.getAction(), level1.rolloutScores.getMax(), level1.rolloutScores.getMin(), level1.rolloutScores.getMean(), level1.rolloutScores.getN());
            }
        }
/*
        if (logger.isTraceEnabled()) {
            logger.trace("next player's moves considerations: ");
            for (MCTSNode level1 : root.getChildren()) {
                logger.trace("{}'s children", level1.getAction());
                level1.printChildren();
            }
        }
*/
        Action chosenOne = root.getBestNode().getAction();
        if (logger.isTraceEnabled()) {
            logger.trace("Move Chosen by {} was {}", agentID, chosenOne);
            root.printChildren();
        }

        if (stateGatherer != null) {
            stateGatherer.storeData(root, state, agentID);
        }

        return chosenOne;
    }

    protected void logDebugGameState(GameState state, int agentID) {
        if (logger.isTraceEnabled()) {
            Map<Integer, List<Card>> possibleCards = DeckUtils.bindCard(agentID, state.getHand(agentID), state.getDeck().toList());

            logger.trace("Possible bindings: ");
            possibleCards.forEach((slot, cards) -> logger.trace("\t {} {}", slot, DebugUtils.getHistStr(DebugUtils.histogram(cards))));

            // Guaranteed cards
            logger.trace("Guaranteed Cards");

            possibleCards.entrySet().stream()
                    .filter(x -> x.getValue().size() == 1)
                    .forEach(this::printCard);

            logger.trace("We know the value of these");
            possibleCards.entrySet().stream()
                    .filter(x -> x.getValue().stream().allMatch(y -> y.value.equals(x.getValue().get(0).value)))
                    .forEach(this::printCard);

            DebugUtils.printTable(logger, state);
        }
    }

    protected MCTSNode select(MCTSNode root, GameState state, IterationObject iterationObject) {
        MCTSNode current = root;
        int treeDepth = calculateTreeDepthLimit(state);
        boolean expandedNode = false;

        while (!state.isGameOver() && current.getDepth() < treeDepth && !expandedNode) {
            MCTSNode next;
            if (current.fullyExpanded(state)) {
                next = current.getUCTNode(state);
            } else {
                next = expand(current, state);
                expandedNode = true;
                //            return next;
            }
            if (next == null) {
                //XXX if all follow on states explored so far are null, we are now a leaf node
                return current;
            }
            current = next;

            int agent = current.getAgent();
            int lives = state.getLives();
            int score = state.getScore();

            Action action = current.getAction();
            logger.trace("Selected action " + action + " for player " + agent);
            if (action != null) {
                List<GameEvent> events = action.apply(agent, state);
                events.forEach(state::addEvent);
                state.tick();
            }

            if (iterationObject.isMyGo(agent)) {
                if (state.getLives() < lives) {
                    iterationObject.incrementLivesLostMyGo();
                }
                if (state.getScore() > score) {
                    iterationObject.incrementPointsGainedMyGo();
                }
            }
        }
        return current;
    }

    protected int calculateTreeDepthLimit(GameState state) {
        return (state.getPlayerCount() * treeDepthMul) + 1;
    }

    /**
     * Select a new action for the expansion node.
     *
     * @param state   the game state to travel from
     * @param agentID the AgentID to use for action selection
     * @param node    the Node to use for expansion
     * @return the next action to be added to the tree from this state.
     */
    protected Action selectActionForExpand(GameState state, MCTSNode node, int agentID) {
        Collection<Action> legalActions = node.getLegalUnexpandedMoves(state, agentID);
        if (legalActions.isEmpty()) {
            return null;
        }

        Iterator<Action> actionItr = legalActions.iterator();

        int selected = random.nextInt(legalActions.size());
        Action curr = actionItr.next();
        for (int i = 0; i < selected; i++) {
            curr = actionItr.next();
        }
        logger.trace("Selected action " + curr + " for expansion from node:");
        // node.printChildren();
        return curr;
    }

    protected MCTSNode expand(MCTSNode parent, GameState state) {
        int nextAgentID = (parent.getAgent() + 1) % state.getPlayerCount();
        Action action = selectActionForExpand(state, parent, nextAgentID);
        // It is possible it wasn't allowed
        if (action == null) {
            return parent;
        }
        if (parent.containsChild(action)) {
            // return the correct node instead
            return parent.getChild(action);
        }
        //XXX we may expand a node which we already visited? :S
        MCTSNode child = createNode(parent, nextAgentID, action, state);
        parent.addChild(child);
        return child;
    }

    protected Action selectActionForRollout(GameState state, int playerID) {
        Collection<Action> legalActions = Utils.generateActions(playerID, state);

        List<Action> listAction = new ArrayList<>(legalActions);
        Collections.shuffle(listAction);
        if (listAction.isEmpty()) {
            for (int i = 0; i < state.getHandSize(); i++) {
                if (state.getHand(playerID).hasCard(i)) {
                    return new DiscardCard(i);
                }
            }
        }

        return listAction.get(0);
    }

    protected int rollout(GameState state, MCTSNode current) {

        int playerID = (current.getAgent() + 1) % state.getPlayerCount();
        // we rollout from current, which records the agent who acted to reach it
        int moves = 0;

        while (!state.isGameOver() && moves < rolloutDepth) {
            Action action = selectActionForRollout(state, playerID);
            List<GameEvent> events = action.apply(playerID, state);
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Player %d, Rollout: %s", playerID, action));
                events.forEach(e -> logger.trace(String.format("\t%s", e.toString())));
            }
            events.forEach(state::addEvent);
            state.tick();
            playerID = (playerID + 1) % state.getPlayerCount();
            moves++;
        }
        logger.trace(String.format("Terminating rollout after %d moves with score of %d", moves, state.getScore()));
        current.backupRollout(moves, state.getScore());
        return state.getScore();
    }

    @Override
    public String toString() {
        return "MCTS";
    }

    protected void printCard(Map.Entry<Integer, List<Card>> entry) {
        logger.trace("{} : {}", entry.getKey(), entry.getValue());
    }

    @Override
    public void onGameOver(double finalScore) {
        if (endGameProcessor != null)
            endGameProcessor.onGameOver(finalScore);
    }
}

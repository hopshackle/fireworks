package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.*;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import com.fossgalaxy.games.fireworks.utils.DebugUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by WebPigeon on 09/08/2016.
 */
public class MCTS implements Agent, HasGameOverProcessing {

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
    protected int nodesExpanded;
    protected boolean nodeExpanded;
    protected int deepestNode, allNodeDepths, rollouts;
    protected ExpansionPolicy expansionPolicy;
    protected Agent rolloutPolicy;


    public MCTS(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
        this.rolloutDepth = rolloutDepth;
        this.treeDepthMul = treeDepthMul;
        this.timeLimit = timeLimit;
        this.C = explorationC;
        this.random = new Random();
        expansionPolicy = new SimpleNodeExpansion(logger, random);
    }

    @AgentConstructor("hs-IS")
    public MCTS(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, Agent rollout) {
        this(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        rolloutPolicy = rollout == null ? new RandomEqual(0) : rollout;
    }

    public void setStateGatherer(StateGatherer sg) {
        stateGatherer = sg;
    }

    public void setEndGameProcessor(HasGameOverProcessing egp) {
        endGameProcessor = egp;
    }

    @Override
    public Action doMove(int agentID, GameState state) {
        int movesLeft = state.getMovesLeft();

        if (movesLeft != state.getPlayerCount() + 1) {
            // we are in the endGame, but this is not recorded within state
        } else {
            movesLeft = Integer.MAX_VALUE;
        }

        MCTSNode root = createRoot((agentID - 1 + state.getPlayerCount()) % state.getPlayerCount(), state);
        rollouts = 0;
        nodesExpanded = 0;
        allNodeDepths = 0;
        executeSearch(agentID, root, state, movesLeft);

        if (logger.isInfoEnabled()) {
            for (MCTSNode level1 : root.getChildren()) {
                logger.info(String.format("Action: %s\tVisits: %d\tScore: %.3f", level1.getAction(), level1.visits, level1.score / level1.visits));
                logger.info("rollout {} moves: max: {}, min: {}, avg: {}, N: {} ", level1.getAction(), level1.rolloutMoves.getMax(), level1.rolloutMoves.getMin(), level1.rolloutMoves.getMean(), level1.rolloutMoves.getN());
                logger.info("rollout {} scores: max: {}, min: {}, avg: {}, N: {} ", level1.getAction(), level1.rolloutScores.getMax(), level1.rolloutScores.getMin(), level1.rolloutScores.getMean(), level1.rolloutScores.getN());
            }
        }

        if (logger.isTraceEnabled()) {
            logger.trace("next player's moves considerations: ");
            for (MCTSNode level1 : root.getChildren()) {
                logger.trace("{}'s children", level1.getAction());
                level1.printChildren();
            }
        }

        MCTSNode bestNode = root.getBestNode();
        Action chosenOne = (bestNode != null) ? bestNode.getAction() : new PlayCard(0);

   //     System.out.println(String.format("Turn %d, Player %d takes Action %s, %d real moves Left, %d lives left, %d information tokens, %d cards in deck",
   //             state.getTurnNumber(), agentID, chosenOne.toString(), StateGatherer.movesLeft(state, agentID), state.getLives(), state.getInfomation(), state.getDeck().getCardsLeft()));

        if (logger.isTraceEnabled()) {
            logger.trace("Move Chosen by {} was {}", agentID, chosenOne);
            root.printChildren();
        }

        if (stateGatherer != null) {
            if (stateGatherer instanceof TreeProcessor) {
                ((TreeProcessor) stateGatherer).processTree(root);
            } else {
                stateGatherer.storeData(root, state, agentID);
            }
        }

        Map<String, Double> stats = new HashMap<>();
        stats.put("MAX_TREE_DEPTH", (double) deepestNode);
        stats.put("MEAN_TREE_DEPTH", allNodeDepths / (double) nodesExpanded);
        stats.put("NODES_EXPANDED", (double) nodesExpanded);
        stats.put("ROLLOUTS", (double) rollouts);
        StatsCollator.addStatistics(stats);
        return chosenOne;
    }

    protected void executeSearch(int agentID, MCTSNode root, GameState state, int movesLeft) {
        long finishTime = System.currentTimeMillis() + timeLimit;
        Map<Integer, List<Card>> possibleCards = DeckUtils.bindCard(agentID, state.getHand(agentID), state.getDeck().toList());
        List<Integer> bindOrder = DeckUtils.bindOrder(possibleCards);

        logDebugGameState(state, agentID);

//        for (int round = 0; round < roundLength; round++) {
        while (System.currentTimeMillis() < finishTime && rollouts < timeLimit * 2) {
            rollouts++;
            //find a leaf node
            GameState currentState = state.getCopy();
            Map<Integer, Card> myHandCards = DeckUtils.bindCards(bindOrder, possibleCards);

            Deck deck = currentState.getDeck();
            Hand myHand = currentState.getHand(agentID);
            for (int slot = 0; slot < myHand.getSize(); slot++) {
                Card hand = myHandCards.get(slot);
                myHand.bindCard(slot, hand);
                deck.remove(hand);
            }
            deck.shuffle();

            MCTSNode current = select(root, currentState, movesLeft);

            if (current.getDepth() > deepestNode) deepestNode = current.getDepth();
            allNodeDepths += current.getDepth();
            if (nodeExpanded) nodesExpanded++;

            double score = rollout(currentState, current, movesLeft - current.getDepth());
            current.backup(score, null,null);
            if (calcTree) {
                System.out.println(root.printD3());
            }
        }
    }

    protected MCTSNode expand(MCTSNode parent, GameState state) {
        // we also need to increment the parent eligible visit count at this point
        parent.incrementParentVisitsForAllEligibleActions(state);
        return expansionPolicy.expand(parent, state);
    }

    protected MCTSNode createRoot(int previousAgentID, GameState state) {
        MCTSNode root = expansionPolicy.createRoot(state, previousAgentID, C);
        root.setReferenceState(state.getCopy());
        return root;
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

    protected MCTSNode select(MCTSNode root, GameState state, int maxMoves) {
        int movesLeft = maxMoves;
        MCTSNode current = root;
        int treeDepth = calculateTreeDepthLimit(state);
        nodeExpanded = false;

        while (!state.isGameOver() && current.getDepth() < treeDepth && !nodeExpanded && movesLeft > 0) {
            MCTSNode next;
            movesLeft--;
            if (current.fullyExpanded(state)) {
                next = current.getUCTNode(state, false);
            } else {
                next = expand(current, state);
                nodeExpanded = true;
                //            return next;
            }
            if (next == null) {
                //XXX if all follow on states explored so far are null, we are now a leaf node
                return current;
            }
            current = next;

            int agent = current.getAgentId();

            Action action = current.getAction();
            if (logger.isTraceEnabled()) logger.trace("Selected action " + action + " for player " + agent);
            if (action != null) {
                action.apply(agent, state);
                if (current.getReferenceState() == null)
                    current.setReferenceState(state.getCopy());
            }

        }
        return current;
    }

    protected int calculateTreeDepthLimit(GameState state) {
        return (state.getPlayerCount() * treeDepthMul) + 1;
    }


    protected Action selectActionForRollout(GameState state, int playerID) {
        if (rolloutPolicy == null) {
            throw new AssertionError("No rollout policy specified");
        } else {
            try {
                // we first need to ensure Player's hand is back in deck
                Hand myHand = state.getHand(playerID);
                Deck deck = state.getDeck();
                int cardsAddedToDeck = 0;
                for (int i = 0; i < myHand.getSize(); i++) {
                    if (myHand.getCard(i) != null) {
                        deck.add(myHand.getCard(i));
                        cardsAddedToDeck++;
                    }
                }
                // then choose the action
                Action chosenAction = rolloutPolicy.doMove(playerID, state);
                // then put their hand back
                for (int i = 0; i < cardsAddedToDeck; i++) {
                    deck.getTopCard();
                }

                return chosenAction;
            } catch (IllegalArgumentException ex) {
                logger.error("warning, agent failed to make move: {}", ex);
                return new RandomEqual(0).doMove(playerID, state);
            }
            catch (IllegalStateException ex) {
                logger.error("Problem with Rules in rollout {} for player {} using policy {}", ex, playerID, rolloutPolicy);
                DebugUtils.printState(logger, state);
                return new RandomEqual(0).doMove(playerID, state);
            }
        }
    }


    protected double rollout(GameState state, MCTSNode current, int movesLeft) {

        int playerID = (current.getAgentId() + 1) % state.getPlayerCount();
        // we rollout from current, which records the agent who acted to reach it
        int moves = 0;
        int movesWithEmptyDeck = 0;

        while (!state.isGameOver() && moves < rolloutDepth && moves < movesLeft) {
            if (!state.getDeck().hasCardsLeft()) {
                movesWithEmptyDeck++;
                if (movesWithEmptyDeck > state.getPlayerCount()) {
           //         throw new AssertionError("WTF");
                }
            }
            Action action = selectActionForRollout(state, playerID);
    //        if (action == null) {
    //            moves = rolloutDepth;
    //            continue;
    //        }
            action.apply(playerID, state);
            playerID = (playerID + 1) % state.getPlayerCount();
            moves++;
        }
        logger.trace(String.format("Terminating rollout after %d moves with score of %d", moves, state.getScore()));
        current.backupRollout(moves, state.getScore());
        return state.getScore();
    }

    @Override
    public String toString() {
        return String.format("MCTS(%s)", rolloutPolicy == null ? "rnd" : rolloutPolicy.toString());
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

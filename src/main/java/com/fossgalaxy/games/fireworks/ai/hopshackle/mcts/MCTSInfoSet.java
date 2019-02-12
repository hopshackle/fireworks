package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.determinize.HandDeterminiser;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StatsCollator;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by WebPigeon on 09/08/2016.
 */
public class MCTSInfoSet extends MCTS {

    protected HandDeterminiser handDeterminiser;
    protected boolean MRIS = false;

    /**
     * Create a default MCTS implementation.
     * <p>
     * This creates an MCTS agent that has a default roll-out length of 50_000 iterations, a depth of 18 and a tree
     * multiplier of 1.
     */
    public MCTSInfoSet() {
        this(MCTSNode.DEFAULT_EXP_CONST, DEFAULT_ROLLOUT_DEPTH, DEFAULT_TREE_DEPTH_MUL, DEFAULT_TIME_LIMIT);
    }

    public MCTSInfoSet(double expConst) {
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

    public MCTSInfoSet(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
    }

    @AgentConstructor("hs-RIS")
    public MCTSInfoSet(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        rolloutPolicy = rollout == null ? new RandomEqual(0) : rollout;
    }

    @Override
    protected void executeSearch(int agentID, MCTSNode root, GameState state, int movesLeft) {
        long finishTime = System.currentTimeMillis() + timeLimit;

//        for (int round = 0; round < roundLength; round++) {
        while (System.currentTimeMillis() < finishTime && rollouts < timeLimit * 2) {
            //find a leaf node
            rollouts++;
            GameState currentState = state.getCopy();

            handDeterminiser = new HandDeterminiser(currentState, agentID, MRIS);
            long oldShiftsPlay = HandDeterminiser.getUniverseShiftPlay();
            long oldShiftsDiscard = HandDeterminiser.getUniverseShiftDiscard();
            MCTSNode current = select(root, currentState, movesLeft);
            // reset to known hand values before rollout
            Map<String, Double> shiftData = new HashMap<>();
            if (HandDeterminiser.getUniverseShiftDiscard() == oldShiftsDiscard && HandDeterminiser.getUniverseShiftPlay() == oldShiftsPlay) {
                shiftData.put("CONSISTENT_ROLLOUT", 1.0);
                shiftData.put("CONSISTENT_PLAY", 1.0);
                shiftData.put("CONSISTENT_DISCARD", 1.0);
            } else {
                shiftData.put("CONSISTENT_ROLLOUT", 0.0);
                if (HandDeterminiser.getUniverseShiftPlay() == oldShiftsPlay) {
                    shiftData.put("CONSISTENT_PLAY", 1.0);
                } else {
                    shiftData.put("CONSISTENT_PLAY", 0.0);
                }
                if (HandDeterminiser.getUniverseShiftDiscard() == oldShiftsDiscard) {
                    shiftData.put("CONSISTENT_DISCARD", 1.0);
                } else {
                    shiftData.put("CONSISTENT_DISCARD", 0.0);
                }
            }
            StatsCollator.addStatistics(shiftData);
            handDeterminiser.reset((current.getAgentId() + 1) % currentState.getPlayerCount(), currentState);

            if (current.getDepth() > deepestNode) deepestNode = current.getDepth();
            allNodeDepths += current.getDepth();
            if (nodeExpanded) nodesExpanded++;

            double score = rollout(currentState, current, movesLeft - current.getDepth());
            if (logger.isDebugEnabled()) logger.debug(String.format("Backing up a final score of %.2f", score));
            current.backup(score, null, null);
            if (calcTree) {
                System.out.println(root.printD3());
            }
        }
    }

    @Override
    protected MCTSNode select(MCTSNode root, GameState state, int movesLeft) {
        MCTSNode current = root;
        int treeDepth = calculateTreeDepthLimit(state);
        nodeExpanded = false;

        while (!state.isGameOver() && current.getDepth() < treeDepth && !nodeExpanded && movesLeft > 0) {
            MCTSNode next;
            movesLeft--;
            // determinise hand before decision is made
            int agentAboutToAct = (current.getAgentId() + 1) % state.getPlayerCount();
            handDeterminiser.determiniseHandFor(agentAboutToAct, state);

            // put active hand into deck for decision making
            Hand myHand = state.getHand(agentAboutToAct);
            Deck deck = state.getDeck();
            Card[] hand = new Card[myHand.getSize()];
            int cardsAdded = 0;
            for (int i = 0; i < myHand.getSize(); i++) {
                if (myHand.getCard(i) != null) {
                    deck.add(myHand.getCard(i));
                    hand[i] = myHand.getCard(i);
                    myHand.bindCard(i, null);
                    cardsAdded++;
                }
            }

            if (rollouts == 0 && logger.isDebugEnabled()) {
                // we do this here to be within the hand/deck fiddle
                String logMessage = "All legal moves from root: " +
                        ((MCTSRuleNode) root).getAllLegalMoves(state, (root.agentId + 1) % state.getPlayerCount())
                                .stream()
                                .map(Action::toString)
                                .collect(Collectors.joining("\t"));
                logger.debug(logMessage);
            }

            if (current.fullyExpanded(state)) {
                next = current.getUCTNode(state, false);
            } else {
                next = expand(current, state);
                nodeExpanded = true;
                //            return next;
            }

            for (int i = 0; i < hand.length; i++) {
                if (hand[i] != null) {
                    myHand.bindCard(i, hand[i]);
                }
            }

            for (int i= 0; i < cardsAdded; i++) deck.getTopCard();

            if (next == null) {
                //XXX if all follow on states explored so far are null, we are now a leaf node
                return current;
            }

            if (next.getAgentId() != agentAboutToAct) {
                throw new AssertionError("WTF");
            }

            current = next;

            int agent = current.getAgentId(); // this is the acting agent

            // we then apply the action to state, and re-determinise the hand for the next agent
            Action action = current.getAction();
            if (logger.isDebugEnabled()) logger.debug("MCTSIS: Selected action " + action + " for player " + agent);
            if (action != null) {
                state.tick();
                handDeterminiser.recordAction(action, agent, state);
                List<GameEvent> events = action.apply(agent, state);
                events.forEach(state::addEvent);
                // we then set the reference state on the node, once the action has actually been executed
                // this is a fully determinised state
                if (current.getReferenceState() == null)
                    current.setReferenceState(state.getCopy());
            }

        }
        return current;
    }


    @Override
    public String toString() {
        return String.format("MCTSInfoSet(%s)", rolloutPolicy == null ? "NONE" : rolloutPolicy.toString());
    }


}

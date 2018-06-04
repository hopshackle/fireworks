package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.mcts.IterationObject;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import com.fossgalaxy.games.fireworks.utils.DebugUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by WebPigeon on 09/08/2016.
 */
public class MCTSInfoSet extends MCTS {

    protected HandDeterminiser handDeterminiser;

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
    @AgentConstructor("mctsIS")
    public MCTSInfoSet(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
    }

    @Override
    public Action doMove(int agentID, GameState state) {
        long finishTime = System.currentTimeMillis() + timeLimit;

        MCTSNode root = createNode(null, (agentID - 1 + state.getPlayerCount()) % state.getPlayerCount(), null, state);

        logDebugGameState(state, agentID);

//        for (int round = 0; round < roundLength; round++) {
        while (System.currentTimeMillis() < finishTime) {
            //find a leaf node
            GameState currentState = state.getCopy();
            IterationObject iterationObject = new IterationObject(agentID);

            handDeterminiser = new HandDeterminiser(currentState, agentID);

            MCTSNode current = select(root, currentState, iterationObject);
            // reset to known hand values before rollout
            handDeterminiser.reset((current.getAgent() + 1) % currentState.getPlayerCount(), currentState);
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

        if (logger.isTraceEnabled()) {
            logger.trace("next player's moves considerations: ");
            for (MCTSNode level1 : root.getChildren()) {
                logger.trace("{}'s children", level1.getAction());
                level1.printChildren();
            }
        }

        Action chosenOne = root.getBestNode().getAction();
        if (logger.isTraceEnabled()) {
            logger.trace("Move Chosen by {} was {}", agentID, chosenOne);
            root.printChildren();
        }
        /*
        if (this instanceof MCTSInfoSetPolicy) {
            Action rolloutAction = ((MCTSInfoSetPolicy) this).selectActionForRollout(state, agentID);
            System.out.println(String.format("Player %d: MCTS choice is %s, with rollout %s", agentID, chosenOne.toString(), rolloutAction.toString()));
        }
        */

        if (stateGatherer != null) {
            stateGatherer.storeData(root, state, agentID);
        }

        return chosenOne;
    }

    protected MCTSNode select(MCTSNode root, GameState state, IterationObject iterationObject) {
        MCTSNode current = root;
        int treeDepth = calculateTreeDepthLimit(state);
        boolean expandedNode = false;

        if (logger.isDebugEnabled()) {
            String logMessage = "All legal moves from root: " +
                    ((MCTSRuleNode) root).getAllLegalMoves(state, (root.agentId + 1) % state.getPlayerCount())
                            .stream()
                            .map(Action::toString)
                            .collect(Collectors.joining("\t"));
            logger.debug(logMessage);
        }


        while (!state.isGameOver() && current.getDepth() < treeDepth && !expandedNode) {
            MCTSNode next;
            // determinise hand before decision is made
            int agentAboutToAct = (current.getAgent() + 1) % state.getPlayerCount();
            handDeterminiser.determiniseHandFor(agentAboutToAct, state);
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

            if (next.getAgent() != agentAboutToAct) {
                throw new AssertionError("WTF");
            }

            current = next;

            int agent = current.getAgent(); // this is the acting agent

            int lives = state.getLives();
            int score = state.getScore();

            // we then apply the action to state, and re-determinise the hand for the next agent
            Action action = current.getAction();
            if (logger.isDebugEnabled()) logger.debug("MCTSIS: Selected action " + action + " for player " + agent);
            if (action != null) {
                List<GameEvent> events = action.apply(agent, state);
                events.forEach(state::addEvent);
                state.tick();
                handDeterminiser.recordAction(action);
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


    @Override
    public String toString() {
        return "MCTSInfoSet";
    }


}

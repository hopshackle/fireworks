package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.mcts.IterationObject;
import com.fossgalaxy.games.fireworks.ai.osawa.rules.OsawaDiscard;
import com.fossgalaxy.games.fireworks.ai.osawa.rules.TellPlayableCardOuter;
import com.fossgalaxy.games.fireworks.ai.rule.*;
import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardProbablyUselessCard;
import com.fossgalaxy.games.fireworks.ai.rule.random.PlayProbablySafeCard;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by WebPigeon on 09/08/2016.
 */
public class MCTSRuleInfoSet extends MCTSInfoSet {

    public static final List<Rule> allRules = new ArrayList<>();

    static {
        allRules.add(new OsawaDiscard());
        allRules.add(new DiscardHighest());
        allRules.add(new TellAboutOnes());
        allRules.add(new TellMostInformation(true));
        allRules.add(new TellAnyoneAboutUsefulCard());
        allRules.add(new TellDispensable());
        allRules.add(new TellPlayableCardOuter());
        allRules.add(new PlaySafeCard());
        allRules.add(new PlayProbablySafeCard(0.75));
        allRules.add(new DiscardProbablyUselessCard(0.75));
        allRules.add(new DiscardOldestFirst());
    }

    /**
     * Create a default MCTS implementation.
     * <p>
     * This creates an MCTS agent that has a default roll-out length of 50_000 iterations, a depth of 18 and a tree
     * multiplier of 1.
     */
    public MCTSRuleInfoSet() {
        this(MCTSNode.DEFAULT_EXP_CONST, DEFAULT_ROLLOUT_DEPTH, DEFAULT_TREE_DEPTH_MUL, DEFAULT_TIME_LIMIT);
    }

    public MCTSRuleInfoSet(double expConst) {
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
    @AgentConstructor("mctsRuleIS")
    public MCTSRuleInfoSet(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
    }


    @Override
    public Action doMove(int agentID, GameState state) {
        long finishTime = System.currentTimeMillis() + timeLimit;

        MCTSRuleNode root = (MCTSRuleNode) createNode(null, (agentID - 1 + state.getPlayerCount()) % state.getPlayerCount(), null, state);

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

        if (stateGatherer != null) {
            List<Rule> rulesTriggered = root.getRulesForChild(root.getBestNode(), state, agentID);
            ((StateGathererWithTarget) stateGatherer).storeData(state, agentID, rulesTriggered);
        }

        /*
        if (this instanceof MCTSInfoSetPolicy) {
            Action rolloutAction = ((MCTSInfoSetPolicy) this).selectActionForRollout(state, agentID);
            System.out.println(String.format("Player %d: MCTS choice is %s, with rollout %s", agentID, chosenOne.toString(), rolloutAction.toString()));
        }
        */
        return chosenOne;
    }

    @Override
    public void gatherStateData(boolean flag) {
        if (flag) {
            stateGatherer = new StateGathererWithTarget();
        } else {
            stateGatherer = null;
        }
    }

    @Override
    protected MCTSNode createNode(MCTSNode parent, int previousAgentID, Action moveTo, GameState state) {
        MCTSRuleNode root = new MCTSRuleNode(
                (MCTSRuleNode) parent,
                previousAgentID,
                moveTo, C,
                allRules);

        return root;
    }

    @Override
    public String toString() {
        return "MCTSRuleInfoSet";
    }

}

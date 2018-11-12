package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.RuleExpansionPolicy;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;

public class CRIS_MCTSRule extends CRIS_MCTS {

    public CRIS_MCTSRule() {
        this(MCTSNode.DEFAULT_EXP_CONST, DEFAULT_ROLLOUT_DEPTH, DEFAULT_TREE_DEPTH_MUL, DEFAULT_TIME_LIMIT);
    }

    public CRIS_MCTSRule(double expConst) {
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
    @AgentConstructor("CRIS-MCTSRule")
    public CRIS_MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        expansionPolicy = new RuleExpansionPolicy(logger, random, MCTSRuleInfoSet.allRules);
    }


    @Override
    public String toString() {
        return "CRIS-MCTSRule";
    }

}

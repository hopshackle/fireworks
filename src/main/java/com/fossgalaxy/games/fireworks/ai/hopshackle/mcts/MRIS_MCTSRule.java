package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;


public class MRIS_MCTSRule extends MCTSRuleInfoSet {

    public MRIS_MCTSRule() {
        this(MCTSNode.DEFAULT_EXP_CONST, DEFAULT_ROLLOUT_DEPTH, DEFAULT_TREE_DEPTH_MUL, DEFAULT_TIME_LIMIT);
    }

    public MRIS_MCTSRule(double expConst) {
        this(expConst, DEFAULT_ROLLOUT_DEPTH, DEFAULT_TREE_DEPTH_MUL, DEFAULT_TIME_LIMIT);
    }

    public MRIS_MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        MRIS = true;
    }

    @AgentConstructor("hs-MRISRule")
    public MRIS_MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit, rollout);
        MRIS = true;
    }
}

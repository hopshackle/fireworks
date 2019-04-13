package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;


public class MRIS_MCTSRule extends MCTSRuleInfoSet {

    @AgentConstructor("hs-MRISRule")
    public MRIS_MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, String rules, String conventions,  Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit, rules, conventions, rollout);
        MRIS = true;
    }
}

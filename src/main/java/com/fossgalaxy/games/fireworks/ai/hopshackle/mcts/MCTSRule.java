package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.RuleExpansionPolicy;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;

public class MCTSRule extends MCTS {

    /**
     * Create an MCTS agent which has the parameters.
     *
     * @param explorationC
     * @param rolloutDepth
     * @param treeDepthMul
     * @param timeLimit    in ms
     */
    @AgentConstructor("hs-mctsRule")
    public MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        expansionPolicy = new RuleExpansionPolicy(logger, random, MCTSRuleInfoSet.allRules);
    }


    @Override
    public String toString() {
        return "MCTSRule";
    }
}

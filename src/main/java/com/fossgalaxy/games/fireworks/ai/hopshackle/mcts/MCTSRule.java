package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.RuleExpansionPolicy;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.RuleFullExpansion;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;

import java.util.Optional;

public class MCTSRule extends MCTS {

    /**
     * Create an MCTS agent which has the parameters.
     *
     * @param explorationC
     * @param rolloutDepth
     * @param treeDepthMul
     * @param timeLimit    in ms
     */
    public MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        expansionPolicy = new RuleExpansionPolicy(logger, random, MCTSRuleInfoSet.allRules);
    }

    @AgentConstructor("hs-ISRule")
    public MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, Agent rollout) {
        this(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        rolloutPolicy = rollout == null ? new RandomAgent() : rollout;
        if (rollout instanceof EvalFnAgent)
            expansionPolicy = new RuleFullExpansion(logger, random, MCTSRuleInfoSet.allRules, Optional.empty(), Optional.of((EvalFnAgent) rollout));
    }

    @Override
    public String toString() {
        return String.format("MCTSRule(%s)", rolloutPolicy == null ? "NONE" : rolloutPolicy.toString());
    }
}

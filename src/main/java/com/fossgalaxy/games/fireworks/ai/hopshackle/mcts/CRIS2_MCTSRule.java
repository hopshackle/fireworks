package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.RuleExpansionPolicy;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.RuleFullExpansion;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;

import java.util.Optional;

public class CRIS2_MCTSRule extends CRIS2_MCTS {

    public CRIS2_MCTSRule() {
        this(MCTSNode.DEFAULT_EXP_CONST, DEFAULT_ROLLOUT_DEPTH, DEFAULT_TREE_DEPTH_MUL, DEFAULT_TIME_LIMIT);
    }

    public CRIS2_MCTSRule(double expConst) {
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
    public CRIS2_MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        expansionPolicy = new RuleExpansionPolicy(logger, random, MCTSRuleInfoSet.allRules);
    }

    @AgentConstructor("hs-CRIS2Rule")
    public CRIS2_MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, Agent rollout) {
        this(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        rolloutPolicy = rollout == null ? new RandomEqual(0) : rollout;
        // TODO: Parameterise this more elegantly in future
        if (rollout instanceof EvalFnAgent)
            expansionPolicy = new RuleFullExpansion(logger, random, MCTSRuleInfoSet.allRules, Optional.empty(), Optional.of((EvalFnAgent) rollout));
    }


    @Override
    public String toString() {
        return String.format("CRIS-MCTSRule(%s)", rolloutPolicy == null ? "NONE" : rolloutPolicy.toString());
    }

}
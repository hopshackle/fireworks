package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;

import java.util.Optional;

public class MCTSRuleInfoSetFullExpansion extends MCTSRuleInfoSet {

    @AgentConstructor("mctsRuleFE")
    public MCTSRuleInfoSetFullExpansion(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, Agent agent) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        expansionPolicy = new RuleFullExpansion(logger, random, allRules, Optional.empty(), Optional.of((EvalFnAgent)agent));
    }

    @Override
    protected double rollout(GameState state, MCTSNode current, int movesLeft) {
        return current.score / current.visits;
    }

    @Override
    public String toString() {
        return "MCTSRuleInfoSetFullExpansion";
    }
}

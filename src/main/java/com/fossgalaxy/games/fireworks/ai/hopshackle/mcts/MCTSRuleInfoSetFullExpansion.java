package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.RuleFullExpansion;
import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;

import java.util.Optional;

public class MCTSRuleInfoSetFullExpansion extends MCTSRuleInfoSet {

    @AgentConstructor("hs-RISRuleFE")
    public MCTSRuleInfoSetFullExpansion(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, String rules, Agent agent) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit, rules, agent);
        expansionPolicy = new RuleFullExpansion(logger, random, allRules, Optional.of((EvalFnAgent)agent));
    }

    @Override
    protected double rollout(GameState state, MCTSNode current, int movesLeft) {
        if (state.isGameOver() || movesLeft <= 0) return state.getScore();
        return current.score / current.visits;
    }

    @Override
    public String toString() {
        return "MCTSRuleInfoSetFullExpansion";
    }
}

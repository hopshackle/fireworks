package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.RuleFullExpansionOpponentModel;
import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.*;

import java.util.*;

public class MCTSOppModelFE extends MCTSOppModelRollout {

    @AgentConstructor("mctsOpponentModelFE")
    public MCTSOppModelFE(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, String opponentModelLoc, String valueFnLoc, String rules) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit, opponentModelLoc, rules);
        EvalFnAgent evalFn = new EvalFnAgent(valueFnLoc, 0.0, allRules, true);
        expansionPolicy = new RuleFullExpansionOpponentModel(logger, random, allRules, Optional.of(evalFn));
    }


    @Override
    protected double rollout(GameState state, MCTSNode current, int movesLeft) {
        if (state.isGameOver() || movesLeft <= 0) return state.getScore();
        return current.score / current.visits;
    }

    @Override
    public String toString() {
        return "MCTSOppModelFullExpansion";
    }
}

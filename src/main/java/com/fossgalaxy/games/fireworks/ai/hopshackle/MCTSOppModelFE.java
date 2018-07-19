package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.mcts.IterationObject;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;
import com.fossgalaxy.games.fireworks.state.events.*;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.games.fireworks.utils.DebugUtils;

import java.io.FileInputStream;
import java.util.*;
import java.util.stream.Collectors;

public class MCTSOppModelFE extends MCTSOppModelRollout {

    @AgentConstructor("mctsOpponentModelFE")
    public MCTSOppModelFE(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, String opponentModelLoc, String valueFnLoc) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit, opponentModelLoc);
        EvalFnAgent evalFn = new EvalFnAgent(valueFnLoc, 0.0, true);
        expansionPolicy = new RuleFullExpansionOpponentModel(logger, random, allRules, Optional.empty(), Optional.of(evalFn));
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

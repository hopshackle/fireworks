package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;

public class MRIS_MCTS extends MCTSInfoSet {

    @AgentConstructor("hs-MRIS")
    public MRIS_MCTS(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, String conventions, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit, conventions, rollout);
        MRIS = true;
    }
}

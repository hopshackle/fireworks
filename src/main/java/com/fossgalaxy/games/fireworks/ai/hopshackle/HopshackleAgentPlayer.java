package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.*;

/*
This just adds an implementation of onGameOver(), which will update the underlying agent
 */
public class HopshackleAgentPlayer extends AgentPlayer {

    public HopshackleAgentPlayer(String name, Agent policy) {
        super(name, policy);
    }

    @Override
    public void onGameOver() {
        if (policy instanceof HasGameOverProcessing)
            ((HasGameOverProcessing) policy).onGameOver(state.getScore());
    }
}

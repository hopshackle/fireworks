package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.HasGameOverProcessing;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.NoLifeState;

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

    public GameState getGameState() {
        return this.state;
    }
/*
    @Override
    public void setID(int id, int nPlayers, String[] names) {
        super.setID(id, nPlayers, names);
        this.state = new NoLifeState(nPlayers);
    }
*/

}

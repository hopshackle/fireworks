package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

public class MonteCarloOppNN implements Agent {

    MCTSOppModelFE actualBrain;

    public MonteCarloOppNN() {
        actualBrain = new MCTSOppModelFE(0.03, 100, 3, 30,
                "RESOpponentModel.params", "RESPlayers_5.params");
    }

    @Override
    public Action doMove(int i, GameState gameState) {
        return actualBrain.doMove(i, gameState);
    }
}

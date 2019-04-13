package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

public class MonteCarloOppNN implements Agent {

    MCTSOppModelFE actualBrain;

    public MonteCarloOppNN() {
        actualBrain = new MCTSOppModelFE(0.03, 100, 3, 30,
                "RESOpponentModel.params", "RESPlayers_5.params", "1|2|3|4|6|7|8|9|10|11|12|15", "Y");
    }

    @Override
    public Action doMove(int i, GameState gameState) {
        return actualBrain.doMove(i, gameState);
    }
}

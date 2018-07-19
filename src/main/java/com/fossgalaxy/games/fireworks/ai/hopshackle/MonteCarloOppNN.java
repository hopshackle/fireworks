package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

public class MonteCarloOppNN implements Agent {

    MCTSOppModelFE actualBrain;

    public MonteCarloOppNN() {
        if (actualBrain == null) {
            try {
                for (int players = 2; players <= 5; players++) {
                    ClassLoader classLoader = getClass().getClassLoader();
                    HopshackleNN brain = HopshackleNN.createFromStream(classLoader.getResourceAsStream("Players_" + players + ".params"));
                    actualBrain = new MCTSOppModelFE(0.03, 100, 3, 30,
                            "RESOpponentModel.params", "RESPlayer_5.params");
                }
            } catch (Exception e) {
                System.out.println("Error when reading in Model " + e.toString());
                e.printStackTrace();
            }
        }
    }

    @Override
    public Action doMove(int i, GameState gameState) {
        return actualBrain.doMove(i, gameState);
    }
}

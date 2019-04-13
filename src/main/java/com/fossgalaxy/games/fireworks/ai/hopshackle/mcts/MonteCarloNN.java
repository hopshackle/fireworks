package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.HopshackleNN;
import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;

public class MonteCarloNN implements Agent {

    static EvalFnAgent[] valueAgent = new EvalFnAgent[5];
    static boolean initialised = false;
    static String rulesToUse ="1|2|3|4|6|7|8|9|10|11|12|15";
    static String conventionsToUse = "Y";

    static MCTSRuleInfoSetFullExpansion[] actualBrains = new MCTSRuleInfoSetFullExpansion[5];

    public MonteCarloNN() {
        if (!initialised) {
            try {
                for (int players = 2; players <= 5; players++) {
                    ClassLoader classLoader = getClass().getClassLoader();
                    HopshackleNN brain = HopshackleNN.createFromStream(classLoader.getResourceAsStream("Players_" + players + ".params"));
                    valueAgent[players - 1] = new EvalFnAgent(brain, 0.0);
                    actualBrains[players - 1] = new MCTSRuleInfoSetFullExpansion(0.03, 100, 3, 30,
                            rulesToUse, conventionsToUse, valueAgent[players - 1]);
                }
            } catch (Exception e) {
                System.out.println("Error when reading in Model " + e.toString());
                e.printStackTrace();
            }
            initialised = true;
        }
    }

    @Override
    public Action doMove(int i, GameState gameState) {
        return actualBrains[gameState.getPlayerCount() - 1].doMove(i, gameState);
    }
}

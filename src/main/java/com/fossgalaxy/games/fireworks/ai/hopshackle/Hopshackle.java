package com.fossgalaxy.games.fireworks.ai.hopshackle;
import com.fossgalaxy.games.fireworks.ai.*;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;

public class Hopshackle implements Agent {

    static EvalFnAgent valueAgent;

    MCTSRuleInfoSetFullExpansion actualBrain;

    public Hopshackle() {
        if (valueAgent == null) {
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                HopshackleNN brain = HopshackleNN.createFromStream(classLoader.getResourceAsStream("Tree_rnd_02.params"));
                valueAgent = new EvalFnAgent(brain);
            } catch (Exception e) {
                System.out.println("Error when reading in Model " + e.toString());
                e.printStackTrace();
            }
        }
        actualBrain = new MCTSRuleInfoSetFullExpansion(0.03, 100, 3, 30, valueAgent);
    }


    @Override
    public Action doMove(int i, GameState gameState) {
        return actualBrain.doMove(i, gameState);
    }
}

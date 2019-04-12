package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.*;

import java.util.*;

public class RandomEqual implements Agent {

    private Random rand = new Random(4);

    @AgentConstructor("hs-random")
    public RandomEqual(int bit) {
        // we have to have a constructor with an argument in order for the AgentConstructor code in the
        // Hanabi framework to function properly
    }

    @Override
    public Action doMove(int playerID, GameState state) {
        Collection<Action> legalActions = Utils.generateActions(playerID, state);

        if (legalActions.isEmpty()) {
            for (int i = 0; i < state.getHandSize(); i++) {
                if (state.getHand(playerID).hasCard(i)) {
                    return new DiscardCard(i);
                }
            }
        }
        List<Action> listAction = new ArrayList<>(legalActions);
        return listAction.get(rand.nextInt(listAction.size()));
    }
}

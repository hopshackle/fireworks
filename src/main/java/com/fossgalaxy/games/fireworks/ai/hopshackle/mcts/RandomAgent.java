package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;

import java.util.*;

public class RandomAgent implements Agent {

    @AgentConstructor("hs-random")
    public RandomAgent() {

    }

    @Override
    public Action doMove(int playerID, GameState state) {
        Collection<Action> legalActions = Utils.generateActions(playerID, state);

        List<Action> listAction = new ArrayList<>(legalActions);
        Collections.shuffle(listAction);
        if (listAction.isEmpty()) {
            for (int i = 0; i < state.getHandSize(); i++) {
                if (state.getHand(playerID).hasCard(i)) {
                    return new DiscardCard(i);
                }
            }
        }

        return listAction.get(0);
    }
}

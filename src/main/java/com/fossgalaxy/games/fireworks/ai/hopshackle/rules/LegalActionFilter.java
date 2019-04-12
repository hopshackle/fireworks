package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.*;

import java.util.function.Predicate;

public class LegalActionFilter {

    public static Predicate<Action> isLegal(int playerID, GameState state) {
        return (Action p) -> {
            // this section should use Action.isLegal(). But that is broken for Play and Discard
            // as it uses hand.getCard() != null, which will always be true for the acting player
            // when we use the state provided by GameRunsner
            if (p instanceof PlayCard) {
                int slot = ((PlayCard) p).slot;
                return state.getHand(playerID).hasCard(slot);
            } else if (p instanceof DiscardCard) {
                int slot = ((DiscardCard) p).slot;
                return state.getHand(playerID).hasCard(slot) && state.getInfomation() != state.getStartingInfomation();
            } else {
                return p.isLegal(playerID, state);
            }
        };
    }
}

package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractRule;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;

import java.util.List;
import java.util.Map;

/**
 * Created by piers on 08/11/16.
 */
public class PlaySafestCardIfNoDiscardPossible extends PlayProbablySafeCard {

    public PlaySafestCardIfNoDiscardPossible(Conventions conventions) {
        super(conventions, 0.0);
    }

    @Override
    public boolean canFire(int playerID, GameState state) {
        return state.getInfomation() == state.getStartingInfomation();
    }

    @Override
    public Action execute(int playerID, GameState state) {
        if (canFire(playerID, state))
            return super.execute(playerID, state);
        return null;
    }

    @Override
    public String toString() {
        return "If No Discard " + super.toString();
    }

}

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
public class PlayDefinitelySafeCard extends PlayProbablySafeCard {

    public PlayDefinitelySafeCard(Conventions conventions) {
        super(conventions, 1.0);
    }


}

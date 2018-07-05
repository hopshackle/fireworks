package com.fossgalaxy.games.fireworks.ai.hopshackle;

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
public class PlayBestCardIfTwoPlayerAndCannotDiscard extends AbstractRule {


    @Override
    public boolean canFire(int playerID, GameState state) {
        return (state.getPlayerCount() == 2 && state.getStartingInfomation() == state.getInfomation());
    }
    @Override
    public Action execute(int playerID, GameState state) {
        if (state.getPlayerCount() != 2 || state.getStartingInfomation() != state.getInfomation()) return null;

        Map<Integer, List<Card>> possibleCards = ConventionUtils.bindBlindCardWithConventions(playerID, state.getHand(playerID), state.getDeck().toList(), state);

        double bestSoFar = 0.0;
        int bestSlot = -1;
        for (Map.Entry<Integer, List<Card>> entry : possibleCards.entrySet()) {
            double probability = DeckUtils.getProbablity(entry.getValue(), x -> isPlayable(x, state));
            if (probability >= bestSoFar) {
                bestSlot = entry.getKey();
                bestSoFar = probability;
            }
        }
        if (bestSlot == -1) return null;
        return new PlayCard(bestSlot);
    }

    public boolean isPlayable(Card card, GameState state) {
        return state.getTableValue(card.colour) + 1 == card.value;
    }

}

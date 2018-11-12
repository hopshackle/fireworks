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
public class PlayProbablySafeLateGameCard extends AbstractRule {

    private final double pThreshold;
    private final int deckThreshold;

    public PlayProbablySafeLateGameCard() {
        this.pThreshold = 0.95;
        this.deckThreshold = 4;
    }

    public PlayProbablySafeLateGameCard(double threshold, int deck) {
        this.pThreshold = threshold;
        this.deckThreshold = deck;
    }

    @Override
    public Action execute(int playerID, GameState state) {
        if (state.getDeck().getCardsLeft() > deckThreshold) return null;
        Map<Integer, List<Card>> possibleCards = ConventionUtils.bindBlindCardWithConventions(playerID, state.getHand(playerID), state.getDeck().toList(), state);

       double bestSoFar = pThreshold;
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

    @Override
    public String toString() {
        return super.toString() + " : Threshold: " + pThreshold + " / " + deckThreshold;
    }

}

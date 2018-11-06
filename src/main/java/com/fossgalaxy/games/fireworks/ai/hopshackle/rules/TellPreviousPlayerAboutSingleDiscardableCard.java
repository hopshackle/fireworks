package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

/**
 * Tell the next player only about a single card in their hand that is currently useful
 */
public class TellPreviousPlayerAboutSingleDiscardableCard extends AbstractTellRule {

    @Override
    public Action execute(int playerID, GameState state) {

        if (state.getPlayerCount() == 2) return null;
        int previousPlayer = (playerID - 1 + state.getPlayerCount()) % state.getPlayerCount();
        Hand hand = state.getHand(previousPlayer);

        for (int slot = 0; slot < state.getHandSize(); slot++) {

            Card card = hand.getCard(slot);
            if (card == null) {
                continue;
            }

            int currTable = state.getTableValue(card.colour);
            if (card.value > currTable) {
                continue;
            }

            int numberOfCardsCluedByColour = 1;
            int numberOfCardsCluedByValue = 1;
            for (int j = 0; j < state.getHandSize(); j++) {
                if (j == slot || !hand.hasCard(j)) continue;
                Card otherCard = hand.getCard(j);
                if (otherCard.value == card.value) numberOfCardsCluedByValue++;
                if (otherCard.colour == card.colour) numberOfCardsCluedByColour++;
            }

            if (numberOfCardsCluedByValue > 1) {
                if (numberOfCardsCluedByColour > 1) {
                    continue;
                    // try next card
                } else {
                    return new TellColour(previousPlayer, card.colour);
                }
            } else {
                return new TellValue(previousPlayer, card.value);
            }
        }

        return null;
    }

    @Override
    public boolean canFire(int playerID, GameState state) {
        return (state.getPlayerCount() > 2 && super.canFire(playerID, state));
    }
}

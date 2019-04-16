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
public class TellAboutSingleUsefulCard extends AbstractTellRule {

    private Conventions conv;

    public TellAboutSingleUsefulCard(Conventions conv) {
        this.conv = conv;
    }

    @Override
    public Action execute(int playerID, GameState state) {

        for (int i = 0; i < state.getPlayerCount(); i++) {
            int nextPlayer = (playerID + i) % state.getPlayerCount();
            Hand hand = state.getHand(nextPlayer);

            for (int slot = 0; slot < state.getHandSize(); slot++) {

                Card card = hand.getCard(slot);
                if (card == null) {
                    continue;
                }

                int currTable = state.getTableValue(card.colour);
                if (card.value != currTable + 1) {
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
                        Action proposal = new TellColour(nextPlayer, card.colour);
                        if (!ConventionUtils.isAnInvalidConventionalTell(proposal, state, playerID, conv))
                            return proposal;
                    }
                } else {
                    Action proposal = new TellValue(nextPlayer, card.value);
                    if (!ConventionUtils.isAnInvalidConventionalTell(proposal, state, playerID, conv))
                        return proposal;
                }
            }
        }

        return null;
    }

}

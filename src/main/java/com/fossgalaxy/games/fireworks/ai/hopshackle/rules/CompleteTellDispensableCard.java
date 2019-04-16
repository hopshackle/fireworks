package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

/**
 * Created by piers on 25/04/17.
 * <p>
 * Tells useful card prioritising the first instance that we can
 * finish telling something, otherwise first instance that we can
 * tell something
 */
public class CompleteTellDispensableCard extends AbstractTellRule {

    private Conventions conv;

    public CompleteTellDispensableCard(Conventions conventions) {
        conv = conventions;
    }

    @Override
    public Action execute(int playerID, GameState state) {


        for (int i = 0; i < state.getPlayerCount(); i++) {
            int nextPlayer = (playerID + i) % state.getPlayerCount();
            Hand hand = state.getHand(nextPlayer);

            //gard against trying to tell ourselves things
            if (nextPlayer == playerID) {
                continue;
            }

            for (int slot = 0; slot < state.getHandSize(); slot++) {

                Card card = hand.getCard(slot);
                if (card == null) {
                    continue;
                }

                int currTable = state.getTableValue(card.colour);
                if (card.value > currTable) {
                    continue;
                }

                // Can we uniquely identify the card?
                if (hand.getKnownValue(slot) == null ^ hand.getKnownColour(slot) == null) {
                    if (hand.getKnownValue(slot) == null) {
                        Action proposal = new TellValue(nextPlayer, card.value);
                        if (!ConventionUtils.isAnInvalidConventionalTell(proposal, state, playerID, conv))
                            return proposal;
                    }

                    if (hand.getKnownColour(slot) == null) {
                        Action proposal = new TellColour(nextPlayer, card.colour);
                        if (!ConventionUtils.isAnInvalidConventionalTell(proposal, state, playerID, conv))
                            return proposal;
                    }
                }

            }
        }
        return null;
    }
}

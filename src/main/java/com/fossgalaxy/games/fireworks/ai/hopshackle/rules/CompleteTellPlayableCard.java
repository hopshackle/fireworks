package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;

public class CompleteTellPlayableCard extends AbstractTellRule {

    private Conventions conv;

    public CompleteTellPlayableCard(Conventions conventions) {
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
                if (card.value != currTable + 1) {
                    continue;
                }

                // Can we uniquely identify the card?
                if(hand.getKnownValue(slot) == null ^ hand.getKnownColour(slot) == null){
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

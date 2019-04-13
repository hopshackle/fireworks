package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Tell any other player about a card in their hand if it is useful in this situation.
 */
public class TellAnyoneAboutPlayableCard extends AbstractTellRule {

    private Conventions conv;

    public TellAnyoneAboutPlayableCard(Conventions conventions) {
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

                for (Action a : ConventionUtils.tellMissing(hand, nextPlayer, slot)) {
                    if (!ConventionUtils.isAConventionalTell(a, state, playerID, conv))
                        return a;
                }
            }
        }
        return null;
    }
}

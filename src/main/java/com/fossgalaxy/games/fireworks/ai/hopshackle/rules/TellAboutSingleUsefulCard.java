package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.HistoryEntry;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import com.fossgalaxy.games.fireworks.state.events.CardDiscarded;
import com.fossgalaxy.games.fireworks.state.events.CardInfoColour;
import com.fossgalaxy.games.fireworks.state.events.CardPlayed;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;

import java.util.ListIterator;

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
                        TellColour proposal = new TellColour(nextPlayer, card.colour);
                        if (!alreadyHinted(proposal,nextPlayer, state) && !ConventionUtils.isAnInvalidConventionalTell(proposal, state, playerID, conv))
                            return proposal;
                    }
                } else {
                    TellValue proposal = new TellValue(nextPlayer, card.value);
                    if (!alreadyHinted(proposal,nextPlayer, state) && !ConventionUtils.isAnInvalidConventionalTell(proposal, state, playerID, conv))
                        return proposal;
                }
            }
        }

        return null;
    }

    private boolean alreadyHinted(Action proposal, int playerTold, GameState state) {
        // we iterate through history until we reach a Play by any player, or a Discard by the player being told
        ListIterator<HistoryEntry> backwards = state.getActionHistory().listIterator(state.getActionHistory().size());
        int count = 0;
        while (backwards.hasPrevious() && count < state.getPlayerCount() * 3) {
            count++;
            HistoryEntry h = backwards.previous();
            GameEvent event = h.history.get(0);
            switch (event.getEvent()) {
                case CARD_INFO_COLOUR:
                case CARD_INFO_VALUE:
                    if (h.action.equals(proposal))
                        return true;
                    break;
                case CARD_DISCARDED:
                    if (h.playerID == playerTold)
                        return false;
                    break;
                case CARD_PLAYED:
                    return false;
                default:
            }
        }
        return false;
    }

}

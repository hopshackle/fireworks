package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;
import com.fossgalaxy.games.fireworks.state.events.*;

import java.util.Iterator;
import java.util.ListIterator;
import java.util.stream.IntStream;

/**
 * Tell the next player only about a single card in their hand that is currently useful
 */
public class TellRedOrangeForPlayableNewCard extends AbstractTellRule {

    private Conventions conv;

    public TellRedOrangeForPlayableNewCard(Conventions conv) {
        this.conv = conv;
    }

    @Override
    public Action execute(int playerID, GameState state) {

        for (int i = 0; i < state.getPlayerCount(); i++) {
            int nextPlayer = (playerID + i) % state.getPlayerCount();

            if (handIsEligibleForHint(state, playerID, nextPlayer, CardColour.RED)) {
                return new TellColour(nextPlayer, CardColour.RED);
            }
            if (handIsEligibleForHint(state, playerID, nextPlayer, CardColour.ORANGE)) {
                return new TellColour(nextPlayer, CardColour.ORANGE);
            }
        }
        return null;
    }

    private boolean handIsEligibleForHint(GameState state, int playerTelling, int playerTold, CardColour hintColour) {
        int mostRecentSlot = ConventionUtils.slotMostRecentlyDrawn(state, playerTold);
        if (mostRecentSlot == -1) return false;
        Hand hand = state.getHand(playerTold);
        Card recentCard = hand.getCard(mostRecentSlot);
        if (recentCard != null && recentCard.value == state.getTableValue(recentCard.colour) + 1) {
            // playable, so we're in business
            // now check to see if they have any Red/Orange cards in hand

            boolean hasCardInColour = IntStream.range(0, state.getHandSize()).anyMatch(i -> hand.getCard(i).colour == hintColour);
            if (!hasCardInColour) return false;

            ListIterator<HistoryEntry> backwards = state.getActionHistory().listIterator(state.getActionHistory().size());
            boolean finished = false, alreadyHinted = false;
            int count = 0;
            while (backwards.hasPrevious() && !finished && count < state.getPlayerCount() * 3) {
                count++;
                HistoryEntry h = backwards.previous();
                GameEvent event = h.history.get(0);
                switch (event.getEvent()) {
                    case CARD_INFO_COLOUR:
                        CardInfoColour cardColour = (CardInfoColour) event;
                        if (cardColour.getColour() == hintColour && cardColour.wasToldTo(playerTold)) {
                            finished = true;
                            alreadyHinted = true;
                        }
                        break;
                    case CARD_DISCARDED:
                        CardDiscarded cardDiscarded = (CardDiscarded) event;
                        if (cardDiscarded.getSlotId() == mostRecentSlot && cardDiscarded.getPlayerId() == playerTold)
                            finished = true;
                        break;
                    case CARD_PLAYED:
                        CardPlayed cardPlayed = (CardPlayed) event;
                        if (cardPlayed.getSlotId() == mostRecentSlot && cardPlayed.getPlayerId() == playerTold)
                            finished = true;
                        break;
                    default:
                }
            }

            if (!alreadyHinted && !ConventionUtils.isAnInvalidConventionalTell(new TellColour(playerTold, hintColour), state, playerTelling, conv)) {
               return true;
            }
        }
        return false;
    }
}

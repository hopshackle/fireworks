package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.TellColour;
import com.fossgalaxy.games.fireworks.state.actions.TellValue;

/**
 * Tell an agent about a card that cannot be safely chucked away, because it is the only remaining one of a card that needs to be played.
 */
public class TellNotDiscardable extends AbstractTellRule {

    private boolean avoidConventionalTells;

    public TellNotDiscardable(boolean avoidConventionalTells) {
        this.avoidConventionalTells = avoidConventionalTells;
    }

    @Override
    public Action execute(int playerID, GameState state) {
        // need to inform all players of their hands
        for (int player = 0; player < state.getPlayerCount(); player++) {
            if (player == playerID) continue;
            Hand hand = state.getHand(player);
            for (int slot = 0; slot < state.getHandSize(); slot++) {
                Card actualCard = hand.getCard(slot);
                if (actualCard == null) continue;
                CardColour knownColour = hand.getKnownColour(slot);
                Integer knownValue = hand.getKnownValue(slot);
                Integer actualValue = actualCard.value;
                CardColour actualColour = actualCard.colour;
                if (!cardIsPlayable(actualCard, state) && !cardIsDiscardable(actualCard, state) && cardIsUnique(actualCard, state)) {

                    if (knownValue == null) {
                        Action option = new TellValue(player, actualValue);
                        if (!avoidConventionalTells || !ConventionUtils.isAConventionalTell(option, state, playerID))
                            return option;
                    }

                    if (knownColour == null) {
                        Action option = new TellColour(player, actualColour);
                        if (!avoidConventionalTells || !ConventionUtils.isAConventionalTell(option, state, playerID))
                            return option;
                    }
                }
            }
        }
        return null;
    }

    private boolean cardIsPlayable(Card card, GameState state) {
        int currTable = state.getTableValue(card.colour);
        return (card.value + 1) == currTable;
    }

    private boolean cardIsDiscardable(Card card, GameState state) {
        int currTable = state.getTableValue(card.colour);
        return card.value <= currTable;
    }
    private boolean cardIsUnique(Card card, GameState state) {
        // cannot be discardable or playable to reach this point
        if (card.value == 5) return true;
        long inDiscard = state.getDiscards().stream().filter(card::equals).count();
        if (card.value == 1) return inDiscard == 2;
        return inDiscard == 1;
    }
}

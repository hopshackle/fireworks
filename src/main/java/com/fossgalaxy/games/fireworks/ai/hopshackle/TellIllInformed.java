package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.ai.rule.TryToUnBlock;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;

/**
 * Tell a player that does't know about a useful card that a card can be played.
 * <p>
 * Created by webpigeon on 09/05/17.
 */
public class TellIllInformed extends AbstractTellRule {

    private boolean avoidConventionalTells;

    public TellIllInformed(boolean avoidConventionalTells) {
        this.avoidConventionalTells = avoidConventionalTells;
    }

    @Override
    public Action execute(int playerID, GameState state) {

        for (int i = 0; i < state.getPlayerCount(); i++) {
            int lookingAt = (playerID + i) % state.getPlayerCount();
            if (lookingAt == playerID) {
                continue;
            }

            Action action = getInformingAction(state, playerID, lookingAt);
            if (action != null) {
                return action;
            }
        }

        return null;

    }

    public Action getInformingAction(GameState state, int informer, int target) {
        Hand hand = state.getHand(target);

        for (int slot = 0; slot < hand.getSize(); slot++) {
            if (!hand.hasCard(slot)) {
                continue;
            }

            Card card = hand.getCard(slot);
            if (!TryToUnBlock.isUsableCard(state, card.colour, card.value)) {
                continue;
            }

            CardColour knownColour = hand.getKnownColour(slot);
            Integer knownValue = hand.getKnownValue(slot);
            if (knownColour != null && knownValue != null) {
                continue;
            }

            for (Action a : ConventionUtils.tellMissing(hand, target, slot)) {
                if (!avoidConventionalTells || !ConventionUtils.isAConventionalTell(a, state, informer))
                    return a;
            }
        }

        return null;
    }

}

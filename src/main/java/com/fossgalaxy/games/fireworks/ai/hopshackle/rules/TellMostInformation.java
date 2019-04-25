package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.ai.rule.AbstractTellRule;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;

/**
 * Created by piers on 09/12/16.
 * <p>
 * This will tell the most information to a hand
 * either new information or information in general
 * <p>
 * From Van Den Bergh paper
 */
public class TellMostInformation extends AbstractTellRule {

    private final boolean newInformation = true;
    private Conventions conv;

    public TellMostInformation(Conventions conventions) {
        conv = conventions;
    }

    @Override
    public Action execute(int playerID, GameState state) {
        Action bestAction = null;
        int bestAffected = -1;
        for (int p = 1; p < state.getPlayerCount(); p++) {
            int player = (playerID + p) % state.getPlayerCount();
            if (player == playerID) {
                continue;
            }
            // Get all possible hints for this player
            Hand hand = state.getHand(player);

            for (int i = 1; i <= 5; i++) {
                int totalAffected = 0;
                for (int slot = 0; slot < state.getHandSize(); slot++) {
                    if (hand.hasCard(slot)) {
                        if (!newInformation || hand.getKnownValue(slot) == null) {
                            if (hand.getCard(slot).value == i) {
                                totalAffected++;
                            }
                        }
                    }
                }
                if (totalAffected > bestAffected) {
                    Action newOption = new TellValue(player, i);
                    if (ConventionUtils.isAnInvalidConventionalTell(newOption, state, playerID, conv)) {
                        // skip this one
                    } else {
                        bestAffected = totalAffected;
                        bestAction = newOption;
                    }
                }
            }

            for (CardColour colour : CardColour.values()) {
                int totalAffected = 0;
                for (int slot = 0; slot < state.getHandSize(); slot++) {
                    if (hand.hasCard(slot)) {
                        if (!newInformation || hand.getKnownColour(slot) == null) {
                            if (hand.getCard(slot).colour == colour) {
                                totalAffected++;
                            }
                        }
                    }
                }
                if (totalAffected > bestAffected) {
                    Action newOption = new TellColour(player, colour);
                    if (ConventionUtils.isAnInvalidConventionalTell(newOption, state, playerID, conv)) {
                        // skip this one
                    } else {
                        bestAffected = totalAffected;
                        bestAction = newOption;
                    }
                }
            }
        }

        return bestAction;
    }
}

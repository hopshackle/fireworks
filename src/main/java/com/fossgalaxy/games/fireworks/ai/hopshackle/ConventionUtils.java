package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;
import com.fossgalaxy.games.fireworks.state.events.*;

import java.util.*;
import java.util.stream.Collectors;

public class ConventionUtils {

    public static boolean singleTouchOnNextPlayer = false;

    public static boolean isAConventionalTell(Action proposedTell, GameState state, int playerID) {
        int affected = 0;
        if (singleTouchOnNextPlayer) {
            int nextPlayer = (playerID + 1) % state.getPlayerCount();
            if (proposedTell instanceof TellValue) {
                TellValue tell = (TellValue) proposedTell;
                if (tell.player == nextPlayer) {
                    Hand hand = state.getHand(tell.player);
                    for (int slot = 0; slot < hand.getSize(); slot++) {
                        if (hand.hasCard(slot) && hand.getCard(slot).value == tell.value) affected++;
                    }
                }
            } else if (proposedTell instanceof TellColour) {
                TellColour tell = (TellColour) proposedTell;
                if (tell.player == nextPlayer) {
                    Hand hand = state.getHand(tell.player);
                    for (int slot = 0; slot < hand.getSize(); slot++) {
                        if (hand.hasCard(slot) && hand.getCard(slot).colour == tell.colour) affected++;
                    }
                }
            }
        }
//        System.out.println(String.format("%d\t%s\t%s", playerID, proposedTell, affected == 1));
        return affected == 1;
    }

    public static List<Action> tellMissing(Hand hand, int playerID, int slot) {
        Card card = hand.getCard(slot);
        List<Action> possibles = new ArrayList<>();
        if (hand.getKnownValue(slot) == null) {
            possibles.add(new TellValue(playerID, card.value));
        } else if (hand.getKnownColour(slot) == null) {
            possibles.add(new TellColour(playerID, card.colour));
        }

        return possibles;
    }


    public static Map<Integer, List<Card>> bindBlindCardWithConventions(int player, Hand hand, List<Card> deck, GameState state) {
        Map<Integer, List<Card>> retValue = DeckUtils.bindBlindCard(player, hand, deck);
        // we start with the DeckUtils result, which uses the legal information

        if (singleTouchOnNextPlayer) {
            List<Integer> slotsDrawn = new ArrayList<>();
            Iterator<GameEvent> iter = state.getHistory().descendingIterator();
            boolean stillLooking = true;
            while (iter.hasNext() && stillLooking) {
                GameEvent lastEvent = iter.next();
      //          stillLooking = false;
                if (lastEvent instanceof CardInfo) {
                    CardInfo event = (CardInfo) lastEvent;
                    int previousPlayer = (player - 1 + state.getPlayerCount()) % state.getPlayerCount();
                    if (event.getSlots().length == 1 && event.wasToldTo(player) && event.wasToldBy(previousPlayer)) {
                        // bingo! Only playable cards permitted

                        for (int slot : event.getSlots()) {
                            if (slotsDrawn.contains(slot)) continue;
                            List<Card> startingPoint = retValue.get(slot);
                            List<Card> filtered = startingPoint.stream().
                                    filter(c -> state.getTableValue(c.colour) + 1 == c.value).collect(Collectors.toList());
                            if (!filtered.isEmpty()) {
                                retValue.put(slot, filtered);
                            } else {
   //                             System.out.println("No possible cards left!");
                            }
                        }
                    }
                    // for the moment we only look at the last event, as if this was a TELL, then there will have been no Discarding or Drawing cards
                } else if (lastEvent instanceof CardReceived) {
                    CardReceived cr = (CardReceived) lastEvent;
                    if (cr.getPlayerId() == player) {
                        slotsDrawn.add(cr.getSlotId());
                        // record that data for this slot is now irrelevant
                    }
                } else if (lastEvent instanceof CardPlayed) {
                    stillLooking = false;
                    // we could track which score was affected, but we would have to use the toString() representation to
                    // extract this information, as it is not publicly available in the interface!
                    // so instead, we stop our backward search at a CardPlay event
                }
            }
        }
        return retValue;
    }
}

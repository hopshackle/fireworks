package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;
import com.fossgalaxy.games.fireworks.state.events.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConventionUtils {

    public static boolean isAnInvalidConventionalTell(Action proposedTell, GameState state, int playerTelling, Conventions conv) {
        if (conv.singleTouchIsPlayable) {
            int affected = 0;
            Card hintedCard = null;
            //      int nextPlayer = (playerTelling + 1) % state.getPlayerCount();
            int playerTold = -1;
            int slotTold = -1;
            if (proposedTell instanceof TellValue) {
                TellValue tell = (TellValue) proposedTell;
                playerTold = tell.player;
                //        if (tell.player == nextPlayer) {
                Hand hand = state.getHand(tell.player);
                for (int slot = 0; slot < hand.getSize(); slot++) {
                    if (hand.hasCard(slot) && hand.getCard(slot).value == tell.value) {
                        hintedCard = hand.getCard(slot);
                        slotTold = slot;
                        affected++;
                    }
                    //            }
                }
            } else if (proposedTell instanceof TellColour) {
                TellColour tell = (TellColour) proposedTell;
                playerTold = tell.player;
                //        if (tell.player == nextPlayer) {
                Hand hand = state.getHand(tell.player);
                for (int slot = 0; slot < hand.getSize(); slot++) {
                    if (hand.hasCard(slot) && hand.getCard(slot).colour == tell.colour) {
                        hintedCard = hand.getCard(slot);
                        slotTold = slot;
                        affected++;
                        //                  }
                    }
                }
            }
            // Finally, even if we touch a single non-playable card, this may still be OK (i.e. NOT an invalid conventional tell)
            // if none of the possible cards are playable
            if (affected == 1 && hintedCard.value != (state.getTableValue(hintedCard.colour) + 1)) {
                List<Card> allCards = state.getDeck().toList();
                for (int i = 0; i < state.getHandSize(); i++) {
                    Card c = state.getHand(playerTold).getCard(i);
                    if (c != null) allCards.add(c);
                }
                List<Card> possibleCards = DeckUtils.bindBlindCard(playerTold, state.getHand(playerTold), allCards).get(slotTold);
                Map<CardColour, Integer> currentScores = Arrays.stream(CardColour.values()).collect(Collectors.toMap(Function.identity(), state::getTableValue));
                if (!filterToPlayableCards(possibleCards, currentScores).isEmpty())
                    return true;
            }
        }
        if (conv.redYellowMeansMostRecentIsPlayable) {
            if (proposedTell instanceof TellColour) {
                TellColour tell = (TellColour) proposedTell;
                if (tell.colour == CardColour.RED || tell.colour == CardColour.ORANGE) {
                    int mostRecentSlot = slotMostRecentlyDrawn(state, tell.player);
                    if (mostRecentSlot != -1 && state.getHand(tell.player).hasCard(mostRecentSlot)) {
                        Card mostRecentCard = state.getHand(tell.player).getCard(mostRecentSlot);
                        if (mostRecentCard.value != state.getTableValue(mostRecentCard.colour) + 1)
                            return true; // most recent card is not playable, so this is an invalid Tell
                    }
                }
            }
        }
        return false;
    }

    public static int slotMostRecentlyDrawn(GameState state, int player) {
        Iterator<GameEvent> backwardsHistory = state.getHistory().descendingIterator();
        while (backwardsHistory.hasNext()) {
            GameEvent event = backwardsHistory.next();
            switch (event.getEvent()) {
                case CARD_DISCARDED:
                    CardDiscarded cd = (CardDiscarded) event;
                    if (cd.getPlayerId() == player)
                        return cd.getSlotId();
                    break;
                case CARD_PLAYED:
                    CardPlayed cp = (CardPlayed) event;
                    if (cp.getPlayerId() == player)
                        return cp.getSlotId();
                    break;
                default:
            }
        }
        return -1;
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


    public static Map<Integer, List<Card>> bindBlindCardWithConventions(int player, Hand hand, List<Card> deck, GameState state, Conventions conv) {
        Map<Integer, List<Card>> retValue = DeckUtils.bindBlindCard(player, hand, deck);
        // we start with the DeckUtils result, which uses the grounded information
        if (!conv.redYellowMeansMostRecentIsPlayable && !conv.singleTouchIsPlayable)
            return retValue;

        Set<Integer> slotsDrawn = new HashSet<>();
        Map<CardColour, Integer> scoresAtPointOfTell = new HashMap<>();
        for (CardColour colour : CardColour.values()) {
            scoresAtPointOfTell.put(colour, state.getTableValue(colour));
        }
        // this tracks any slots that have been drawn since the Tell was received, and hence for which we have no information
        Iterator<GameEvent> iter = state.getHistory().descendingIterator();
        int count = 0;
        while (iter.hasNext() && count < state.getPlayerCount() * 3) { // heuristic to go back 3 events per player
            GameEvent lastEvent = iter.next();
            if (lastEvent instanceof CardInfo) {
                CardInfo event = (CardInfo) lastEvent;
                //      int previousPlayer = (player - 1 + state.getPlayerCount()) % state.getPlayerCount();
                if (event.wasToldTo(player)) {
                    if (conv.singleTouchIsPlayable && event.getSlots().length == 1) { //&& event.wasToldBy(previousPlayer)) {
                        // bingo! Only playable cards permitted
                        // so we make sure only cards playable at this point are used
                        for (int slot : event.getSlots()) {
                            if (!slotsDrawn.contains(slot)) {
                                List<Card> playableCards = filterToPlayableCards(retValue.get(slot), scoresAtPointOfTell);
                                if (!playableCards.isEmpty()) {
                                    retValue.put(slot, playableCards);
                                } else {
                                    boolean stop = true;
                                    // This is fine...we cannot misinterpet the Tell, so it just carries its grounded info
                                }
                            }
                        }
                    }
                    if (conv.redYellowMeansMostRecentIsPlayable && event.getEvent() == MessageType.CARD_INFO_COLOUR) {
                        CardInfoColour colourTell = (CardInfoColour) event;
                        if (colourTell.getColour() == CardColour.ORANGE || colourTell.getColour() == CardColour.RED) {
                            // this tells us the most recently drawn slot is playable
                            int slot = slotMostRecentlyDrawn(state, player);
                            if (slot != -1 && !slotsDrawn.contains(slot)) {
                                List<Card> playableCards = filterToPlayableCards(retValue.get(slot), scoresAtPointOfTell);
                                if (!playableCards.isEmpty()) {
                                    retValue.put(slot, playableCards);
                                } else {
                                    boolean stop = true;
                                    // really should not happen...but is possible if someone PLAYs a repeat card
                                    // e.g. O2 when current score is O2
                                }
                            }
                        }
                    }
                }
            } else if (lastEvent instanceof CardDiscarded) {
                CardDiscarded cd = (CardDiscarded) lastEvent;
                if (player == cd.getPlayerId()) {
                    slotsDrawn.add(cd.getSlotId());
                    // record that data for this slot is now irrelevant
                }
            } else if (lastEvent instanceof CardPlayed) {
                CardPlayed cp = (CardPlayed) lastEvent;
                // check that this was a valid play!
                // which we cannot 100% guarantee...e.g. if we play O1 when current score is O1....
                if (scoresAtPointOfTell.get(cp.getColour()) == cp.getValue())
                    scoresAtPointOfTell.put(cp.getColour(), cp.getValue() - 1); // decrement score
                if (player == cp.getPlayerId())
                    slotsDrawn.add(cp.getSlotId());
            }
        }


        return retValue;
    }

    private static List<Card> filterToPlayableCards(List<Card> startingPoint, Map<CardColour, Integer> currentScores) {
        return startingPoint.stream()
                .filter(c -> currentScores.get(c.colour) + 1 == c.value)
                .collect(Collectors.toList());
    }
}

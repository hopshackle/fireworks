package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.determinize;

import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.ConventionUtils;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.Conventions;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StatsCollator;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.*;

public class HandDeterminiser {

    private static long totalActionCount, totalPlay, totalDiscard, universeShiftCountOnPlay, universeShiftCountOnDiscard;
    private int slotLastUsed, otherSlotLastUsed;
    private List<List<Card>> handRecord;
    private int playerCount, rootAgent;
    private boolean alwaysRedeterminise;
    private Card cardLastUsed;
    private Random r = new Random(26678);
    private Logger logger = LoggerFactory.getLogger(HandDeterminiser.class);
    private Conventions conv;

    public HandDeterminiser(GameState state, int rootID, boolean MRIS, Conventions conventions) {
        playerCount = state.getPlayerCount();
        alwaysRedeterminise = MRIS;
        rootAgent = rootID;
        slotLastUsed = -1;
        otherSlotLastUsed = -1;
        this.conv = conventions;

        // we then do our one-off determinisation of the root players cards
        for (int i = 0; i < state.getHandSize(); i++)
            if (state.getHand(rootID).getCard(i) != null) {
                state.getDeck().add(state.getCardAt(rootID, i));
            }
        AllPlayerDeterminiser.bindNewCards(rootID, state, conv);

        // and then store the 'master set' to save back to, and track inconsistent rollouts
        handRecord = IntStream.range(0, playerCount)
                .mapToObj(i -> IntStream.range(0, state.getHandSize())
                        .mapToObj(state.getHand(i)::getCard)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        if (logger.isDebugEnabled()) {
            String handString = IntStream.range(0, state.getHandSize())
                    .mapToObj(state.getHand(rootID)::getCard)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            logger.debug(String.format("Player %d determinised to %s", rootID, handString));
        }
    }

    public void determiniseHandFor(int agentID, GameState state) {
        Hand myHand = state.getHand(agentID);
        Deck deck = state.getDeck();
    //    checkCardTotal(state);

        // reset the hand of the previous agent (if not root) to known values where possible
        reset(agentID, state);

        // if the last move was Play or Discard a card, then we must update the handRecord for the previous player
        // as they have now drawn a card to replace the one Played or Discarded
        if (slotLastUsed != -1) {
            int previousAgent = (agentID + state.getPlayerCount() - 1) % state.getPlayerCount();
            handRecord.get(previousAgent).set(slotLastUsed, state.getCardAt(previousAgent, slotLastUsed));
        }
        if (otherSlotLastUsed != -1) {
            int previousAgent = (agentID + state.getPlayerCount() - 1) % state.getPlayerCount();
            handRecord.get(previousAgent).set(otherSlotLastUsed, state.getCardAt(previousAgent, otherSlotLastUsed));
        }

        // put current hand back into deck (except for the root agent)
        // and then bind new values
        if (agentID != rootAgent) {
            for (int slot = 0; slot < myHand.getSize(); slot++) {
                Card card = myHand.getCard(slot);
                if (card != null) deck.add(card);
            }

            // we then bind new cards (same for root and !root)
            AllPlayerDeterminiser.bindNewCards(agentID, state, conv);
        }
        deck.shuffle();
        checkCardTotal(state);
    }

    private void checkCardTotal(GameState state) {
        int totalCards = state.getScore() + state.getDeck().getCardsLeft() + state.getDiscards().size();
        for (int i = 0; i < state.getPlayerCount(); i++) {
            for (int j = 0; j < state.getHandSize(); j++)
                if (state.getCardAt(i, j) != null) totalCards++;
        }
        if (totalCards != 50) {
            throw new AssertionError("Should have exactly 50 cards at all times");
        }
    }

    public void reset(int agentID, GameState state) {
        // the aim here is to reset all cards to their known versions before rollout
        // reset the hand of the previous agent (if not root) to known values where possible

        // We do not do this for the root agent, as their hand is determinised once for each rollout at initialisation
        // So they keep exactly what happens as events occur in any given rollout
        // (the same reason we skip them in determiniseHandFor()
        if (alwaysRedeterminise) return;        // In MRIS mode we never reset cards, but stay in the new game

        int previousAgent = (agentID + state.getPlayerCount() - 1) % state.getPlayerCount();
        Hand previousHand = state.getHand(previousAgent);
        Deck deck = state.getDeck();
        List<Card> previousCards = handRecord.get(previousAgent);
        otherSlotLastUsed = -1;
        if (previousAgent != rootAgent) {
            if (!previousCards.isEmpty()) {
                for (int slot = 0; slot < previousHand.getSize(); slot++) {
                    if (state.getCardAt(previousAgent, slot) != null) {
                        if (slot == slotLastUsed) {
                            // do not add the card that was at this position to deck, as it has had all information wiped.
                            // instead we add back to deck the current card that was drawn after play/discard (and will pick a new one)
                            // (we put back the current card because there is a chance that it was one of the known cards in hand before we re-determinised)
                            deck.add(state.getCardAt(previousAgent, slot));
                        } else {
                            deck.add(previousHand.getCard(slot)); // add hand back to deck
                        }
                    }
                }
                List<Card> previousMinusSlotUsed = IntStream.range(0, previousCards.size())
                        .filter(i -> i != slotLastUsed).mapToObj(previousCards::get).collect(Collectors.toList());
                if (cardLastUsed != null && previousMinusSlotUsed.contains(cardLastUsed)) {
                    // this is another special case, in which the card we discarded was in our previous hand (but in another slot)
                    // in this case we must not naively re-bind it, as that might create a new card!
                    // Instead we re-bind any valid card that meets the criteria for the slot
                    otherSlotLastUsed = previousCards.indexOf(cardLastUsed);
                    if (otherSlotLastUsed == slotLastUsed)
                        otherSlotLastUsed = previousCards.lastIndexOf(cardLastUsed);
                }
                for (int slot = 0; slot < previousHand.getSize(); slot++) {
                    if (slot == slotLastUsed || slot == otherSlotLastUsed) {
                        // wait until all known cards have been rebound
                    } else if (previousCards.get(slot) != null) {
                        previousHand.bindCard(slot, previousCards.get(slot));   // re-bind card
                        int deckSize = deck.getCardsLeft();
                        deck.remove(previousCards.get(slot));  // and remove it from the deck
                        if (deck.getCardsLeft() == deckSize) {
                            throw new AssertionError("Ooops - this should have declined");
                        }
                    }
                }
                if (otherSlotLastUsed > -1) {
                    // we need to draw a card that is compatible with our known information
                    List<Card> possibles = DeckUtils.bindBlindCard(previousAgent, previousHand, deck.toList()).get(otherSlotLastUsed);
                    if (possibles.isEmpty()) {
                        // OK. This *can* occur because the only card(s) that are now bindable
                        // are in fact sitting elsewhere in the hand and are not in the deck.
                        // Ideally what we do here is re-try, with the rest of the hand to find the problematic card, and then
                        // recurse?
                        // or we wimp out and bind a random card, wiping out the information in the slot
                        deck.shuffle();
                        previousHand.setCard(otherSlotLastUsed, deck.getTopCard()); // this wipes known information
                    } else {
                        Card chosen = possibles.get(r.nextInt(possibles.size()));
                        previousHand.bindCard(otherSlotLastUsed, chosen);
                        deck.remove(chosen);
                    }
                }
                if (slotLastUsed > -1 && deck.hasCardsLeft()) {
                    deck.shuffle();
                    // since we hav no information on the card drawn, we just re-draw from the shuffled deck
                    Card topCard = deck.getTopCard();
                    previousHand.bindCard(slotLastUsed, topCard);
                }
            }
        }
  //      checkCardTotal(state);
    }


    /* This is called before we aply the action to the GameState
        so all the cards are still in position
     */
    public void recordAction(Action action, int playerID, GameState state) {
        slotLastUsed = -1;
        cardLastUsed = null;
        totalActionCount++;
        if (action instanceof PlayCard) {
            slotLastUsed = ((PlayCard) action).slot;
            totalPlay++;
        }
        if (action instanceof DiscardCard) {
            slotLastUsed = ((DiscardCard) action).slot;
            totalDiscard++;
        }
        if (slotLastUsed != -1) {
            cardLastUsed = state.getCardAt(playerID, slotLastUsed);
            if (!cardLastUsed.equals(getHandRecord(playerID, slotLastUsed))) {
                // the card that we played or discarded was different to the one everyone else knew we had
                // i.e. it was IS-Incompatible to them. We have shifted game universe.
                if (action instanceof PlayCard) {
                    universeShiftCountOnPlay++;
                } else {
                    universeShiftCountOnDiscard++;
                }
            }
        }
    }

    public Card getHandRecord(int agent, int slot) {
        if (handRecord.get(agent).size() <= slot) return null;
        return handRecord.get(agent).get(slot);
    }

    public int getSlotLastUsed() {
        return slotLastUsed;
    }

    public static double percentageUniverseShiftOfPlay() {
        return (double) universeShiftCountOnPlay / (double) totalPlay;
    }

    public static double percentageUniverseShiftOfDiscard() {
        return (double) universeShiftCountOnDiscard / (double) totalDiscard;
    }

    public static double percentageUniverseShiftOfTotal() {
        return (double) (universeShiftCountOnDiscard + universeShiftCountOnPlay) / (double) totalActionCount;
    }

    public static void resetUniverseShift() {
        totalActionCount = 0;
        totalDiscard = 0;
        totalPlay = 0;
        universeShiftCountOnPlay = 0;
        universeShiftCountOnDiscard = 0;
    }

    public static long getUniverseShiftPlay() {
        return universeShiftCountOnPlay;
    }

    public static long getUniverseShiftDiscard() {
        return universeShiftCountOnDiscard;
    }
}

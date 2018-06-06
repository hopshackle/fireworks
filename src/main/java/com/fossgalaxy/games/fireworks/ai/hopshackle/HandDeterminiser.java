package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;

import java.util.*;
import java.util.stream.*;

public class HandDeterminiser {

    private int slotLastUsed;
    private List<List<Card>> handRecord;
    private int playerCount, rootAgent;
    private Card cardLastUsed;
    private Random r = new Random(26678);

    public HandDeterminiser(GameState state, int rootID) {
        playerCount = state.getPlayerCount();
        rootAgent = rootID;
        slotLastUsed = -1;
        handRecord = new ArrayList<>(playerCount);
        for (int i = 0; i < playerCount; i++) {
            handRecord.add(new ArrayList<>());
        }
        // we then do our one-off determinisation of the root players cards
        for (int i = 0; i < state.getHandSize(); i++)
            if (state.getHand(rootID).getCard(i) != null) {
                state.getDeck().add(state.getCardAt(rootID, i));
            }
        bindNewCards(rootID, state);
    }

    public void determiniseHandFor(int agentID, GameState state) {
        Hand myHand = state.getHand(agentID);
        Deck deck = state.getDeck();

        // record cards currently in hand for agent, so we can go back to these after their turn
        List<Card> handDetail = new ArrayList<>(state.getHandSize());
        handRecord.set(agentID, handDetail);
        for (int slot = 0; slot < state.getHandSize(); slot++) {
            handDetail.add(myHand.getCard(slot));
        }

        // reset the hand of the previous agent (if not root) to known values where possible
        reset(agentID, state);

        // put current hand back into deck (except for the root agent)
        // and then bind new values
        if (agentID != rootAgent) {
            for (int slot = 0; slot < myHand.getSize(); slot++) {
                Card card = myHand.getCard(slot);
                if (card != null) deck.add(card);
            }

            // we then bind new cards (same for root and !root)
            bindNewCards(agentID, state);
        }
        deck.shuffle();

        int totalCards = state.getScore() + deck.getCardsLeft() + state.getDiscards().size();
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

        int previousAgent = (agentID + state.getPlayerCount() - 1) % state.getPlayerCount();
        Hand previousHand = state.getHand(previousAgent);
        Deck deck = state.getDeck();
        if (previousAgent != rootAgent) {
            List<Card> previousCards = handRecord.get(previousAgent);
            if (!previousCards.isEmpty()) {
                int otherSlotToRedraw = -1;
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
                if (cardLastUsed != null && previousCards.contains(cardLastUsed) && !previousCards.get(slotLastUsed).equals(cardLastUsed)) {
                    // this is another special case, in which the card we discarded was in our previous hand (but in another slot)
                    // in this case we must not naively re-bind it, as that might create a new card!
                    // Instead we re-bind any valid card that meets the criteria for the slot
                    otherSlotToRedraw = previousCards.indexOf(cardLastUsed);
                }
                for (int slot = 0; slot < previousHand.getSize(); slot++) {
                    if (slot == slotLastUsed || slot == otherSlotToRedraw) {
                        // wait until all known cards have been rebound
                    } else {
                        previousHand.bindCard(slot, previousCards.get(slot));   // re-bind card
                        deck.remove(previousCards.get(slot));  // and remove it from the deck
                    }
                }
                if (otherSlotToRedraw > -1) {
                    // we need to draw a card that is compatible with our known information
                    List<Card> possibles = DeckUtils.bindBlindCard(previousAgent, previousHand, deck.toList()).get(otherSlotToRedraw);
                    if (possibles.isEmpty()) {
                        // OK. This *can* occur because the only card(s) that are now bindable
                        // are in fact sitting elsewhere in the hand and are not in the deck.
                        // Ideally what we do here is re-try, with the rest of the hand to find the problematic card, and then
                        // recurse?
                        // or we wimp out and bind a random card, wiping out the information in the slot
                        deck.shuffle();
                        previousHand.setCard(otherSlotToRedraw, deck.getTopCard()); // this wipes known information
                    } else {
                        Card chosen = possibles.get(r.nextInt(possibles.size()));
                        previousHand.bindCard(otherSlotToRedraw, chosen);
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
    }

    /*
    Cards in previous hand have already been added back into deck before this method is called
     */
    private void bindNewCards(int agentID, GameState state) {
        Hand myHand = state.getHand(agentID);
        Deck deck = state.getDeck();
        List<Card> toChooseFrom = state.getDeck().toList();

        if (toChooseFrom.isEmpty()) {
            throw new AssertionError("no cards ");
        } else {
            Map<Integer, List<Card>> possibleCards = DeckUtils.bindBlindCard(agentID, state.getHand(agentID), toChooseFrom);
            List<Integer> bindOrder = DeckUtils.bindOrder(possibleCards);
            bindOrder = bindOrder.stream().filter(slot -> !possibleCards.get(slot).isEmpty()).collect(Collectors.toList());
            Map<Integer, Card> myHandCards = DeckUtils.bindCards(bindOrder, possibleCards);
            for (int slot = 0; slot < myHand.getSize(); slot++) {
                Card hand = myHandCards.getOrDefault(slot, null);
                myHand.bindCard(slot, hand);
                deck.remove(hand);
            }
        }
    }

    public void recordAction(Action action, int playerID, GameState state) {
        slotLastUsed = -1;
        cardLastUsed = null;
        if (action instanceof PlayCard) slotLastUsed = ((PlayCard) action).slot;
        if (action instanceof DiscardCard) slotLastUsed = ((DiscardCard) action).slot;
        if (slotLastUsed != -1) {
            cardLastUsed = state.getCardAt(playerID, slotLastUsed);
        }
    }

    public Card getHandRecord(int agent, int slot) {
        if (handRecord.get(agent).size() <= slot) return null;
        return handRecord.get(agent).get(slot);
    }

    public int getSlotLastUsed() {
        return slotLastUsed;
    }
}

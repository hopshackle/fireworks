package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HandDeterminiser {

    private int slotLastUsed;
    private List<List<Card>> handRecord;
    private int playerCount, rootAgent;

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
    }

    public void reset(int agentID, GameState state) {
        // the aim here is to reset all cards to their known versions before rollout
        // reset the hand of the previous agent (if not root) to known values where possible
        int previousAgent = (agentID + state.getPlayerCount() - 1) % state.getPlayerCount();
        Hand previousHand = state.getHand(previousAgent);
        Deck deck = state.getDeck();
        if (previousAgent != rootAgent) {
            List<Card> previousCards = handRecord.get(previousAgent);
            if (!previousCards.isEmpty()) {
                for (int slot = 0; slot < previousHand.getSize(); slot++) {
                    if (state.getCardAt(previousAgent, slot) != null) {
                        if (slot == slotLastUsed) {
                            // do not add this card to deck, as it was played or discarded!
                            // instead we add back to deck the current card (and will pick a new one)
                            // we put back the current card because there is a chance that it was
                            // one of the known cards in hand before we re-determinised
                            deck.add(state.getCardAt(previousAgent, slot));
                        } else {
                            deck.add(previousHand.getCard(slot)); // add hand back to deck
                        }
                    }
                }
                for (int slot = 0; slot < previousHand.getSize(); slot++) {
                    if (slot == slotLastUsed) {
                        // wait until all known cards have been rebound
                    } else {
                        previousHand.bindCard(slot, previousCards.get(slot));   // re-bind card
                        deck.remove(previousCards.get(slot));  // and remove it from the deck
                    }
                }
                if (slotLastUsed > -1 && deck.hasCardsLeft()) {
                    // since we hav no information on the card drawn, we just re-draw from the shuffled deck
                    deck.shuffle();
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

    public void recordAction(Action action) {
        slotLastUsed = -1;
        if (action instanceof PlayCard) slotLastUsed = ((PlayCard) action).slot;
        if (action instanceof DiscardCard) slotLastUsed = ((DiscardCard) action).slot;
    }

    public Card getHandRecord(int agent, int slot) {
        if (handRecord.get(agent).size() <= slot) return null;
        return handRecord.get(agent).get(slot);
    }

    public int getSlotLastUsed() {
        return slotLastUsed;
    }
}

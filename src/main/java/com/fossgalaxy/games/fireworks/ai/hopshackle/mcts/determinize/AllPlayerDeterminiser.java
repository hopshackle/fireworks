package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.determinize;

import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.ConventionUtils;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.Conventions;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.*;
import com.fossgalaxy.games.fireworks.state.actions.*;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.*;

public class AllPlayerDeterminiser {

    protected GameState[] determinisationsByPlayer;
    protected MCTSNode parentNode, triggerNode;
    protected int root;
    private static Logger logger = LoggerFactory.getLogger(AllPlayerDeterminiser.class);
    private Conventions conv;

    public AllPlayerDeterminiser(AllPlayerDeterminiser apd) {
        root = apd.root;
        determinisationsByPlayer = new GameState[apd.determinisationsByPlayer.length];
        for (int i = 0; i < determinisationsByPlayer.length; i++) {
            determinisationsByPlayer[i] = apd.determinisationsByPlayer[i].getCopy();
            parentNode = apd.parentNode;
        }
        conv = apd.conv;
    }

    public AllPlayerDeterminiser(GameState stateToCopy, int rootPlayerID, String conventionString) {
        root = rootPlayerID;
        determinisationsByPlayer = new GameState[stateToCopy.getPlayerCount()];
        conv = new Conventions(conventionString);
        // we then do our one-off determinisation of the root player's cards
        // this is because these cards are the ones visible to the other players, so will be constant for the
        // remaining determinisations
        determinisationsByPlayer[root] = determiniseHand(root, stateToCopy);
        logHand(determinisationsByPlayer[root], root);

        // and then determinise for each of the other players
        for (int i = 0; i < stateToCopy.getPlayerCount(); i++) {
            if (i == root) continue;
            determinisationsByPlayer[i] = determiniseHand(i, determinisationsByPlayer[root]);
            logHand(determinisationsByPlayer[i], i);
        }
    }

    private GameState determiniseHand(int player, GameState base) {
        GameState state = base.getCopy();
        Hand myHand = state.getHand(player);
        Deck deck = state.getDeck();

        // put current hand back into deck
        // and then bind new values
        for (int slot = 0; slot < myHand.getSize(); slot++) {
            Card card = myHand.getCard(slot);
            if (card != null) deck.add(card);
        }

        // we then bind new cards
        bindNewCards(player, state, conv);
        deck.shuffle();

        int totalCards = state.getScore() + deck.getCardsLeft() + state.getDiscards().size();
        for (int i = 0; i < state.getPlayerCount(); i++) {
            for (int j = 0; j < state.getHandSize(); j++)
                if (state.getHand(i).hasCard(j)) totalCards++;
        }
        if (totalCards != 50) {
            throw new AssertionError("Should have exactly 50 cards at all times");
        }
        return state;
    }

    private void logHand(GameState state, int player) {
        if (logger.isDebugEnabled()) {
            String handString = IntStream.range(0, state.getHandSize())
                    .mapToObj(state.getHand(player)::getCard)
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            logger.debug(String.format("Player %d determinised to %s", player, handString));
        }
    }

    /*
    Cards in previous hand have already been added back into deck before this method is called
     */
    public static void bindNewCards(int agentID, GameState state, Conventions conv) {
        Hand myHand = state.getHand(agentID);
        Deck deck = state.getDeck();
        List<Card> toChooseFrom = state.getDeck().toList();

        if (toChooseFrom.isEmpty()) {
            throw new AssertionError("no cards ");
        } else {
            Map<Integer, List<Card>> possibleCardsFinal = ConventionUtils.bindBlindCardWithConventions(agentID, state.getHand(agentID), toChooseFrom, state, conv);
            for (int i : possibleCardsFinal.keySet()) {
                if (myHand.hasCard(i) && possibleCardsFinal.get(i).isEmpty())
                    throw new AssertionError("Should have possible card in slot " + i);
            }
            List<Integer> bindOrder = DeckUtils.bindOrder(possibleCardsFinal);
            bindOrder = bindOrder.stream().filter(slot -> !possibleCardsFinal.get(slot).isEmpty()).collect(Collectors.toList());
            Map<Integer, List<Card>> possibleCards = possibleCardsFinal;
            int attempts = 0;
            boolean success = false;
            do {
                try {
                    Map<Integer, Card> myHandCards = DeckUtils.bindCards(bindOrder, possibleCards);
                    for (int slot = 0; slot < myHand.getSize(); slot++) {
                        Card hand = myHandCards.getOrDefault(slot, null);
                        myHand.bindCard(slot, hand);
                        deck.remove(hand);
                    }
                    success = true;
                } catch (IllegalArgumentException e) {
                    // this may occur if  we do have a cards for the slot, these have all gone by the time we get to bind it.
                    // to address this
                    attempts++;
                    if (attempts > 5 && attempts < 9) {
                        // we give up
                        logger.info("Failed to bind cards in HandDeterminiser using Conventions - trying without");
                        Map<Integer, List<Card>> possibleCardsFinal2 = DeckUtils.bindBlindCard(agentID, state.getHand(agentID), toChooseFrom);
                        bindOrder = DeckUtils.bindOrder(possibleCardsFinal2);
                        bindOrder = bindOrder.stream().filter(slot -> !possibleCardsFinal2.get(slot).isEmpty()).collect(Collectors.toList());
                        possibleCards = possibleCardsFinal2;
                    } else if (attempts > 8) {
                        // OK. We're really stuck
                        logger.info("That didn't work either");
                        throw e;
                    }
                }
            } while (!success);
        }
    }

    /*
    This method should redeterminise the game for the specified playerID, while keeping within their current
    Information Set, so that the determinisation remains consistent with all previous events
     */
    public void redeterminiseWithinIS(int playerID) {
        determinisationsByPlayer[playerID] = determiniseHand(playerID, determinisationsByPlayer[root]);
        if (playerID == root) {
            // this also means that all other players will likely have inconsistent determinisations
            throw new AssertionError("Not yet implemented for the root of an APD");
        }
    }

    public void applyAndCompatibilise(MCTSNode node, boolean makeConsistent) {
        Action action = node.getAction();
        GameState masterDeterminisation = getDeterminisationFor(root).getCopy();
        // firstly we apply the action to the master determinisation
        try {
            action.apply(node.getAgentId(), determinisationsByPlayer[root]);
        } catch (RulesViolation rv) {
            throw rv;
        }
        // then for each other determinisation we check if it is consistent
        for (int i = 0; i < masterDeterminisation.getPlayerCount(); i++) {
            if (i == root) continue;
            if (!isConsistent(action, node.getAgentId(),
                    determinisationsByPlayer[i],
                    masterDeterminisation)) {
                if (!makeConsistent)
                    throw new AssertionError("We have an inconsistent determinisation where we are not expecting one");
            }
            // We need to redeterminise to a valid game state given the history
            // For each iteration, we always have a fixed set of cards for the root player (as these are unknown
            // to everybody). Hence we take that as a base (with the correct
            // card either played or discarded), put the ith player's hand into the deck and re-bind.
            // We could check for full compatibility and apply the action individually...but this is less error prone
            determinisationsByPlayer[i] = determiniseHand(i, determinisationsByPlayer[root]);
            logHand(determinisationsByPlayer[i], i);
        }

        for (int i = 0; i < determinisationsByPlayer.length; i++) {
            if (determinisationsByPlayer[i].getTurnNumber() != determinisationsByPlayer[root].getTurnNumber())
                throw new AssertionError("All determinisations should have the same number of turns");
        }

        if (logger.isDebugEnabled())
            logger.debug("CRIS-MCTS: Selected action " + action + " for player " + node.getAgentId());

        if (node.getReferenceState() == null)
            node.setReferenceState(determinisationsByPlayer[root].getCopy());

    }

    public static boolean isConsistent(Action action, int player, GameState state, GameState reference) {

        if (action instanceof PlayCard) {
            PlayCard playCard = (PlayCard) action;
            Card played = state.getCardAt(player, playCard.slot);
            return (played.equals(reference.getCardAt(player, playCard.slot)));
        }
        if (action instanceof DiscardCard) {
            DiscardCard discardCard = (DiscardCard) action;
            Card discarded = state.getCardAt(player, discardCard.slot);
            return (discarded.equals(reference.getCardAt(player, discardCard.slot)));
        }
        if (action instanceof TellValue) {
            // We are inconsistent only if none of our cards can possible have this value
            // which technically should never happen
            TellValue tellValue = (TellValue) action;
            for (int i = 0; i < state.getHandSize(); i++) {
                for (int v : state.getHand(tellValue.player).getPossibleValues(i)) {
                    if (v == tellValue.value) return true;
                }
            }
            throw new AssertionError("Completely inconsistent Tell has occurred " + tellValue.toString());
        }
        if (action instanceof TellColour) {
            TellColour tellColour = (TellColour) action;
            for (int i = 0; i < state.getHandSize(); i++) {
                for (CardColour c : state.getHand(tellColour.player).getPossibleColours(i)) {
                    if (c == tellColour.colour) return true;
                }
            }
            throw new AssertionError("Completely inconsistent Tell has occurred " + tellColour.toString());
        }
        throw new AssertionError("Unknown Action type " + action.toString() + " in compatibility check");
    }

    public GameState getDeterminisationFor(int playerID) {
        return determinisationsByPlayer[playerID];
    }

    public GameState getMasterDeterminisation() {
        return determinisationsByPlayer[root];
    }

    public int getRootPlayer() {
        return root;
    }

    public MCTSNode getParentNode() {
        return parentNode;
    }

    public MCTSNode getTriggerNode() {return triggerNode;}

    public void setParentNode(MCTSNode node) {
        parentNode = node;
    }

    public void setTriggerNode(MCTSNode node) { if (triggerNode == null) triggerNode = node;}

    public String toString() {
        return String.format("Root %d from node %s", root, parentNode);
    }
}

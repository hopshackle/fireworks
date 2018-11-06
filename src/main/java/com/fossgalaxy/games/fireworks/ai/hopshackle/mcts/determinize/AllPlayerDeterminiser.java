package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.determinize;

import com.fossgalaxy.games.fireworks.GameStats;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.ConventionUtils;
import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.*;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.*;

public class AllPlayerDeterminiser {

    protected GameState[] determinisationsByPlayer;
    protected int root;
    private static Logger logger = LoggerFactory.getLogger(HandDeterminiser.class);

    public AllPlayerDeterminiser(AllPlayerDeterminiser apd) {
        root = apd.root;
        determinisationsByPlayer = new GameState[apd.determinisationsByPlayer.length];
        for (int i = 0; i < determinisationsByPlayer.length; i++) {
            determinisationsByPlayer[i] = apd.determinisationsByPlayer[i].getCopy();
        }
    }

    public AllPlayerDeterminiser(GameState stateToCopy, int rootPlayerID) {
        root = rootPlayerID;
        determinisationsByPlayer = new GameState[stateToCopy.getPlayerCount()];

        // we then do our one-off determinisation of the root player's cards
        // this is because these cards are the ones visible to the other players, so will be constant for the
        // remaining determinisations
        GameState state = stateToCopy.getCopy();
        for (int i = 0; i < state.getHandSize(); i++)
            if (state.getHand(root).getCard(i) != null) {
                state.getDeck().add(state.getCardAt(root, i));
            }
        bindNewCards(root, state);
        state.getDeck().shuffle();
        logHand(state, root);

        determinisationsByPlayer[root] = state;

        // and then determinise for each of the other players
        for (int i = 0; i < stateToCopy.getPlayerCount(); i++) {
            if (i == root) continue;
            state = determinisationsByPlayer[root].getCopy();
            Hand myHand = state.getHand(i);
            Deck deck = state.getDeck();

            // put current hand back into deck
            // and then bind new values
            for (int slot = 0; slot < myHand.getSize(); slot++) {
                Card card = myHand.getCard(slot);
                if (card != null) deck.add(card);
            }

            // we then bind new cards
            bindNewCards(i, state);
            deck.shuffle();

            int totalCards = state.getScore() + deck.getCardsLeft() + state.getDiscards().size();
            for (int i1 = 0; i1 < state.getPlayerCount(); i1++) {
                for (int j = 0; j < state.getHandSize(); j++)
                    if (state.getCardAt(i, j) != null) totalCards++;
            }
            if (totalCards != 50) {
                throw new AssertionError("Should have exactly 50 cards at all times");
            }

            determinisationsByPlayer[i] = state;
            logHand(state, i);
        }
    }

    private void logHand(GameState state, int player) {
        if (logger.isDebugEnabled()) {
            String handString = IntStream.range(0, state.getHandSize())
                    .mapToObj(state.getHand(root)::getCard)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            logger.debug(String.format("Player %d determinised to %s", player, handString));
        }

    }

    /*
    Cards in previous hand have already been added back into deck before this method is called
     */
    public static void bindNewCards(int agentID, GameState state) {
        Hand myHand = state.getHand(agentID);
        Deck deck = state.getDeck();
        List<Card> toChooseFrom = state.getDeck().toList();

        if (toChooseFrom.isEmpty()) {
            throw new AssertionError("no cards ");
        } else {
            Map<Integer, List<Card>> possibleCardsFinal = ConventionUtils.bindBlindCardWithConventions(agentID, state.getHand(agentID), toChooseFrom, state);
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

    public AllPlayerDeterminiser copyApplyCompatibilise(MCTSNode node) {

        AllPlayerDeterminiser retValue = new AllPlayerDeterminiser(this);
        Action action = node.getAction();
        for (int i = 0; i < determinisationsByPlayer.length; i++) {
            if (isCompatible(action, determinisationsByPlayer[i])) {
                GameState state = retValue.getDeterminisationFor(i);
                state.tick();
                List<GameEvent> events = action.apply(node.getAgentId(), state);
                events.forEach(state::addEvent);
            } else {
                // We need to redeterminise to a valid game state given the history
                // For each iteration, we always have a fixed set of cards for the root player (as these are unknown
                // to everybody). Hence all we need to do is put hand into the deck, bind new valid cards to hand, and
                // shuffle the deck
                // TODO:
            }
        }
        // we then apply the action to state, and re-determinise the hand for the next agent

        if (logger.isDebugEnabled())
            logger.debug("CRIS-MCTS: Selected action " + action + " for player " + node.getAgentId());

        if (node.getReferenceState() == null)
            node.setReferenceState(retValue.getDeterminisationFor(node.getAgentId()).getCopy());

        return retValue;
    }

    public GameState getDeterminisationFor(int playerID) {
        return determinisationsByPlayer[playerID];
    }
}

package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.utils.DebugUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class MCTSRulePolicy extends MCTSRule {
    private final Logger LOG = LoggerFactory.getLogger(MCTSRuleInfoSetPolicy.class);
    private final Agent rolloutPolicy;

    @AgentConstructor("hs-mctsRulePolicy")
    public MCTSRulePolicy(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        this.rolloutPolicy = rollout;
        // TODO: Parameterise this more elegantly in future
        if (rollout instanceof EvalFnAgent)
            expansionPolicy = new RuleFullExpansion(logger, random, MCTSRuleInfoSet.allRules, Optional.empty(), Optional.of((EvalFnAgent) rollout));
    }

    /**
     * Rather than perform a random move, query a policy for one.
     *
     * @param state
     * @param playerID
     * @return
     */
    @Override
    protected Action selectActionForRollout(GameState state, int playerID) {
        try {
            // we first need to ensure Player's hand is back in deck
            Hand myHand = state.getHand(playerID);
            Deck deck = state.getDeck();
            int cardsAddedToDeck = 0;
            for (int i = 0; i < myHand.getSize(); i++) {
                if (myHand.getCard(i) != null) {
                    deck.add(myHand.getCard(i));
                    //                System.out.println("Added " + myHand.getCard(i));
                    cardsAddedToDeck++;
                }
            }
            // then choose the action
            Action chosenAction = rolloutPolicy.doMove(playerID, state);
            // then put their hand back
            for (int i = 0; i < cardsAddedToDeck; i++) {
                Card removedCard = deck.getTopCard();
                //         System.out.println("Removed " + removedCard);
            }

            return chosenAction;
        } catch (IllegalArgumentException ex) {
            LOG.error("warning, agent failed to make move: {}", ex);
            return super.selectActionForRollout(state, playerID);
        } catch (IllegalStateException ex) {
            LOG.error("Problem with Rules in rollout {} for player {} using policy {}", ex, playerID, rolloutPolicy);
            DebugUtils.printState(LOG, state);
            return super.selectActionForRollout(state, playerID);
        }
    }

    @Override
    public String toString() {
        return String.format("policyMCTS(%s)", rolloutPolicy);
    }
}

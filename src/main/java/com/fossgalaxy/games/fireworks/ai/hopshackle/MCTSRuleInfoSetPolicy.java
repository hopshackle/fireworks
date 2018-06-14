package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;
import com.fossgalaxy.games.fireworks.utils.DebugUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * A version of the MCTS agent that replaces the random rollout with policy based rollouts.
 */
public class MCTSRuleInfoSetPolicy extends MCTSRuleInfoSet {
    private final Logger LOG = LoggerFactory.getLogger(MCTSRuleInfoSetPolicy.class);
    private final Agent rolloutPolicy;

    public MCTSRuleInfoSetPolicy(Agent rolloutPolicy) {
        this.rolloutPolicy = rolloutPolicy;
    }

    @AgentConstructor("mctsRuleISPolicy")
    public MCTSRuleInfoSetPolicy(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        this.rolloutPolicy = rollout;
        // TODO: Parameterise this more elegantly in future
        if (rollout instanceof ClassifierFnAgent)
            expansionPolicy = new RuleFullExpansion(logger, random, allRules, Optional.of((ClassifierFnAgent) rollout), Optional.empty());
        if (rollout instanceof EvalFnAgent)
            expansionPolicy = new RuleFullExpansion(logger, random, allRules, Optional.empty(), Optional.of((EvalFnAgent) rollout));
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
            LOG.error("Problem with Rules in rollout {} for player {}", ex, playerID);
            DebugUtils.printState(LOG, state);
            return new DiscardCard(0);
        }
    }

    @Override
    public String toString() {
        return String.format("policyMCTS(%s)", rolloutPolicy);
    }
}

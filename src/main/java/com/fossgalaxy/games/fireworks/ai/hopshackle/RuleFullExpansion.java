package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import org.slf4j.Logger;

import java.util.*;

public class RuleFullExpansion extends RuleExpansionPolicy {

    protected Optional<ClassifierFnAgent> QAgent;
    protected Optional<EvalFnAgent> VAgent;

    public RuleFullExpansion(Logger logger, Random random, List<Rule> allRules, Optional<ClassifierFnAgent> qAgent, Optional<EvalFnAgent> vAgent) {
        super(logger, random, allRules);
        QAgent = qAgent;
        VAgent = vAgent;
    }


    @Override
    public Action selectActionForExpand(GameState state, MCTSNode node, int agentID) {
        throw new UnsupportedOperationException("Full Expansion Policy should not use this");
    }

    @Override
    public MCTSNode expand(MCTSNode parent, GameState state) {
        // We need to obtain a score for all actions, and then create nodes for all
        // the problem is what value to create these with...we could start with the value of the parent node
        // which is a bit of a hack...but

        int nextAgentID = (parent.getAgent() + 1) % state.getPlayerCount();
        Map<Action, Double> actionValues = new HashMap<>();
        if (QAgent.isPresent()) {
            actionValues = QAgent.get().getAllActionValues(nextAgentID, state);
            double stateValue = VAgent.isPresent()
                    ? VAgent.get().valueState(state, Optional.empty(), nextAgentID)
                    : state.getScore();
            double largestValue = Double.NEGATIVE_INFINITY;
            for (Action a : actionValues.keySet()) {
                double value = actionValues.get(a);
                if (value > largestValue) {
                    largestValue = value;
                }
            }
            for (Action a : actionValues.keySet()) {
                double value = actionValues.get(a);
                actionValues.put(a, stateValue - (1.0 - value / largestValue));
            }
        } else {
            actionValues = VAgent.get().getAllActionValues(nextAgentID, state);
        }


        MCTSNode best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (Action a : actionValues.keySet()) {
            if (!parent.containsChild(a)) {
                MCTSNode child = createNode(parent, nextAgentID, a, state, parent.expConst);
                child.visits = 1;
                child.parentWasVisitedAndIWasLegal.put(a, 1);
                child.score = actionValues.get(a);
                if (child.score > bestScore) {
                    bestScore = child.score;
                    best = child;
                }
                // we assume that the best action achieves the state value, and that one with a value of 0.0 achieves one point fewer
                if (logger.isTraceEnabled())
                    logger.trace(String.format("Creating node for %s with score %.2f (parent %.2f)", a.toString(), child.score, parent.score / parent.visits));
                parent.addChild(child);
            }
        }
        return best;
    }

}

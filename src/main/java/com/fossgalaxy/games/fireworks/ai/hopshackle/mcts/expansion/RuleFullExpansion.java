package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion;

import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSNode;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import org.slf4j.Logger;

import java.util.*;

public class RuleFullExpansion extends RuleExpansionPolicy {

    protected EvalFnAgent VAgent;

    public RuleFullExpansion(Logger logger, Random random, List<Rule> allRules, EvalFnAgent vAgent) {
        super(logger, random, allRules);
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
        // which is a bit of a hack...but...
        // we then assume that the best move is worth a point, and pro rata the rest

        int nextAgentID = parent.singleAgentTree ? parent.getAgentId() : (parent.getAgentId() + 1) % state.getPlayerCount();
        Map<Action, Double> actionValues = VAgent.getAllActionValues(nextAgentID, state);

        for (Action a : actionValues.keySet()) {
            if (!parent.containsChild(a)) {
                MCTSNode child = createNode(parent, nextAgentID, a, parent.expConst, 1, actionValues.get(a) * 25.0 + state.getScore());
                // we assume that the best action achieves the state value, and that one with a value of 0.0 achieves one point fewer
                if (logger.isDebugEnabled())
                    logger.debug(String.format("Creating node for %s with score %.2f (parent %.2f)", a.toString(), child.getMeanScore(), parent.getMeanScore() / parent.getVisits()));
                parent.addChild(child);
            }
        }
        // Now we have expanded all nodes, we use standard UCT to pick one
        return parent.getUCTNode(state, false);
    }

}

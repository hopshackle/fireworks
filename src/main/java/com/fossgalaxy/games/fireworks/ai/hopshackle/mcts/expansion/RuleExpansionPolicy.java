package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSNode;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSRuleNode;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import org.slf4j.Logger;
import java.util.*;

public class RuleExpansionPolicy extends SimpleNodeExpansion {

    protected List<Rule> allRules;

    public RuleExpansionPolicy(Logger logger, Random random, List<Rule> rules) {
        super(logger, random);
        allRules = rules;
    }

    @Override
    public MCTSNode createNode(MCTSNode parent, int previousAgentID, Action moveTo, double C, int priorVisits, double priorValue) {
        MCTSRuleNode root = new MCTSRuleNode(
                (MCTSRuleNode) parent,
                previousAgentID,
                moveTo, C,
                allRules, priorVisits, priorValue);

        return root;
    }


    @Override
    public MCTSNode createRoot(GameState refState, int previousAgentID, double C) {
        MCTSNode root = new MCTSRuleNode(
                null,
                previousAgentID,
                null, C, allRules, 0, 0.0);
        return root;
    }

}

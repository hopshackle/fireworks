package com.fossgalaxy.games.fireworks.ai.hopshackle;

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
    public MCTSNode createNode(MCTSNode parent, int previousAgentID, Action moveTo, GameState state, double C) {
        MCTSRuleNode root = new MCTSRuleNode(
                (MCTSRuleNode) parent,
                previousAgentID,
                moveTo, C,
                allRules, state);

        return root;
    }
}

package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import org.slf4j.Logger;

import java.util.List;
import java.util.Random;

public class RuleExpansionPolicyOpponentModel extends RuleExpansionPolicy {

    public RuleExpansionPolicyOpponentModel(Logger logger, Random random, List<Rule> rules) {
        super(logger, random, rules);
    }

    @Override
    public MCTSNode createNode(MCTSNode parent, int previousAgentID, Action moveTo, double C) {
        int playerIDToUse = parent.agentId;
        return super.createNode(parent, playerIDToUse, moveTo, C);
    }

    @Override
    public MCTSNode createRoot(GameState refState, int previousAgentID, double C) {
        return super.createRoot(refState, previousAgentID, C);
    }
}

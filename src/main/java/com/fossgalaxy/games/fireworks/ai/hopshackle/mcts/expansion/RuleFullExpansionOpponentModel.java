package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion;

import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSNode;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import org.slf4j.Logger;

import java.util.*;

public class RuleFullExpansionOpponentModel extends RuleFullExpansion {

    /*
    The only difference is that all the MCTSNodes are from the perspective of the same agent
     */
    public RuleFullExpansionOpponentModel(Logger logger, Random random, List<Rule> allRules, Optional<EvalFnAgent> vAgent) {
        super(logger, random, allRules, vAgent);
    }

    @Override
    public MCTSNode createNode(MCTSNode parent, int previousAgentID, Action moveTo, double C) {
        int playerIDToUse = parent == null ? previousAgentID : parent.getAgentId();
        return super.createNode(parent, playerIDToUse, moveTo, C);
    }

}

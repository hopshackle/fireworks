package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.RuleGenerator;
import com.fossgalaxy.games.fireworks.ai.rule.*;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;

import java.util.*;

public class CRIS2_MCTSRule extends CRIS2_MCTS {

    protected List<Rule> allRules;

    @AgentConstructor("hs-CRIS2Rule")
    public CRIS2_MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, String rules, String conventions, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit, conventions, rollout);
        allRules = RuleGenerator.generateRules(rules, conventions);
        if (rollout instanceof EvalFnAgent)
            expansionPolicy = new RuleFullExpansion(logger, random, allRules, (EvalFnAgent) rollout);
        else
            expansionPolicy = new RuleExpansionPolicy(logger, random, allRules);
    }


    @Override
    public String toString() {
        return String.format("CRIS-MCTSRule(%s)", rolloutPolicy == null ? "NONE" : rolloutPolicy.toString());
    }

}

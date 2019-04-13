package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.*;

import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.RuleGenerator;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;

import java.util.*;

public class MCTSRule extends MCTS {

    protected List<Rule> allRules;

    @AgentConstructor("hs-ISRule")
    public MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, String rules, String conventions, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        allRules = RuleGenerator.generateRules(rules, conventions);
        rolloutPolicy = rollout == null ? new RandomEqual(0) : rollout;
        if (rollout instanceof EvalFnAgent)
            expansionPolicy = new RuleFullExpansion(logger, random, allRules, Optional.of((EvalFnAgent) rollout));
        else
            expansionPolicy = new RuleExpansionPolicy(logger, random, allRules);

    }

    @Override
    public String toString() {
        return String.format("MCTSRule(%s)", rolloutPolicy == null ? "NONE" : rolloutPolicy.toString());
    }
}

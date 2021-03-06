package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.RuleGenerator;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;

import java.util.List;
import java.util.Optional;

public class CRIS_MCTSRule extends CRIS_MCTS {

    protected List<Rule> allRules;

    @AgentConstructor("hs-CRISRule")
    public CRIS_MCTSRule(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, String rules, String conventions, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit, conventions, rollout);
        allRules = RuleGenerator.generateRules(rules, conventions);
        // TODO: Parameterise this more elegantly in future
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

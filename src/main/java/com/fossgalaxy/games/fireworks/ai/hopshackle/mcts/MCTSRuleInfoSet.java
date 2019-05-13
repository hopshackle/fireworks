package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.RuleGenerator;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;

import java.util.*;

/**
 * Created by WebPigeon on 09/08/2016.
 */
public class MCTSRuleInfoSet extends MCTSInfoSet {

    public List<Rule> allRules;

    @AgentConstructor("hs-RISRule")
    public MCTSRuleInfoSet(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, String ruleMnemonics, String conventions, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit, conventions, rollout);
        allRules = RuleGenerator.generateRules(ruleMnemonics, conventions);
        if (rollout instanceof EvalFnAgent)
            expansionPolicy = new RuleFullExpansion(logger, random, allRules, (EvalFnAgent) rollout);
        else
            expansionPolicy = new RuleExpansionPolicy(logger, random, allRules);
    }

    @Override
    public String toString() {
        return String.format("MCTSRuleInfoSet(%s)", rolloutPolicy == null ? "NONE" : rolloutPolicy.toString());
    }

}

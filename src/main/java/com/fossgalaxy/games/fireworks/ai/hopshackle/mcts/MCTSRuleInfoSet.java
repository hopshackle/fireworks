package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.RuleFullExpansion;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.PlayProbablySafeCard;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.PlayProbablySafeLateGameCard;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.RuleExpansionPolicy;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.*;
import com.fossgalaxy.games.fireworks.ai.rule.AbstractRule;
import com.fossgalaxy.games.fireworks.ai.rule.CompleteTellUsefulCard;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;

import java.util.*;

/**
 * Created by WebPigeon on 09/08/2016.
 */
public class MCTSRuleInfoSet extends MCTSInfoSet {

    public static TreeMap<Integer, Rule> masterRuleMap = new TreeMap<>();
    public List<Rule> allRules;

    static {
        masterRuleMap.put(1, new TellNextPlayerAboutSingleUsefulCard());
        masterRuleMap.put(2, new TellMostInformation(true, true));
        masterRuleMap.put(3, new TellAnyoneAboutUsefulCard(true));
        masterRuleMap.put(4, new TellDispensable(true));
        masterRuleMap.put(5, new TellIllInformed(true));
        masterRuleMap.put(6, new CompleteTellUsefulCard());
        masterRuleMap.put(7, new CompleteTellDispensableCard());
        masterRuleMap.put(8, new CompleteTellCurrentlyNotPlayableCard());
        masterRuleMap.put(9, new PlayProbablySafeCard(0.7));
        masterRuleMap.put(10, new PlayProbablySafeLateGameCard(0.4, 5));
        masterRuleMap.put(11, new DiscardProbablyUselessCard(0.8));
        masterRuleMap.put(12, new DiscardLeastLikelyToBeNecessary());
        masterRuleMap.put(13, new DiscardProbablyUselessCard(0.0));
        masterRuleMap.put(14, new PlayBestCardIfTwoPlayerAndCannotDiscard());
        masterRuleMap.put(15, new TellNotDiscardable(true));
// 1|2|3|4|6|7|8|9|10|11|12|15
        /*
        allRulesWithoutConventions.add(new TellMostInformation(true, false));
        allRulesWithoutConventions.add(new TellAnyoneAboutUsefulCard(false));
        allRulesWithoutConventions.add(new TellDispensable(false));
   //     allRulesWithoutConventions.add(new TellIllInformed(false));
        allRulesWithoutConventions.add(new CompleteTellUsefulCard());
        allRulesWithoutConventions.add(new CompleteTellDispensableCard());
        allRulesWithoutConventions.add(new CompleteTellCurrentlyNotPlayableCard());
        allRulesWithoutConventions.add(new PlayProbablySafeCard(0.7));
        allRulesWithoutConventions.add(new PlayProbablySafeLateGameCard(0.4, 5));
        //     allRules.add(new DiscardProbablyUselessCard(0.8));
        //    allRules.add(new DiscardOldestFirst());
        //     allRules.add(new DiscardOldestNoInfoFirst());
        //      allRules.add(new DiscardLeastLikelyToBeNecessary());
        allRulesWithoutConventions.add(new DiscardProbablyUselessCard(0.0));
    //    allRulesWithoutConventions.add(new PlayBestCardIfTwoPlayerAndCannotDiscard());
        //      allRules.add(new TellFives());
        allRulesWithoutConventions.add(new TellNotDiscardable(false));
        */
    }

    @AgentConstructor("hs-RISRule")
    public MCTSRuleInfoSet(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, String ruleMnemonics, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        allRules = initialiseRules(ruleMnemonics);
        rolloutPolicy = rollout == null ? new RandomEqual(0) : rollout;
        // TODO: Parameterise this more elegantly in future
        if (rollout instanceof EvalFnAgent)
            expansionPolicy = new RuleFullExpansion(logger, random, allRules, Optional.of((EvalFnAgent) rollout));
        else
            expansionPolicy = new RuleExpansionPolicy(logger, random, allRules);
    }

    public static List<Rule> initialiseRules(String mnemonics) {
        List<Rule> retValue = new ArrayList<>();
        String[] ruleArray = mnemonics.split("[|]");
        for (String mnemonic : ruleArray) {
            Integer key = Integer.valueOf(mnemonic);
            if (masterRuleMap.containsKey(key)){
                retValue.add(masterRuleMap.get(key));
            } else {
                throw new AssertionError("Mnemonic for rule does not exist: " + mnemonic);
            }
        }
        return retValue;
    }


    @Override
    public String toString() {
        return String.format("MCTSRuleInfoSet(%s)", rolloutPolicy == null ? "NONE" : rolloutPolicy.toString());
    }

}

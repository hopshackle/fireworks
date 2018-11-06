package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.PlayProbablySafeCard;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.PlayProbablySafeLateGameCard;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.RuleExpansionPolicy;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.*;
import com.fossgalaxy.games.fireworks.ai.rule.CompleteTellUsefulCard;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;

import java.util.*;

/**
 * Created by WebPigeon on 09/08/2016.
 */
public class MCTSRuleInfoSet extends MCTSInfoSet {

    public static final List<Rule> allRules = new ArrayList<>();
    public static final List<Rule> allRulesWithoutConventions = new ArrayList<>();

    static {
        allRules.add(new TellNextPlayerAboutSingleUsefulCard());
        //      allRules.add(new TellPreviousPlayerAboutSingleDiscardableCard());
        allRules.add(new TellMostInformation(true, true));
        allRules.add(new TellAnyoneAboutUsefulCard(true));
        allRules.add(new TellDispensable(true));
   //     allRules.add(new TellIllInformed(true));
        allRules.add(new CompleteTellUsefulCard());
        allRules.add(new CompleteTellDispensableCard());
        allRules.add(new CompleteTellCurrentlyNotPlayableCard());
        allRules.add(new PlayProbablySafeCard(0.7));
        allRules.add(new PlayProbablySafeLateGameCard(0.4, 5));
        //     allRules.add(new DiscardProbablyUselessCard(0.8));
        //    allRules.add(new DiscardOldestFirst());
        //     allRules.add(new DiscardOldestNoInfoFirst());
        // allRules.add(new DiscardLeastLikelyToBeNecessary());
        allRules.add(new DiscardProbablyUselessCard(0.0));
        // allRules.add(new PlayBestCardIfTwoPlayerAndCannotDiscard());
        //      allRules.add(new TellFives());

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
    }

    /**
     * Create a default MCTS implementation.
     * <p>
     * This creates an MCTS agent that has a default roll-out length of 50_000 iterations, a depth of 18 and a tree
     * multiplier of 1.
     */
    public MCTSRuleInfoSet() {
        this(MCTSNode.DEFAULT_EXP_CONST, DEFAULT_ROLLOUT_DEPTH, DEFAULT_TREE_DEPTH_MUL, DEFAULT_TIME_LIMIT);
    }

    public MCTSRuleInfoSet(double expConst) {
        this(expConst, DEFAULT_ROLLOUT_DEPTH, DEFAULT_TREE_DEPTH_MUL, DEFAULT_TIME_LIMIT);
    }

    /**
     * Create an MCTS agent which has the parameters.
     *
     * @param explorationC
     * @param rolloutDepth
     * @param treeDepthMul
     * @param timeLimit    in ms
     */
    @AgentConstructor("hs-mctsRuleMR")
    public MCTSRuleInfoSet(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        expansionPolicy = new RuleExpansionPolicy(logger, random, allRules);
    }


    @Override
    public String toString() {
        return "MCTSRuleInfoSet";
    }

}

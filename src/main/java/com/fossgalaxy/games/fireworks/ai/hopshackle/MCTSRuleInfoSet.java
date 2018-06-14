package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.mcts.IterationObject;
import com.fossgalaxy.games.fireworks.ai.osawa.rules.OsawaDiscard;
import com.fossgalaxy.games.fireworks.ai.osawa.rules.TellPlayableCardOuter;
import com.fossgalaxy.games.fireworks.ai.rule.*;
import com.fossgalaxy.games.fireworks.ai.rule.random.DiscardProbablyUselessCard;
import com.fossgalaxy.games.fireworks.ai.rule.random.PlayProbablySafeCard;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by WebPigeon on 09/08/2016.
 */
public class MCTSRuleInfoSet extends MCTSInfoSet {

    public static final List<Rule> allRules = new ArrayList<>();

    static {
  //      allRules.add(new OsawaDiscard());
  //      allRules.add(new DiscardHighest());
        allRules.add(new TellAboutOnes());
        allRules.add(new TellMostInformation(true));
        allRules.add(new TellAnyoneAboutUsefulCard());
        allRules.add(new TellDispensable());
        allRules.add(new TellPlayableCardOuter());
        allRules.add(new TellIllInformed());
        allRules.add(new TellFinesse());
        allRules.add(new PlaySafeCard());
        allRules.add(new PlayProbablySafeCard(0.6));
        allRules.add(new PlayFinesse());
        allRules.add(new DiscardProbablyUselessCard(0.6));
        allRules.add(new DiscardOldestFirst());
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
    @AgentConstructor("mctsRuleIS")
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

package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.LegalActionFilter;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;

import java.util.*;
import java.util.stream.*;

/**
 * Created by hopshackle on 26/05/2018.
 */
public class MCTSRuleNode extends MCTSNode {

    protected final List<Rule> allRules;

    public MCTSRuleNode(MCTSRuleNode parent, int agentId, Action moveToState, double expConst, List<Rule> possibleRules, int priorVisits, double priorValue) {
        super(parent, agentId, moveToState, expConst, new ArrayList<>(), priorVisits, priorValue);
        allRules = possibleRules;
        assert (parent != null && moveToState != null) || (parent == null && moveToState == null);
    }

    @Override
    public void addChild(MCTSNode node) {
        children.add(node);
    }

    @Override
    public MCTSNode getUCTNode(GameState state, boolean trial) {
        double bestScore = -Double.MAX_VALUE;
        MCTSNode bestChild = null;

        int agentToAct = singleAgentTree ? agentId : (getAgentId() + 1) % state.getPlayerCount();
        List<Action> legalMoves = getAllLegalMoves(state, agentToAct);

        List<MCTSNode> validChildren = children.stream()
                .filter(c -> legalMoves.contains(c.moveToState))
                .collect(Collectors.toList());

        for (MCTSNode child : validChildren) {
            double childScore = child.getUCTValue() + (random.nextDouble() * EPSILON);
            if (logger.isDebugEnabled()) logger.debug(String.format("\tUCT: %.2f from base %.2f (%d/%d complete/eligible visits) for %s", childScore,
                    child.score / child.visits, child.getVisits(), parentWasVisitedAndIWasLegal.get(child.moveToState), child.moveToState));

            if (childScore > bestScore) {
                bestScore = childScore;
                bestChild = child;
            }
        }
        if (!trial) incrementParentVisitsForAllEligibleActions(state);

        if (logger.isDebugEnabled()) logger.debug(String.format("\tChosen Action is %s", bestChild == null ? "NULL" : bestChild.moveToState));
        return bestChild;
    }

    public List<Rule> getRulesForChild(MCTSNode child, GameState fromState, int agentID) {
        return allRules.stream()
                .filter(r -> {
                    Action a = r.execute(agentID, fromState);
                    if (a == null) return false;
                    return (a.equals(child.getAction()));
                })
                .collect(Collectors.toList());
    }

    @Override
    public boolean fullyExpanded(GameState state, int nextId) {
        return getLegalUnexpandedMoves(state, nextId).isEmpty();
    }

    public List<Action> getAllLegalMoves(GameState state, int nextID) {
        // we assume that state has had hand/deck sorted before making a decision

        List<Action> retValue = allRules.stream()
                .map(r -> r.execute(nextID, state))
                .filter(Objects::nonNull)
                .distinct()
                .filter(LegalActionFilter.isLegal(nextID, state))
                .collect(Collectors.toList());

        return retValue;
    }

    @Override
    public List<Action> getLegalUnexpandedMoves(GameState state, int nextId) {
        return getAllLegalMoves(state, nextId).stream()
                .filter(a -> !containsChild(a))
                .collect(Collectors.toList());
    }

}

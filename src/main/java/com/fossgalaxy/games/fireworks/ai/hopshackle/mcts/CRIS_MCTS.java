package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.determinize.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.*;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;
import com.fossgalaxy.games.fireworks.state.events.*;

import java.util.*;
import java.util.stream.*;

public class CRIS_MCTS extends MCTS {
    /*
    Consistent Re-determinising Information Set Monte Carlo Tree Search
     */
    @AgentConstructor("CRIS-MCTS")
    public CRIS_MCTS(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit) {
//        this.roundLength = roundLength;
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
    }


    @Override
    protected void executeSearch(int agentID, MCTSNode root, GameState state, int movesLeft) {
        long finishTime = System.currentTimeMillis() + timeLimit;

        while (System.currentTimeMillis() < finishTime || rollouts == 0) {
            //find a leaf node
            rollouts++;
            GameState currentState = state.getCopy();
            AllPlayerDeterminiser apd = new AllPlayerDeterminiser(state, agentID);
            if (logger.isDebugEnabled()) {
                String logMessage = "All legal moves from root: " +
                        ((MCTSRuleNode) root).getAllLegalMoves(apd.getDeterminisationFor(agentID), (agentID + 1) % state.getPlayerCount())
                                .stream()
                                .map(Action::toString)
                                .collect(Collectors.joining("\t"));
                logger.debug(logMessage);
            }
            executeBranchingSearch(agentID, apd, root, currentState, movesLeft);

            if (calcTree) {
                System.out.println(root.printD3());
            }
        }
    }

    protected void executeBranchingSearch(int agentID, AllPlayerDeterminiser apd, MCTSNode root, GameState state, int movesLeft) {

        /*
        Here we need to make a decision for each of the possible determinisations
        But we do not want to roll all the way out!
        So this incorporates select() and rollout() and backup()
         */
        int treeDepth = calculateTreeDepthLimit(state);
        MCTSNode[] next = new MCTSNode[state.getPlayerCount()];
        boolean[] expanded = new boolean[state.getPlayerCount()];
        for (int i = 0; i < state.getPlayerCount(); i++) {
            GameState determinisation = apd.getDeterminisationFor(i);
            nodeExpanded = false;
            if (!state.isGameOver() && root.getDepth() < treeDepth && movesLeft > 0) {
                next[i] = oneStepSelect(root, determinisation, movesLeft, apd);
                if (next[i] != null & next[i].getAgent() != agentID) {
                    throw new AssertionError("WTF");
                }
                if (nodeExpanded) {
                    expanded[i] = true;
                    nodesExpanded++;
                    if (next[i].getDepth() > deepestNode) deepestNode = next[i].getDepth();
                    allNodeDepths += next[i].getDepth();
                }
            }
        }

        /*
        Now we have a node for each determinisation. If any of them are null, or just expanded, then we can
        immediately rollout() and backup(). Rollout() only uses the determinisation responsible for the decision.
        So we do not need to compatibilise.
        For all others, we recursively call executeBranchingSearch after we have renewed determinisations to
        be compatible with the actual move made
        */
        for (int i = 0; i < state.getPlayerCount(); i++) {
            if (next[i] == null) {
                double score = rollout(apd.getDeterminisationFor(i), root, movesLeft);
                root.backup(score);
                if (logger.isDebugEnabled()) logger.debug(String.format("Backing up a final score of %.2f", score));
            } else if (expanded[i]) {
                double score = rollout(apd.getDeterminisationFor(i), next[i], movesLeft - 1);
                next[i].backup(score);
                if (logger.isDebugEnabled()) logger.debug(String.format("Backing up a final score of %.2f", score));
            } else {
                boolean oldAction = false;
                for (int j = 0; j < i; j++)
                    if (next[i] == next[j]) oldAction = true;
                if (!oldAction) {
                    // We only explore each node taken once
                    int agentAboutToAct = (next[i].getAgent() + 1) % state.getPlayerCount();

                    AllPlayerDeterminiser newApd = apd.copyApplyCompatibilise(next[i]);
                    executeBranchingSearch(agentAboutToAct, newApd, next[i], newApd.getDeterminisationFor(i), movesLeft - 1);
                }
            }
        }
    }


    protected MCTSNode oneStepSelect(MCTSNode current, GameState state, int movesLeft, AllPlayerDeterminiser apd) {
        MCTSNode next;
        if (current.fullyExpanded(state)) {
            next = current.getUCTNode(state);
        } else {
            next = expand(current, state);
            nodeExpanded = true;
        }
        return next;
    }
}
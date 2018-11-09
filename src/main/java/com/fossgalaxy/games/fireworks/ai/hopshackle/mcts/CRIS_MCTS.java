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
        //      MCTSNode[] next = new MCTSNode[state.getPlayerCount()];
        //     boolean[] expanded = new boolean[state.getPlayerCount()];
        MCTSNode next = null;
        if (root.getDepth() < treeDepth) {
            next = oneStepSelect(root, apd.getDeterminisationFor(agentID));
            if (next != null && next.getAgentId() != agentID) {
                throw new AssertionError("WTF");
            }
            if (logger.isDebugEnabled())
                logger.debug(String.format("Determinisation %d for player %d makes decision %s",
                        agentID, agentID, next.getAction().toString()));
            if (nodeExpanded) {
                nodesExpanded++;
                if (next.getDepth() > deepestNode) deepestNode = next.getDepth();
                allNodeDepths += next.getDepth();
            }
        }

        /*
        If the new node is null, or just expanded, then we can
        immediately rollout() and backup(). Rollout() only uses the determinisation responsible for the decision.
        So we do not need to compatibilise.
        In all other cases, we recursively call executeBranchingSearch after we have renewed determinisations to
        be compatible with the actual move made
        */

        if (next == null) {
            double score = rollout(apd.getMasterDeterminisation().getCopy(), root, movesLeft);
            if (logger.isDebugEnabled())
                logger.debug(String.format("Rollout at tree limit give score of %.2f", score));
            root.backup(score, apd);
            if (logger.isDebugEnabled()) logger.debug(String.format("Backing up a final score of %.2f", score));
        } else if (nodeExpanded) {
            double score = rollout(apd.getMasterDeterminisation().getCopy(), next, movesLeft - 1);
            next.backup(score, apd);
            if (logger.isDebugEnabled()) logger.debug(String.format("Backing up a final score of %.2f", score));
        } else {
            AllPlayerDeterminiser referenceAPD = new AllPlayerDeterminiser(apd);

            int agentAboutToAct = (next.getAgentId() + 1) % state.getPlayerCount();

            List<Integer> branchesNeeded = apd.applyAndCompatibilise(next);
            executeBranchingSearch(agentAboutToAct, apd, next, apd.getMasterDeterminisation(), movesLeft - 1);

            for (int i : branchesNeeded) {
                // we need to branch
                AllPlayerDeterminiser newApd = new AllPlayerDeterminiser(referenceAPD.getDeterminisationFor(i), i);
                newApd.setParentNode(root);
                newApd.applyAndCompatibilise(next);
                if (logger.isDebugEnabled())
                    logger.debug(String.format("Launching search for %s", newApd));
                executeBranchingSearch(agentAboutToAct, newApd, next, newApd.getDeterminisationFor(i), movesLeft - 1);
            }
        }
    }

    protected MCTSNode oneStepSelect(MCTSNode current, GameState state) {
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
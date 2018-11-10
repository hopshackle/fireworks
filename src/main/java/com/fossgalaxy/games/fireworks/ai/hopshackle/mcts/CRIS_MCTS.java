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

        boolean activeActionConsistentWithMasterDeterminisation = false;
        do {
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
        If the new node is null, then no decision has been taken, there is no risk of inconsistency and we can
        immediately rollout() and backup(). Rollout() only uses the master determinisation for the APD.
        In all other cases, we recursively call executeBranchingSearch after we have renewed determinisations to
        be compatible with the actual move made
        */

            if (next == null) {
                double score = rollout(apd.getMasterDeterminisation().getCopy(), root, movesLeft);
                if (logger.isDebugEnabled())
                    logger.debug(String.format("Rollout at tree limit give score of %.2f", score));
                root.backup(score, apd.getParentNode());
                if (logger.isDebugEnabled()) logger.debug(String.format("Backing up a final score of %.2f", score));
                return;
            } else {
                int agentNextToAct = (next.getAgentId() + 1) % state.getPlayerCount();
                // we actually just check here for inconsistency!
                List<Integer> branchesNeeded = new ArrayList();
                for (int i = 0; i < state.getPlayerCount(); i++) {
                    if (!AllPlayerDeterminiser.isConsistent(next.getAction(), agentID, apd.getDeterminisationFor(i), apd.getMasterDeterminisation())) {
                        branchesNeeded.add(i);
                        if (i == apd.getRootPlayer())
                            throw new AssertionError("Should only ever need to branch for the active determinisation");
                    }
                }
                activeActionConsistentWithMasterDeterminisation = branchesNeeded.isEmpty();

                for (int i : branchesNeeded) {
                    // we need to branch
                    AllPlayerDeterminiser newApd = new AllPlayerDeterminiser(apd.getDeterminisationFor(i), i);
                    newApd.setParentNode(root);
                    newApd.applyAndCompatibilise(next, true); // we use the action taken by the active determinisation
                    if (logger.isDebugEnabled())
                        logger.debug(String.format("Launching search for %s", newApd));
                    if (nodeExpanded) {
                        double score = rollout(newApd.getDeterminisationFor(i), next, movesLeft - 1);
                        next.backup(score, root);
                        if (logger.isDebugEnabled()) logger.debug(String.format("Backing up a final score of %.2f", score));
                    } else {
                        executeBranchingSearch(agentNextToAct, newApd, next, newApd.getDeterminisationFor(i), movesLeft - 1);
                    }
                    // we then need to redeterminise the branched determinisation in apd, so that on the next iteration
                    // the same action might be compatible
                    apd.redeterminiseWithinIS(i);
                }

            }
        } while (!activeActionConsistentWithMasterDeterminisation);

        // now that we finally have an action compatible with our master determinisation, off we go
        // Now we can kick off the APD that bought us here....up to now we have been kicking off APDs to improve
        // the local opponent model
        apd.applyAndCompatibilise(next, false);
        if (nodeExpanded) {
            double score = rollout(apd.getMasterDeterminisation(), next, movesLeft - 1);
            next.backup(score, apd.getParentNode());
            if (logger.isDebugEnabled()) logger.debug(String.format("Backing up a final score of %.2f", score));
        } else {
            int agentNextToAct = (next.getAgentId() + 1) % state.getPlayerCount();
            executeBranchingSearch(agentNextToAct, apd, next, apd.getMasterDeterminisation(), movesLeft - 1);
        }

    }

    private void applyActionBeforeRollout(MCTSNode node, GameState state) {
        try {
            state.tick();
            List<GameEvent> events = node.getAction().apply(node.getAgentId(), state);
            events.forEach(state::addEvent);
        } catch (RulesViolation rv) {
            throw rv;
        }
    }

    protected MCTSNode oneStepSelect(MCTSNode current, GameState state) {
        nodeExpanded = false;
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
package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
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
    @AgentConstructor("CRIS-MCTSPolicy")
    public CRIS_MCTS(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit);
        this.rolloutPolicy = rollout;
    }


    public CRIS_MCTS() {
        this(MCTSNode.DEFAULT_EXP_CONST, DEFAULT_ROLLOUT_DEPTH, DEFAULT_TREE_DEPTH_MUL, DEFAULT_TIME_LIMIT);
    }

    @Override
    protected void executeSearch(int agentID, MCTSNode root, GameState state, int movesLeft) {
        long finishTime = System.currentTimeMillis() + timeLimit;

        while (System.currentTimeMillis() < finishTime || rollouts == 0) {
            //find a leaf node
            rollouts++;
            AllPlayerDeterminiser apd = new AllPlayerDeterminiser(state, agentID);
            executeBranchingSearch(agentID, apd, root, movesLeft);

            if (calcTree) {
                System.out.println(root.printD3());
            }
        }
    }

    protected void executeBranchingSearch(int agentID, AllPlayerDeterminiser apd, MCTSNode root, int movesLeft) {

        GameState state = apd.getDeterminisationFor(agentID);
        if (state.isGameOver() || movesLeft < 1) {
            root.backup(state.getScore(), apd.getParentNode());
            return;
        }
        /*
        Here we need to make a decision for each of the possible determinisations
        But we do not want to roll all the way out!
        So this incorporates select() and rollout() and backup()
         */
        int treeDepth = calculateTreeDepthLimit(state);
        //      MCTSNode[] next = new MCTSNode[state.getPlayerCount()];
        //     boolean[] expanded = new boolean[state.getPlayerCount()];
        MCTSNode next = null;
        int iteration = 0;
        int possibleActions = root.allUnexpandedActions.size() + root.getChildren().size();
        boolean activeActionConsistentWithMasterDeterminisation = false;
        do {
            if (root.getDepth() < treeDepth) {
                iteration++;

                // put active hand into deck for decision making
                Hand myHand = state.getHand(agentID);
                Deck deck = state.getDeck();
                Card[] hand = new Card[myHand.getSize()];
                int cardsAdded = 0;
                for (int i = 0; i < myHand.getSize(); i++) {
                    if (myHand.getCard(i) != null) {
                        deck.add(myHand.getCard(i));
                        hand[i] = myHand.getCard(i);
                        myHand.bindCard(i, null);
                        cardsAdded++;
                    }
                }
                if (root instanceof MCTSRuleNode)
                    possibleActions = ((MCTSRuleNode) root).getAllLegalMoves(state, agentID).size();

                next = oneStepSelect(root, state);

                // then undo hand to deck hack before proceeding
                for (int i = 0; i < hand.length; i++) {
                    if (hand[i] != null) {
                        myHand.bindCard(i, hand[i]);
                    }
                }
                for (int i = 0; i < cardsAdded; i++) deck.getTopCard();

                if (next != null && next.getAgentId() != agentID) {
                    throw new AssertionError("WTF");
                }
                if (logger.isDebugEnabled())
                    logger.debug(String.format("Determinisation %d for player %d makes decision %s",
                            agentID, agentID, next == null ? "null" : next.getAction().toString()));
                if (nodeExpanded) {
                    nodesExpanded++;
                    if (next.getDepth() > deepestNode) deepestNode = next.getDepth();
                    allNodeDepths += next.getDepth();
                }
            } else {
                next = null;
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
                if (branchesNeeded.size() > 1) {
                    boolean stop = true;
                }

                if (possibleActions == 1) {
                    // if we only have one action, then there is no point branching...as this will only improve the opponent model at this node
                    // and this information will never be used...all we want to do is find an action that is compatible with the master determinisation
                    if (!activeActionConsistentWithMasterDeterminisation)
                        apd.redeterminiseWithinIS(agentID);
                } else {
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
                            if (logger.isDebugEnabled())
                                logger.debug(String.format("Backing up a final score of %.2f", score));
                        } else {
                            executeBranchingSearch(agentNextToAct, newApd, next, movesLeft - 1);
                        }
                        // we then need to redeterminise the branched determinisation in apd, so that on the next iteration
                        // the same action might be compatible
                        apd.redeterminiseWithinIS(i);
                    }
                }
            }
        } while (!activeActionConsistentWithMasterDeterminisation);
        // if only one action is possible, then data we gather from branched rollouts cannot possibly be used.
        // the apd that back-propagates will always use the known values of the discarded card.

        if (iteration > 50) {
  //          System.out.println(String.format("Final consistent active action %s after %d iterations at depth %d", next.getAction(), iteration, root.getDepth()));
        }
        // now that we finally have an action compatible with our master determinisation, off we go
        // Now we can kick off the APD that bought us here....up to now we have been kicking off APDs to improve
        // the local opponent model
        apd.applyAndCompatibilise(next, false);
        // if we only have one possible action, then we override consistency
        if (nodeExpanded) {
            double score = rollout(apd.getMasterDeterminisation(), next, movesLeft - 1);
            next.backup(score, apd.getParentNode());
            if (logger.isDebugEnabled()) logger.debug(String.format("Backing up a final score of %.2f", score));
        } else {
            int agentNextToAct = (next.getAgentId() + 1) % state.getPlayerCount();
            executeBranchingSearch(agentNextToAct, apd, next, movesLeft - 1);
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

    public String toString() {
        return String.format("CRIS-MCST(%s)", rolloutPolicy == null ? "NONE" : rolloutPolicy.toString());
    }
}
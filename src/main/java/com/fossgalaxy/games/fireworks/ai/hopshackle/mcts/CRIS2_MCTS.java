package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.determinize.AllPlayerDeterminiser;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StatsCollator;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.Deck;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;

import java.util.ArrayList;
import java.util.List;

public class CRIS2_MCTS extends CRIS_MCTS {

    @AgentConstructor("hs-CRIS2")
    public CRIS2_MCTS(double explorationC, int rolloutDepth, int treeDepthMul, int timeLimit, String conventions, Agent rollout) {
        super(explorationC, rolloutDepth, treeDepthMul, timeLimit, conventions, rollout);
    }

    protected void executeBranchingSearch(int agentID, AllPlayerDeterminiser apd, MCTSNode root, int movesLeft) {

        GameState state = apd.getDeterminisationFor(agentID);
        if (state.isGameOver() || movesLeft < 1) {
            root.backup(state.getScore(), apd.getTriggerNode(), apd.getParentNode());
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
        int possibleActions = root.allUnexpandedActions.size() + root.getChildren().size();
        boolean activeActionConsistentWithMasterDeterminisation = false;


        if (root.getDepth() < treeDepth) {

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

            next = oneStepSelect(root, state, apd);

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
            root.backup(score, apd.getTriggerNode(), apd.getParentNode());
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

            if (!activeActionConsistentWithMasterDeterminisation) {
                if (possibleActions == 1) {
                    nonBranches++;
                    // no need to actually branch, given discard will never impact decision
                } else {
                    for (int i : branchesNeeded) {
                        // we need to branch
                        branches++;
                        AllPlayerDeterminiser newApd = new AllPlayerDeterminiser(apd.getDeterminisationFor(i), i, conv);
                        newApd.setParentNode(root);
                        newApd.applyAndCompatibilise(next, true); // we use the action taken by the active determinisation
                        if (logger.isDebugEnabled())
                            logger.debug(String.format("Launching search for %s", newApd));
                        if (nodeExpanded) {
                            double score = rollout(newApd.getDeterminisationFor(i), next, movesLeft - 1);
                            if (logger.isDebugEnabled())
                                logger.debug(String.format("Backing up a final score of %.2f", score));
                            next.backup(score, apd.getTriggerNode(), root);
                        } else {
                            executeBranchingSearch(agentNextToAct, newApd, next, movesLeft - 1);
                        }
                        // we then need to redeterminise the branched determinisation in apd, so that on the next iteration
                        // the same action might be compatible
                        //               apd.redeterminiseWithinIS(i);
                    }
                }
            }
        }


        if (!activeActionConsistentWithMasterDeterminisation) {
            // we have branched a trajectory using the sampled action and back-propagated it to here
            // (using the concrete determinisation of the active player that is incompatible with the root D).
            // We now need to force-compatibilise the main APD, and continue.
            // This will back-propagate only *above* this node.
            apd.applyAndCompatibilise(next, true);
            apd.setTriggerNode(root);
        } else {
            apd.applyAndCompatibilise(next, false);
        }

        if (nodeExpanded) {
            double score = rollout(apd.getMasterDeterminisation(), next, movesLeft - 1);
            if (logger.isDebugEnabled()) logger.debug(String.format("Backing up a final score of %.2f", score));
            next.backup(score, apd.getTriggerNode(), apd.getParentNode());
        } else {
            int agentNextToAct = (next.getAgentId() + 1) % state.getPlayerCount();
            executeBranchingSearch(agentNextToAct, apd, next, movesLeft - 1);
        }

    }

    public String toString() {
        return String.format("CRIS2-MCST(%s)", rolloutPolicy == null ? "NONE" : rolloutPolicy.toString());
    }
}
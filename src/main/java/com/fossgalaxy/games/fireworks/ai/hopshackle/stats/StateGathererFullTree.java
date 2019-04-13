package com.fossgalaxy.games.fireworks.ai.hopshackle.stats;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSNode;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;

import java.io.*;
import java.util.*;

public class StateGathererFullTree extends StateGatherer implements TreeProcessor {

    protected int VISIT_THRESHOLD;
    protected FileWriter writerCSV;
    protected int MAX_DEPTH;
    protected String filename;

    public StateGathererFullTree(String rules, String conventions, int visitThreshold, int depth) {
        super(rules, conventions);
        VISIT_THRESHOLD = visitThreshold;
        MAX_DEPTH = depth;
        filename = "/TreeData.csv";
    }

    @Override
    public void processTree(MCTSNode root) {
        if (root.getVisits() > VISIT_THRESHOLD) {
            try {
                writerCSV = new FileWriter(fileLocation + filename, true);
                processNode(root);
                writerCSV.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        /*
        System.out.println(String.format("Player %d : %s : %d updates",
                (root.agentId + 1) % root.referenceState.getPlayerCount(),
                root.getBestNode().moveToState,
                recordsWritten));
                */
    }

    protected void processNode(MCTSNode node) {
        if (node.getDepth() > MAX_DEPTH) return;
        for (MCTSNode child : node.getChildren()) {
            if (child.getVisits() >= VISIT_THRESHOLD) {
                // child node associated with action, and the parent state from which this action was taken
                // (or could have been)

                GameState refState = node.getReferenceState();
                if (refState == null) {
                    throw new AssertionError("Should not have a null reference state");
                }

                Hand activehand = refState.getHand(child.getAgentId());
                boolean moveIsLegal;
                // once again, we can't use .isLegal() on the Action, because this is a little buggy
                // and does not use hasCard() as it should
                if (child.getAction() instanceof PlayCard) {
                    int slot = ((PlayCard) child.getAction()).slot;
                    moveIsLegal = activehand.hasCard(slot);
                } else if (child.getAction() instanceof DiscardCard) {
                    int slot = ((DiscardCard) child.getAction()).slot;
                    moveIsLegal = activehand.hasCard(slot) && refState.getInfomation() != refState.getStartingInfomation();
                } else {
                    moveIsLegal = child.getAction().isLegal(child.getAgentId(), refState);
                }
                if (moveIsLegal) // as the move might not be legal from the reference state, but was for some other state that passed through
                    storeData(child, refState, child.getAgentId());

                // it is possible for a child action to be illegal from the reference state
                // due to the specific cards drawn by the other players en-route (as the card drawn by Play or Discard
                // does not affect the transition function / information set differentiation).
                // In addition, at each rollout we have a different determinisation in place for the hand
                // of the root player.
                processNode(child);

            }
        }
    }

    @Override
    public void storeData(MCTSNode node, GameState state, int playerID) {
        // target is the increase in game score from the starting state to game end on taking this action
        double target = (node.getMeanScore() - state.getScore()) / 25.0;
        Map<String, Double> features = extractFeaturesWithRollForward(state, node.getAction(), playerID);
        String csvLine = asCSVLine(features);
        try {
            writerCSV.write(String.format("%.3f\t%s\n", target, csvLine));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


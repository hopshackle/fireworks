package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.state.GameState;

import java.io.*;
import java.util.*;

public class StateGathererFullTree extends StateGatherer {

    private int VISIT_THRESHOLD;
    protected FileWriter writerCSV;
    private int recordsWritten = 0;

    public StateGathererFullTree(int visitThreshold) {
        VISIT_THRESHOLD = visitThreshold;
    }

    public void processTree(MCTSNode root) {
        recordsWritten = 0;
        if (root.visits > VISIT_THRESHOLD) {
            try {
                writerCSV = new FileWriter(fileLocation + "/TreeData.csv", true);
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

    private void processNode(MCTSNode node) {
        for (MCTSNode child : node.children) {
            if (child.visits >= VISIT_THRESHOLD) {
                // child node associated with action, and the parent state from which this action was taken
                // the state on the child is a reference one...so may not be applicable
                if (child.moveToState.isLegal(child.agentId, node.referenceState))
                    storeData(child, node.referenceState, child.agentId);
                // note it is also possible for a child action to be illegal from the reference state
                // dur to the specific hand-determinization for the active player
                processNode(child);
            }
        }
    }

    @Override
    public void storeData(MCTSNode node, GameState state, int playerID) {
        // target is the increase in game score from the starting state to game end on taking this action
        recordsWritten++;
        double target = ((node.score / node.visits) - state.getScore()) / 25.0;
        Map<String, Double> features = extractFeaturesWithRollForward(state, node.moveToState, playerID);
        String csvLine = asCSVLine(features);
        try {
            writerCSV.write(String.format("%.3f\t%s\n", target, csvLine));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


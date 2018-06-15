package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.state.Card;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.Hand;

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

                // the attached state is likeley to be determinised, which means that
                // cards in the active player's hand are also in the deck
                // this is great for probability calculations, but less good for detecting the end of the game
                // for which we have to check the cards left in deck to estimate (due to issues in the state
                // that GameRunner sends us).
                Hand activehand = node.referenceState.getHand(child.agentId);
                Card[] cards = new Card[activehand.getSize()];
                for (int i = 0; i < activehand.getSize(); i++) {
                    if (activehand.hasCard(i)) {
                        cards[i] = activehand.getCard(i);
                        activehand.bindCard(i, null);
                    }
                }

                if (child.moveToState.isLegal(child.agentId, node.referenceState))
                    storeData(child, node.referenceState, child.agentId);

                // and reset hand
                for (int i = 0; i < activehand.getSize(); i++) {
                    if (activehand.hasCard(i)) {
                        activehand.bindCard(i, cards[i]);
                    }
                }
                // note it is also possible for a child action to be illegal from the reference state
                // due to the specific hand-determinization for the active player
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


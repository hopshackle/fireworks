package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.state.*;

import java.io.*;
import java.util.*;

public class StateGathererMonteCarlo extends StateGatherer implements HasGameOverProcessing {

    private List<Map<String, Double>> experienceData = new ArrayList();
    protected FileWriter writerCSV;

    @Override
    public void storeData(MCTSNode node, GameState gameState, int playerID) {
        Map<String, Double> features = extractFeatures(gameState, playerID, true);
        if (logger.isDebugEnabled()) logger.debug(asCSVLine(features));
        experienceData.add(features);
    }


    @Override
    public void onGameOver(double finalScore) {
        openFile();
        try {
            for (Map<String, Double> tuple : experienceData) {
                String csvLine = asCSVLine(tuple);
                double scoreGain = finalScore / 25.0 - tuple.get("SCORE");
                tuple.put("RESULT", scoreGain);
                writerCSV.write(String.format("%.3f\t%s\n", scoreGain, csvLine));
            }
            writerCSV.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void openFile() {
        try {
            writerCSV = new FileWriter(fileLocation + "/StateData.csv", true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.rule.*;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.Action;

import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

public class StateGathererWithTarget extends StateGatherer {

    public static List<String> allTargets = new ArrayList();
    static {
        for (Rule r : MCTSRuleInfoSet.allRules) {
            allTargets.add(r.getClass().getSimpleName());
        }
    }

    public void storeData(GameState gameState, int playerID, List<Rule> rulesTriggered) {
        Map<String, Double> features = extractFeatures(gameState, playerID);
        for (Rule r : rulesTriggered) {
            features.put(r.getClass().getSimpleName(), 1.00 / rulesTriggered.size());
        }
        if (logger.isDebugEnabled()) logger.debug(asCSVLine(features));
        experienceData.add(features);
    }

    @Override
    public void onGameOver(double finalScore) {

        try {
            FileWriter writerCSV = new FileWriter(fileLocation + "/rawData.csv", true);
            for (Map<String, Double> tuple : experienceData) {
                String csvLine = asCSVLine(tuple);
                writerCSV.write(csvLine + "\n");
            }
            writerCSV.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String asCSVLine(Map<String, Double> tuple) {
        String featureString = super.asCSVLine(tuple);
        String targetString = allTargets.stream()
                .map(k -> tuple.getOrDefault(k, 0.00))
                .map(d -> String.format("%.3f", d))
                .collect(Collectors.joining("\t"));
        return targetString + "\t" + featureString;
    }
}

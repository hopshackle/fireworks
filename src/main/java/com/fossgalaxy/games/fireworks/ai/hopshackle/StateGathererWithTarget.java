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

    public void storeData(MCTSNode node, GameState gameState, int playerID) {
        if (node instanceof MCTSRuleNode) {
            Map<String, Double> features = extractFeatures(gameState, playerID);
            MCTSRuleNode ruleNode = (MCTSRuleNode) node;
            List<Rule> rulesTriggered = ruleNode.getRulesForChild(ruleNode.getBestNode(), gameState, playerID);

            for (Rule r : rulesTriggered) {
                features.put(r.getClass().getSimpleName(), 1.00 / rulesTriggered.size());
            }
            if (logger.isDebugEnabled()) logger.debug(asCSVLine(features));
            try {
                FileWriter writerCSV = new FileWriter(fileLocation + "/StateTargetData.csv", true);
                String csvLine = asCSVLineWithTargets(features);
                writerCSV.write(csvLine + "\n");
                writerCSV.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            throw new AssertionError("StateGatherWithTarget requires MCTSRuleNode");
        }

    }

    protected String asCSVLineWithTargets(Map<String, Double> tuple) {
        String featureString = asCSVLine(tuple);
        String targetString = allTargets.stream()
                .map(k -> tuple.getOrDefault(k, 0.00))
                .map(d -> String.format("%.3f", d))
                .collect(Collectors.joining("\t"));
        return targetString + "\t" + featureString;
    }
}

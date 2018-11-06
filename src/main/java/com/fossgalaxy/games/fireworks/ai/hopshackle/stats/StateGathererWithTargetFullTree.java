package com.fossgalaxy.games.fireworks.ai.hopshackle.stats;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSNode;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSRuleInfoSet;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSRuleNode;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StateGathererWithTargetFullTree extends StateGathererFullTree {

    public static List<String> allTargets = new ArrayList();

    static {
        for (Rule r : MCTSRuleInfoSet.allRules) {
            allTargets.add(r.getClass().getSimpleName());
        }
    }

    public StateGathererWithTargetFullTree(int visitThreshold, int depth) {
        super(visitThreshold, depth);
        filename = "/TreeTargetData.csv";
    }

    protected void processNode(MCTSNode node) {
        if (node.getDepth() > MAX_DEPTH || node.getVisits() < VISIT_THRESHOLD) return;
        int agentID = (node.getAgentId() + 1) % node.getReferenceState().getPlayerCount();
        storeData(node, node.getReferenceState(), agentID);
        for (MCTSNode child : node.getChildren()) {
            processNode(child);
        }
    }


    @Override
    public void storeData(MCTSNode node, GameState gameState, int playerID) {
        if (node instanceof MCTSRuleNode) {
            Map<String, Double> features = extractFeatures(gameState, playerID, true);
            MCTSRuleNode ruleNode = (MCTSRuleNode) node;
            List<Rule> rulesTriggered = ruleNode.getRulesForChild(ruleNode.getBestNode(), gameState, playerID);

            if (rulesTriggered.isEmpty()) {
          //      System.out.println("No Rules triggered at depth " + node.getDepth());
                return;
            }
            for (Rule r : rulesTriggered) {
                features.put(r.getClass().getSimpleName(), 1.00 / rulesTriggered.size());
            }
            if (logger.isDebugEnabled()) logger.debug(asCSVLine(features));
            try {
                String csvLine = asCSVLineWithTargets(features);
                writerCSV.write(csvLine + "\n");
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

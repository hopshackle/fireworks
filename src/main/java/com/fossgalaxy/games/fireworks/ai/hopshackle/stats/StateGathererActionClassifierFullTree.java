package com.fossgalaxy.games.fireworks.ai.hopshackle.stats;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSNode;
import com.fossgalaxy.games.fireworks.state.GameState;
import org.apache.commons.math3.distribution.NormalDistribution;

import java.util.Map;

public class StateGathererActionClassifierFullTree extends StateGathererFullTree {

    private NormalDistribution Z = new NormalDistribution();

    public StateGathererActionClassifierFullTree(String rules, String conventions, int visitThreshold, int depth) {
        super(rules, conventions, visitThreshold, depth);
        filename = "/TreeActionData.csv";
    }

    @Override
    public void storeData(MCTSNode node, GameState gameState, int playerID) {
        double bestScore = node.getBestNode().rolloutScores.getMean();
        double bestN = node.getBestNode().rolloutScores.getN();
        double bestVar = Math.pow(node.getBestNode().rolloutScores.getStdDev(), 2);
        if (bestN < 50) return;

        try {
            for (MCTSNode child : node.getChildren()) {
                double childScore = child.getMeanScore();
                double childN = child.getVisits();
                if (childN < 20) continue;
                double childVar = Math.pow(child.rolloutScores.getStdDev(), 2);

                if (bestVar == 0.0 || childVar == 0.0) continue;
                double statistic = (bestScore - childScore) / Math.sqrt(bestVar / bestN + childVar / childN);
                double score = 2.0 * Z.cumulativeProbability(-statistic);
                if (Double.isNaN(score)) {
                    throw new AssertionError("Not a Number in calculation");
                }
                Map<String, Double> features = extractFeaturesWithRollForward(gameState, child.getAction(), playerID);
                if (logger.isDebugEnabled()) {
                    logger.debug(String.format("Action %s has value %.3f\n", child.getAction(), score));
                    logger.debug(asCSVLine(features));
                }
                String csvLine = asCSVLine(features);
                writerCSV.write(String.format("%.3f\t%s\n", score, csvLine));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

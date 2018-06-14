package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.StandardizeSerializerStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.javatuples.*;

import java.util.*;
import java.util.stream.*;

public class ClassifierFnAgent implements Agent {

    private Logger logger = LoggerFactory.getLogger(ClassifierFnAgent.class);
    private MultiLayerNetwork model;
    private NormalizerStandardize normalizer;
    private double temperature;
    private boolean debug = true;
    private Random rand = new Random(47);

    @AgentConstructor("classFn")
    public ClassifierFnAgent(String modelLocation, double temp) {
        temperature = temp;
        //Load the model
        try {
            model = ModelSerializer.restoreMultiLayerNetwork(modelLocation);
            NormalizerSerializer ns = new NormalizerSerializer();
            ns.addStrategy(new StandardizeSerializerStrategy());
            normalizer = ns.restore(modelLocation + ".normal");
        } catch (Exception e) {
            System.out.println("Error when reading in Model from " + modelLocation + ": " + e.toString());
            e.printStackTrace();
        }
    }

    public ClassifierFnAgent(MultiLayerNetwork model, NormalizerStandardize normalizer) {
        this.model = model;
        this.normalizer = normalizer;
    }

    @Override
    public Action doMove(int agentID, GameState gameState) {
        /*
            1) Determine which Rule to trigger
            2) Trigger it
         */

        Map<Action, Double> actionValues = getAllActionValues(agentID, gameState);

        final double LV = actionValues.values().stream().mapToDouble(i -> i).max().getAsDouble();

        actionValues.keySet().forEach(
                k -> {
                    double newValue = Math.exp((actionValues.get(k) - LV) / temperature);
                    actionValues.put(k, newValue);
                }
        );

        double total = actionValues.values().stream().reduce(0.0, Double::sum);

        actionValues.keySet().forEach(
                k -> {
                    double newValue = actionValues.get(k) / total;
                    actionValues.put(k, newValue);
                }
        );

        double cdfRoll = rand.nextDouble();

        if (debug) {
            String logMessage = actionValues.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .map(e -> String.format("\nAction: %s\tProb: %.2f",
                            e.getKey().toString(), e.getValue()))
                    .collect(Collectors.joining());
            logger.debug(logMessage);
            logger.debug(String.format("Random roll is %.3f", cdfRoll));
        }

        for (Action a : actionValues.keySet()) {
            if (cdfRoll <= actionValues.get(a)) {
                Action chosenAction = a;
                if (debug) {
                    logger.debug("Selected action " + a);
                }
                return chosenAction;
            }
            cdfRoll -= actionValues.get(a);
        }
        throw new AssertionError("Should not be able to reach this point");
    }

    public Map<Action, Double> getAllActionValues(int agentID, GameState gameState) {

        int numberOfRules = MCTSRuleInfoSet.allRules.size();
        double[] ruleValues = valueState(gameState, agentID);

        if (debug) {
            logger.debug("Considering move for Player " + agentID);
            String logMessage = IntStream.range(0, numberOfRules)
                    .mapToObj(i -> String.format("\nValue: %2.2f\tRule: %25s\tAction: %s",
                            ruleValues[i],
                            MCTSRuleInfoSet.allRules.get(i).getClass().getSimpleName(),
                            MCTSRuleInfoSet.allRules.get(i).execute(agentID, gameState)))
                    .collect(Collectors.joining());
            logger.debug(logMessage);
        }

        List<Pair<Action, Double>> actionsAndValues = MCTSRuleInfoSet.allRules.stream()
                .map(r -> new Pair<>(r.execute(agentID, gameState), ruleValues[MCTSRuleInfoSet.allRules.indexOf(r)]))
                .collect(Collectors.toList());

        Map<Action, List<Pair<Action, Double>>> validActionDetails = actionsAndValues.stream()
                .filter(p -> p.getValue0() != null)
                .filter(p -> {
                    // this section should use Action.isLegal(). But that is broken for Play and Discard
                    if (p.getValue0() instanceof PlayCard) {
                        int slot = ((PlayCard) p.getValue0()).slot;
                        return gameState.getHand(agentID).hasCard(slot);
                    } else if (p.getValue0() instanceof DiscardCard) {
                        int slot = ((DiscardCard) p.getValue0()).slot;
                        return gameState.getHand(agentID).hasCard(slot) && gameState.getInfomation() != gameState.getStartingInfomation();
                    } else {
                        return gameState.getInfomation() != 0;
                    }
                })
                .collect(Collectors.groupingBy(Pair::getValue0));

        double[] actionValues = new double[validActionDetails.size()];
        Action[] actions = new Action[validActionDetails.size()];
        int count = 0;
        double largestValue = Double.NEGATIVE_INFINITY;
        for (Action key : validActionDetails.keySet()) {
            double sum = 0.00;
            for (Pair<Action, Double> p : validActionDetails.get(key))
                sum += p.getValue1();
            if (sum > largestValue) largestValue = sum;
            actionValues[count] = sum;
            actions[count] = key;
            count++;
        }

        Map<Action, Double> retValue = new HashMap<>();
        for (int i = 0; i < actionValues.length; i++) {
            retValue.put(actions[i], actionValues[i]);
        }
        return retValue;
    }

    public double[] valueState(GameState state, int agentID) {
        Map<String, Double> features = StateGatherer.extractFeatures(state, agentID);
        INDArray featureRepresentation = StateGatherer.featuresToNDArray(features);
        if (debug) {
            logger.debug(featureRepresentation.toString());
        }
        normalizer.transform(featureRepresentation);
        INDArray output = model.output(featureRepresentation);
        return output.toDoubleVector();
    }
}

package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
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

public class QFnAgent implements Agent {

    private Logger logger = LoggerFactory.getLogger(QFnAgent.class);
    private MultiLayerNetwork model;
    private NormalizerStandardize normalizer;
    private double temperature = 0.1;
    private boolean debug = true;
    private Random rand = new Random(47);

    @AgentConstructor("QFn")
    public QFnAgent(String modelLocation) {
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

    public QFnAgent(MultiLayerNetwork model, NormalizerStandardize normalizer) {
        this.model = model;
        this.normalizer = normalizer;
    }

    @Override
    public Action doMove(int agentID, GameState gameState) {
        /*
            1) Determine which Rule to trigger
            2) Trigger it
         */

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

        Map<Action, List<Pair<Action, Double>>> validActionDetails = MCTSRuleInfoSet.allRules.stream()
                .map(r -> new Pair<>(r.execute(agentID, gameState), ruleValues[MCTSRuleInfoSet.allRules.indexOf(r)]))
                .filter(p -> p.getValue0() != null)
                .filter(p -> p.getValue0().isLegal(agentID, gameState))
                .collect(Collectors.groupingBy(Pair::getValue0));

        double[] actionValues = new double[validActionDetails.size()];
        Action[] actions = new Action[validActionDetails.size()];
        int count = 0;
        for (Action key : validActionDetails.keySet()) {
            double sum = 0.00;
            for (Pair<Action, Double> p : validActionDetails.get(key))
                sum += p.getValue1();
            actionValues[count] = sum;
            actions[count] = key;
            count++;
        }

        List<Double> temppdf = Arrays.stream(actionValues)
                .mapToObj(v -> Math.exp(v / temperature))
                .collect(Collectors.toList());

        double total = temppdf.stream().reduce(0.0, Double::sum);
        List<Double> pdf = temppdf.stream()
                .map(d -> d / total)
                .collect(Collectors.toList());

        double cdfRoll = rand.nextDouble();

        if (debug) {
            String logMessage = IntStream.range(0, actions.length)
                    .filter(i -> pdf.get(i) > 0.00001)
                    .mapToObj(i -> String.format("\nAction: %s\tValue: %2.2f\tProb: %.2f",
                            actions[i].toString(), actionValues[i], pdf.get(i)))
                    .collect(Collectors.joining());
            logger.debug(logMessage);
            logger.debug(String.format("Random roll is %.3f", cdfRoll));
        }

        for (int i = 0; i < actions.length; i++) {
            if (cdfRoll <= pdf.get(i)) {
                Action chosenAction = actions[i];
                if (debug) {
                    logger.debug("Selected action " + actions[i]);
                }
                return chosenAction;
            }
            cdfRoll -= pdf.get(i);
        }
        throw new AssertionError("Should not be able to reach this point");
    }

    private double[] valueState(GameState state, int agentID) {
        INDArray featureRepresentation = StateGatherer.extractFeaturesAsNDArray(state, null, agentID);
        if (debug) {
            logger.debug(featureRepresentation.toString());
        }
        normalizer.transform(featureRepresentation);
        INDArray output = model.output(featureRepresentation);
        return output.toDoubleVector();
    }
}

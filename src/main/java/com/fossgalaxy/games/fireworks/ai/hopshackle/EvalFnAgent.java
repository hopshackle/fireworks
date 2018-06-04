package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.javatuples.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.dataset.*;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.StandardizeSerializerStrategy;
import org.slf4j.*;

import java.util.*;
import java.util.stream.*;

public class EvalFnAgent implements Agent {

    private Logger logger = LoggerFactory.getLogger(EvalFnAgent.class);
    private MultiLayerNetwork model;
    private NormalizerStandardize normalizer;
    private double temperature = 0.1;
    private boolean debug = true;
    private Random rand = new Random(47);

    @AgentConstructor("evalFn")
    public EvalFnAgent(String modelLocation) {
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

    public EvalFnAgent(MultiLayerNetwork model, NormalizerStandardize normalizer) {
        this.model = model;
        this.normalizer = normalizer;
    }

    @Override
    public Action doMove(int agentID, GameState gameState) {
        /*
            1) Obtain the set of actions to be considered
            2) For each roll the state forward, extract features, and value it using the model
            3) Choose an action based on a Boltzmann distribution
         */

        List<Action> actionsToBeConsidered = getPossibleActions(agentID, gameState);
        List<Double> actionValues = actionsToBeConsidered.stream()
                .map(a -> new Pair(a, rollForward(a, agentID, gameState)))
                .map(p -> valueState(((GameState)p.getValue1()), ((Action)p.getValue0()), agentID))
                .collect(Collectors.toList());

        List<Double> temppdf = actionValues.stream()
                .map(v -> Math.exp(v / temperature))
                .collect(Collectors.toList());

        double total = temppdf.stream().reduce(0.0, Double::sum);
        List<Double> pdf = temppdf.stream()
                .map(d -> d / total)
                .collect(Collectors.toList());

        double cdfRoll = rand.nextDouble();

        if (debug) {
            logger.debug("Considering move for Player " + agentID);
            String logMessage = IntStream.range(0, actionsToBeConsidered.size())
                    .mapToObj(i -> String.format("\nAction: %s\tValue: %2.2f\tProb: %.2f",
                            actionsToBeConsidered.get(i).toString(), actionValues.get(i), pdf.get(i)))
                    .collect(Collectors.joining());
            logger.debug(logMessage);
            logger.debug(String.format("Random roll is %.3f", cdfRoll));
        }

        for (int i = 0; i < actionsToBeConsidered.size(); i++) {
            if (cdfRoll <= pdf.get(i)) {
                if (debug) logger.debug("Selected action " + actionsToBeConsidered.get(i).toString());
                return actionsToBeConsidered.get(i);
            }
            cdfRoll -= pdf.get(i);
        }
        throw new AssertionError("Should not be able to reach this point");
    }

    private List<Action> getPossibleActions(int agentID, GameState state) {
        List<Action> retValue = MCTSRuleInfoSet.allRules.stream()
                .filter(r -> r.canFire(agentID, state))
                .map(r -> r.execute(agentID, state))
                .filter(p -> {
                    // this section should use Action.isLegal(). But that is broken for Play and Discard
                    if (p instanceof PlayCard) {
                        int slot = ((PlayCard) p).slot;
                        return state.getHand(agentID).hasCard(slot);
                    } else if (p instanceof DiscardCard) {
                        int slot = ((DiscardCard) p).slot;
                        return state.getHand(agentID).hasCard(slot) && state.getInfomation() != state.getStartingInfomation();
                    } else {
                        return state.getInfomation() != 0;
                    }
                })
                .distinct()
                .collect(Collectors.toList());
        return retValue;
    }

    public static GameState rollForward(Action action, int agentID, GameState state) {
        if(action instanceof PlayCard || action instanceof DiscardCard) return state;
        // we don't roll forward Play or Discard actions, as that introduces determinised info
        // instead we cater for this in the feature representation
        GameState stateCopy = state.getCopy();
        action.apply(agentID, stateCopy);
        return stateCopy;
    }

    private double valueState(GameState state, Action action, int agentID) {
        Map<String, Double> features = StateGatherer.extractFeatures(state, agentID);
        features.putAll(StateGatherer.extractActionFeatures(action, state, agentID));
        INDArray featureRepresentation = StateGatherer.featuresToNDArray(features);
        if (debug) {
            logger.debug(featureRepresentation.toString());
        }
        normalizer.transform(featureRepresentation);
        INDArray output = model.output(featureRepresentation);
        return output.getDouble(0);
    }
}

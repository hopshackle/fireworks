package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.events.GameEvent;
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
import java.util.function.Function;
import java.util.stream.*;

public class EvalFnAgent implements Agent {

    private Logger logger = LoggerFactory.getLogger(EvalFnAgent.class);
    private MultiLayerNetwork model;
    private NormalizerStandardize normalizer;
    private double temperature;
    private boolean debug = true;
    private Random rand = new Random(47);

    @AgentConstructor("evalFn")
    public EvalFnAgent(String modelLocation, double temp) {
        //Load the model
        temperature = temp;
        debug = logger.isDebugEnabled();
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
        Map<Action, Double> actionValues = getAllActionValues(agentID, gameState);
        if (temperature < 1e-4) {
            // we just pick the best
            return actionValues.entrySet().stream()
                    .max(Comparator.comparing(Map.Entry::getValue))
                    .get().getKey();
        }
        // otherwise we calculate a pdf

        double largestValue = actionValues.values().stream().mapToDouble(i -> i).max().getAsDouble();
        List<Action> actionsToBeConsidered = actionValues.keySet().stream().collect(Collectors.toList());
        List<Double> temppdf = actionsToBeConsidered.stream()
                .map(k -> Math.exp((actionValues.get(k) - largestValue) / temperature))
                .collect(Collectors.toList());

        double total = temppdf.stream().reduce(0.0, Double::sum);
        List<Double> pdf = temppdf.stream()
                .map(d -> d / total)
                .collect(Collectors.toList());

        double cdfRoll = rand.nextDouble();

        if (debug) {
            logger.debug("Considering move for Player " + agentID);
            String logMessage = actionsToBeConsidered.stream()
                    .map(a -> String.format("\nAction: %s\tValue: %2.2f\tProb: %.2f",
                            a.toString(), actionValues.get(a) * 25.0, pdf.get(actionsToBeConsidered.indexOf(a))))
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
        //      List<Action> retValue = Utils.generateSuitableActions(agentID, state).stream()
        //              .distinct().collect(Collectors.toList());
        List<Action> retValue = MCTSRuleInfoSet.allRules.stream()
                .filter(r -> r.canFire(agentID, state))
                .map(r -> r.execute(agentID, state))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());


        return retValue.stream().filter(p -> {
            // this section should use Action.isLegal(). But that is broken for Play and Discard
            // as it uses hand.getCard() != null, which will always be true for the acting player
            // when we use the state provided by GameRunsner
            if (p instanceof PlayCard) {
                int slot = ((PlayCard) p).slot;
                return state.getHand(agentID).hasCard(slot);
            } else if (p instanceof DiscardCard) {
                int slot = ((DiscardCard) p).slot;
                return state.getHand(agentID).hasCard(slot) && state.getInfomation() != state.getStartingInfomation();
            } else {
                return state.getInfomation() != 0;
            }
        }).collect(Collectors.toList());
    }

    public static GameState rollForward(Action action, int agentID, GameState state) {
        if (action instanceof PlayCard || action instanceof DiscardCard) return state;
        // we don't roll forward Play or Discard actions, as that introduces determinised info
        // instead we cater for this in the feature representation
        GameState stateCopy = state.getCopy();
        List<GameEvent> events = action.apply(agentID, stateCopy);
        events.forEach(stateCopy::addEvent);
        return stateCopy;
    }

    public double valueState(GameState state, Optional<Action> action, int agentID) {
        Map<String, Double> features = StateGatherer.extractFeatures(state, agentID);
        if (action.isPresent())
            features.putAll(StateGatherer.extractActionFeatures(action.get(), state, agentID));
        INDArray featureRepresentation = StateGatherer.featuresToNDArray(features);
        if (debug) {
            logger.debug(featureRepresentation.toString());
        }
        normalizer.transform(featureRepresentation);
        INDArray output = model.output(featureRepresentation);
        return output.getDouble(0);
    }


    public Map<Action, Double> getAllActionValues(int agentID, GameState gameState) {
        List<Action> actionsToBeConsidered = getPossibleActions(agentID, gameState);
        List<Double> actionValues = actionsToBeConsidered.stream()
                .map(a -> new Pair(a, rollForward(a, agentID, gameState)))
                .map(p -> valueState(((GameState) p.getValue1()), Optional.ofNullable((Action) p.getValue0()), agentID))
                .collect(Collectors.toList());
        Map<Action, Double> retValue = new HashMap<>();
        IntStream.range(0, actionsToBeConsidered.size())
                .forEach(i -> retValue.put(actionsToBeConsidered.get(i), actionValues.get(i)));
        return retValue;
    }
}

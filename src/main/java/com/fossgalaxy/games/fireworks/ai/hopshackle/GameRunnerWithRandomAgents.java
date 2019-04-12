package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSRuleInfoSet;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StateGatherer;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StateGathererWithTarget;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.fossgalaxy.games.fireworks.state.events.*;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;

import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A runner for Hanabi that pitches random agents against each other to generate data from which we can learn
 * to infer the type of an agent from the actions they take
 */
public class GameRunnerWithRandomAgents extends GameRunner {

    Random rnd = new Random();
    int[] agentIndicesByPlayer;

    public static String[] agentDescriptors = new String[]{
            "clivej2",
            "legal_random",
            "cautious",
            "flawed",
            "piers",
            "risky2[0.7]",
            "vdb-paper",
            "evalFn[RESPlayers_5.params:0.0:true]",
            "evalFn[RESPlayers_5.params:0.0:false]",
            "iggi2",
            "outer"
    };

    protected Agent[] agents = new Agent[agentDescriptors.length];

    /**
     * Create a game runner with a given ID and number of players.
     *
     * @param gameID          the ID of the game
     * @param expectedPlayers the number of players we expect to be playing.
     */
    public GameRunnerWithRandomAgents(String gameID, int expectedPlayers) {
        this(gameID, new BasicState(HAND_SIZE[expectedPlayers], expectedPlayers));
    }

    public GameRunnerWithRandomAgents(String gameID, GameState state) {
        super(gameID, state);
        for (int i = 0; i < agentDescriptors.length; i++) {
            agents[i] = AgentUtils.buildAgent(agentDescriptors[i]);
        }
        agentIndicesByPlayer = new int[state.getPlayerCount()];
        for (int i = 0; i < state.getPlayerCount(); i++)
            agentIndicesByPlayer[i] = -1;       // to check they are all populated
    }

    /**
     * Add a player to the game.
     * <p>
     * This should not be attempted once the game has started.
     *
     * @param player the player to add to the game
     */
    public void addPlayer(Player player) {
        logger.info("player {} is {}", nPlayers, player);
        players[nPlayers++] = Objects.requireNonNull(player);
    }

    public void addRandomPlayer(int totalPlayers) {
        int roll = rnd.nextInt(agentDescriptors.length);
        agentIndicesByPlayer[nPlayers] = roll;
        addPlayer(new HopshackleAgentPlayer(agentDescriptors[roll], agents[roll]));
    }

    /**
     * Ask the next player for their move.
     */
    protected void nextMove() {
        Player player = players[nextPlayer];
        assert player != null : "that player is not valid";

        logger.debug("asking player {} for their move", nextPlayer);
        long startTime = getTick();

        //get the action and try to apply it
        Action action = player.getAction();

        long endTime = getTick();
        logger.debug("agent {} took {} ms to make their move", nextPlayer, endTime - startTime);
        logger.debug("move {}: player {} made move {}", moves, nextPlayer, action);

        //if the more was illegal, throw a rules violation
        if (!action.isLegal(nextPlayer, state)) {
            throw new RulesViolation(action);
        }

        // store this as a datapoint
        GameState playerState = ((HopshackleAgentPlayer) player).getGameState();
        Map<String, Double> features = StateGatherer.extractFeatures(playerState, nextPlayer, true);
        Map<String, Double> featuresBase = StateGatherer.extractFeatures(playerState, nextPlayer, false);
        List<Rule> rulesTriggered = getRulesThatTriggered(action, playerState, nextPlayer);

        for (Rule r : rulesTriggered) {
            features.put(r.getClass().getSimpleName(), 1.00);
        }
        if (action instanceof PlayCard) features.put("PLAY_CARD", 1.00);
        if (action instanceof DiscardCard) features.put("DISCARD_CARD", 1.00);

        try {
            FileWriter writerCSV = new FileWriter("hanabi/OpponentData.csv", true);
            String csvLine = asCSVLineWithTargets(features, featuresBase, agentIndicesByPlayer[nextPlayer]);
            writerCSV.write(csvLine + "\n");
            writerCSV.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //perform the action and get the effects
        logger.info("player {} made move {} as turn {}", nextPlayer, action, moves);
        moves++;
        Collection<GameEvent> events = action.apply(nextPlayer, state);
        events.forEach(this::send);

        //make sure it's the next player's turn
        nextPlayer = (nextPlayer + 1) % players.length;
    }

    public static List<Rule> getRulesThatTriggered(Action action, GameState fromState, int agentID) {
        return MCTSRuleInfoSet.masterRuleMap.values().stream()
                .filter(r -> {
                    Action a = r.execute(agentID, fromState);
                    if (a == null) return false;
                    return (a.equals(action));
                })
                .collect(Collectors.toList());
    }

    protected String asCSVLine(Map<String, Double> tuple, Map<String, Double> tuple2) {
        return StateGatherer.allFeatures.stream()
                .map(k -> tuple.getOrDefault(k, 0.00))
                .map(d -> String.format("%.3f", d))
                .collect(Collectors.joining("\t"))
                + "\t" +
                StateGatherer.allFeatures.stream()
                        .map(k -> tuple2.getOrDefault(k, 0.00))
                        .map(d -> String.format("%.3f", d))
                        .collect(Collectors.joining("\t"));
    }

    protected String asCSVLineWithTargets(Map<String, Double> tuple, Map<String, Double> tuple2, int agentType) {
        String featureString = asCSVLine(tuple, tuple2);
        String ruleString = StateGathererWithTarget.allTargets.stream()
                .map(k -> tuple.getOrDefault(k, 0.0))
                .map(d -> String.format("%.3f", d))
                .collect(Collectors.joining("\t"));
        String targetString = IntStream.range(0, agentDescriptors.length)
                .mapToObj(i -> i == agentType ? "1" : "0")
                .collect(Collectors.joining("\t"));
        return targetString + "\t" + ruleString + "\t" + featureString;
    }

}

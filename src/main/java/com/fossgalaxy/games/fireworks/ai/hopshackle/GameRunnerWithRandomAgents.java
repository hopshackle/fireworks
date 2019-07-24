package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSRuleInfoSet;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.RuleGenerator;
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

    public static String[] agentDescriptors = new String[]{
            "clivej2",
            "legal_random",
            "cautious",
            "flawed",
            "piers",
            "risky2[0.7]",
            "vdb-paper",
            "evalFn[RESPlayers_5.params:0:1|2|3|4|6|7|8|9|10|12|15:NN]",
            "evalFn[RESPlayers_5.params:0:1|2|3|4|6|7|8|9|10|12|15:YN]",
            "iggi2",
            "outer"
    };


    protected Agent[] agents = new Agent[agentDescriptors.length];
    private Random rnd = new Random();
    private int[] agentIndicesByPlayer;
    public static List<Rule> rulesToTrackBase = RuleGenerator.generateRules("2|3|4|6|7|8|9|12|13|15|18", "NN");
    public static List<Rule> rulesToTrackConv = RuleGenerator.generateRules("1|2|3|4|9|12|13|15|18", "YN");
    public static StateGathererWithTarget stateGathererBase = new StateGathererWithTarget("2|3|4|6|7|8|9|12|13|15|18", "NN");
    public static StateGathererWithTarget stateGathererConv  = new StateGathererWithTarget("1|2|3|4|9|12|13|15|18", "YN");
    public static List<String> allFeatures = new ArrayList();

    static {
        allFeatures.addAll(StateGatherer.allFeatures);
        allFeatures.addAll(rulesToTrackBase.stream()
                .map(r -> r.getClass().getSimpleName())
                .collect(Collectors.toList()));
        allFeatures.addAll(rulesToTrackConv.stream()
                .map(r -> r.getClass().getSimpleName())
                .filter(name -> !allFeatures.contains(name))
                .collect(Collectors.toList()));
    }

    /**
     * Create a game runner with a given ID and number of players.
     *
     * @param gameID          the ID of the game
     * @param expectedPlayers the number of players we expect to be playing.
     */
    public GameRunnerWithRandomAgents(String gameID, int expectedPlayers) {
        this(gameID, new BasicState(HAND_SIZE[expectedPlayers], expectedPlayers));
    }

    private GameRunnerWithRandomAgents(String gameID, GameState state) {
        super(gameID, state);
        for (int i = 0; i < agentDescriptors.length; i++) {
            agents[i] = AgentUtils.buildAgent(agentDescriptors[i]);
        }
        agentIndicesByPlayer = new int[state.getPlayerCount()];
        for (int i = 0; i < state.getPlayerCount(); i++)
            agentIndicesByPlayer[i] = -1;       // to check they are all populated
    }

    public void addPlayer(Player player, String name) {
        super.addPlayer(player);
        playerNames.add(name);
    }

    public int addRandomPlayer() {
        int roll = rnd.nextInt(agentDescriptors.length);
        agentIndicesByPlayer[nPlayers] = roll;
        addPlayer(new HopshackleAgentPlayer(agentDescriptors[roll], agents[roll]), agentDescriptors[roll]);
        if (playerNames.size() != nPlayers)
            throw new AssertionError("WTF");
        return roll;
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
        Map<String, Double> features = stateGathererBase.extractFeatures(playerState, nextPlayer);
        Map<String, Double> featuresConv = stateGathererConv.extractFeatures(playerState, nextPlayer);
        List<Rule> rulesTriggeredBase = getRulesThatTriggered(rulesToTrackBase, action, playerState, nextPlayer);
        List<Rule> rulesTriggeredConv = getRulesThatTriggered(rulesToTrackConv, action, playerState, nextPlayer);

        for (Rule r : rulesToTrackBase) {
            features.put(r.getClass().getSimpleName(), rulesTriggeredBase.contains(r) ? 1.00 : 0.00);
        }
        for (Rule r : rulesToTrackConv) {
            featuresConv.put(r.getClass().getSimpleName(), rulesTriggeredConv.contains(r) ? 1.00 : 0.00);
        }
        if (action instanceof PlayCard) features.put("PLAY_CARD", 1.00);
        if (action instanceof DiscardCard) features.put("DISCARD_CARD", 1.00);

        try {
            FileWriter writerCSV = new FileWriter("hanabi/OpponentData.csv", true);
            String csvLine = asCSVLineWithTargets(features, featuresConv, agentIndicesByPlayer[nextPlayer]);
            writerCSV.write(csvLine + "\n");
            writerCSV.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //perform the action and get the effects
        logger.info("player {} made move {} as turn {}", nextPlayer, action, moves);
        moves++;
        Collection<GameEvent> events = action.apply(nextPlayer, state);
        notifyAction(nextPlayer, action, events);

        //make sure it's the next player's turn
        nextPlayer = (nextPlayer + 1) % players.length;
    }

    public static List<Rule> getRulesThatTriggered(List<Rule> allRules, Action action, GameState fromState, int agentID) {
        return allRules.stream()
                .filter(r -> {
                    Action a = r.execute(agentID, fromState);
                    if (a == null) return false;
                    return (a.equals(action));
                })
                .collect(Collectors.toList());
    }

    protected String asCSVLine(Map<String, Double> tuple, Map<String, Double> tuple2) {
        return allFeatures.stream()
                .map(k -> tuple.getOrDefault(k, 0.00))
                .map(d -> String.format("%.3f", d))
                .collect(Collectors.joining("\t"))
                + "\t" +
                allFeatures.stream()
                        .map(k -> tuple2.getOrDefault(k, 0.00))
                        .map(d -> String.format("%.3f", d))
                        .collect(Collectors.joining("\t"));
    }

    protected String asCSVLineWithTargets(Map<String, Double> tuple, Map<String, Double> tuple2, int agentType) {
        String featureString = asCSVLine(tuple, tuple2);
        String targetString = IntStream.range(0, agentDescriptors.length)
                .mapToObj(i -> i == agentType ? "1" : "0")
                .collect(Collectors.joining("\t"));
        return targetString + "\t" + featureString;
    }

}

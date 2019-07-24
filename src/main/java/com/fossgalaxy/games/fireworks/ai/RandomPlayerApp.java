package com.fossgalaxy.games.fireworks.ai;

import com.fossgalaxy.games.fireworks.ai.hopshackle.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSOppModelRollout;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.BasicStats;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.GameStats;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StatsCollator;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StatsSummary;
import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Game runner for testing.
 * <p>
 * This will run a bunch of games with your agent so you can see how it does.
 */
public class RandomPlayerApp {

    public static void main(String[] args) {
        int numGames = (args.length < 1) ? 1 : Integer.valueOf(args[0]);
        boolean reuseAgents = args.length > 2;
        if (args.length < 2) {
            runGamesAndLogResults(numGames, "", reuseAgents);
        } else {
            runGamesAndLogResults(numGames, args[1], reuseAgents);
        }
    }

    private static void runGamesAndLogResults(int numGames, String agentDescriptor, boolean reuseAgents) {

        Random random = new Random();
        StatsSummary scoreSummary = new BasicStats();
        StatsSummary timeSummary = new BasicStats();
        StatsCollator.clear();

        for (int i = 0; i < numGames; i++) {
            //         System.out.println("Game " + i + " starting");
            int numPlayers = random.nextInt(4) + 2;
            GameRunnerWithRandomAgents runner = new GameRunnerWithRandomAgents("test-game", numPlayers);

            int overridePlayerNumber = agentDescriptor.equals("") ? -1 : random.nextInt(numPlayers);
            //add your agents to the game
            Agent a = (overridePlayerNumber > 0) ? AgentUtils.buildAgent(agentDescriptor) : null;
            int[] agents = new int[numPlayers];
            for (int j = 0; j < numPlayers; j++) {
                if (j == overridePlayerNumber) {
                    Player player = new HopshackleAgentPlayer(agentDescriptor, a);
                    runner.addPlayer(player);
                } else {
                    agents[j] = runner.addRandomPlayer();
                }
            }

            GameStats stats = runner.playGame(random.nextLong());
            scoreSummary.add(stats.score);
            timeSummary.add((double) stats.time / (double) stats.moves);
            System.out.println(String.format("Game %3d finished with score of %2d and %.0f ms per move", i, stats.score, (double) stats.time / stats.moves));
   /*         if (a instanceof MCTSOppModelRollout) {
                MCTSOppModelRollout reportingAgent = (MCTSOppModelRollout) a;
                List<Map<Integer, Double>> beliefs = reportingAgent.getCurrentOpponentBeliefs();
                for (int player = 0; player < numPlayers; player++) {
                    if (player == overridePlayerNumber) continue;
                    System.out.println(String.format("\tPlayer %d beliefs (Actual is %2d):", player, agents[player]));
                    beliefs.get(player).entrySet().stream()
                            .filter(e -> e.getValue() > 0.01)
                            .forEach(e -> System.out.println(String.format("\t\t%.2f\t%2d", e.getValue(), e.getKey())));
                }
            } */
        }

        //print out the stats
        System.out.println(String.format("Score Avg: %.2f, min: %.0f, max: %.0f, std err: %.2f, Time per move: %.1f ms",
                scoreSummary.getMean(),
                scoreSummary.getMin(),
                scoreSummary.getMax(),
                scoreSummary.getStdErr(),
                timeSummary.getMean()));

        System.out.println(StatsCollator.summaryString());
    }
}

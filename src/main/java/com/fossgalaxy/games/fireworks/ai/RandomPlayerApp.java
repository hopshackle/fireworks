package com.fossgalaxy.games.fireworks.ai;

import com.fossgalaxy.games.fireworks.ai.hopshackle.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.BasicStats;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.GameStats;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StatsCollator;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StatsSummary;
import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;

import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * Game runner for testing.
 * <p>
 * This will run a bunch of games with your agent so you can see how it does.
 */
public class RandomPlayerApp {

    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss 'on' dd-LLL");

    public static void main(String[] args) {
        int numGames = (args.length < 1) ? 1 : Integer.valueOf(args[0]);
        if (args.length < 2) {
            runGamesAndLogResults(numGames, "");
        } else {
            runGamesAndLogResults(numGames, args[1]);
        }
    }

    private static void runGamesAndLogResults(int numGames, String agentDescriptor) {

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
            for (int j = 0; j < numPlayers; j++) {
                if (j == overridePlayerNumber) {
                    Agent a = AgentUtils.buildAgent(agentDescriptor);
                    Player player = new HopshackleAgentPlayer(agentDescriptor, a);
                    runner.addPlayer(player);
                } else {
                    runner.addRandomPlayer(numPlayers);
                }
            }

            GameStats stats = runner.playGame(random.nextLong());
            scoreSummary.add(stats.score);
            timeSummary.add((double) stats.time / (double) stats.moves);
            System.out.println(String.format("Game %3d finished with score of %2d and %.0f ms per move", i, stats.score, (double) stats.time / stats.moves));
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

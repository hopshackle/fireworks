package com.fossgalaxy.games.fireworks.ai;

import com.fossgalaxy.games.fireworks.GameRunner;
import com.fossgalaxy.games.fireworks.GameStats;
import com.fossgalaxy.games.fireworks.ai.AgentPlayer;
import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.stats.BasicStats;
import com.fossgalaxy.stats.StatsSummary;

import java.util.Random;

/**
 * Game runner for testing.
 * <p>
 * This will run a bunch of games with your agent so you can see how it does.
 */
public class OriginalApp {
    public static void main(String[] args) {
        int numGames = 100;
        String agentName = "MonteCarloNN";

        Random random = new Random();
        StatsSummary statsSummary = new BasicStats();

        for (int i = 0; i < numGames; i++) {
            int numPlayers = ((int) (Math.random() * 4)) + 2;
            GameRunner runner = new GameRunner("test-game", numPlayers);

            //add your agents to the game
            for (int j = 0; j < numPlayers; j++) {
                // the player class keeps track of our state for us...
                Player player = new AgentPlayer(agentName, AgentUtils.buildAgent(agentName));
                runner.addPlayer(player);
            }

            GameStats stats = runner.playGame(random.nextLong());
            statsSummary.add(stats.score);
        }

        //print out the stats
        System.out.println(String.format("Our agent: Avg: %f, min: %f, max: %f",
                statsSummary.getMean(),
                statsSummary.getMin(),
                statsSummary.getMax()));
    }
}

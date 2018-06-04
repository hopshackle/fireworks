package com.fossgalaxy.games.fireworks.ai;

import com.fossgalaxy.games.fireworks.GameStats;
import com.fossgalaxy.games.fireworks.ai.hopshackle.*;
import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;
import com.fossgalaxy.stats.BasicStats;
import com.fossgalaxy.stats.StatsSummary;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Game runner for testing.
 *
 * This will run a bunch of games with your agent so you can see how it does.
 */
public class App 
{

    private static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("HH:mm:ss 'on' dd-LLL");
    public static void main( String[] args )
    {
        String policy = (args.length < 1) ? "outer" : args[0];
        int numPlayers = (args.length < 2) ? 4 : Integer.valueOf(args[1]);
        int numGames = (args.length < 3) ? 1 : Integer.valueOf(args[2]);
        String dataStrategy = (args.length < 4) ? "none" : args[3];

        runGamesAndLogResults(policy, numPlayers, numGames, dataStrategy);
    }

    private static void runGamesAndLogResults(String agentDescriptor, int numPlayers, int numGames, String dataStrategy) {

        System.out.println("Starting run for " + agentDescriptor + " at " + dateFormat.format(ZonedDateTime.now(ZoneId.of("UTC"))));

        Random random = new Random();
        StatsSummary statsSummary = new BasicStats();

        for (int i=0; i<numGames; i++) {
            //         System.out.println("Game " + i + " starting");
            GameRunner runner = new GameRunner("test-game", numPlayers);

            //add your agents to the game
            for (int j=0; j<numPlayers; j++) {
                // the player class keeps track of our state for us...
                Agent a = AgentUtils.buildAgent(agentDescriptor);
                Player player = new HopshackleAgentPlayer(agentDescriptor, a);
                if (a instanceof MCTS) {
                    MCTS policy = (MCTS) a;
                    switch (dataStrategy) {
                        case "none":
                            break;
                        case "MC":
                            StateGathererMonteCarlo sgcm = new StateGathererMonteCarlo();
                            policy.setStateGatherer(sgcm);
                            policy.setEndGameProcessor(sgcm);
                            break;
                        case "simpleClassifier":
                            StateGathererWithTarget sgwt = new StateGathererWithTarget();
                            policy.setStateGatherer(sgwt);
                            break;
                        case "rollForwardClassifier":
                            StateGathererActionClassifier sgac = new StateGathererActionClassifier();
                            policy.setStateGatherer(sgac);
                            break;
                        default:
                            throw new AssertionError("dataStrategy not recognised: " + dataStrategy);
                    }
                }
                runner.addPlayer(player);
            }

            GameStats stats = runner.playGame(random.nextLong());
            statsSummary.add(stats.score);
            System.out.println(String.format("Game %3d finished with score of %2d", i, stats.score));
        }

        //print out the stats
        System.out.println(String.format("%s: Avg: %f, min: %f, max: %f",
                agentDescriptor,
                statsSummary.getMean(),
                statsSummary.getMin(),
                statsSummary.getMax()));
    }
}

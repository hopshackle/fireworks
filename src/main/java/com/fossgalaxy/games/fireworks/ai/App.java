package com.fossgalaxy.games.fireworks.ai;

import com.fossgalaxy.games.fireworks.ai.hopshackle.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTS;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.*;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StatsSummary;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.StatsCollator;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.BasicStats;
import com.fossgalaxy.games.fireworks.ai.hopshackle.stats.GameStats;
import com.fossgalaxy.games.fireworks.players.Player;
import com.fossgalaxy.games.fireworks.utils.AgentUtils;

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
        String dataParam = (args.length < 5) ? "" : args[4];

        runGamesAndLogResults(policy, numPlayers, numGames, dataStrategy, dataParam);
    }

    private static void runGamesAndLogResults(String agentDescriptor, int numPlayers, int numGames, String dataStrategy, String dataParam) {

        System.out.println("Starting run for " + agentDescriptor + " at " + dateFormat.format(ZonedDateTime.now(ZoneId.of("UTC"))));

        Random random = new Random();
        StatsSummary scoreSummary = new BasicStats();
        StatsSummary timeSummary = new BasicStats();
        StatsCollator.clear();

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
                    int threshold = dataParam.equals("") ? 50 : Integer.valueOf(dataParam);
                    switch (dataStrategy) {
                        case "none":
                            break;
                        case "MC":
                            // Extracts state features only. Target is Monte Carlo score from full game.
                            // Hence this values (s).
                            StateGathererMonteCarlo sgcm = new StateGathererMonteCarlo();
                            policy.setStateGatherer(sgcm);
                            policy.setEndGameProcessor(sgcm);
                            break;
                        case "simpleClassifier":
                            // Uses allRules to determine which ones are triggered. Spreads a total pmf of 1.0
                            // evenly across all triggered rules. State features only.
                            // Hence this provides a score for each rule in an action-classifier.
                            StateGathererWithTargetFullTree sgwt = new StateGathererWithTargetFullTree(threshold, 12);
                            policy.setStateGatherer(sgwt);
                            break;
                        case "rollForwardClassifier":
                            // Uses State and Action features. Target is set to be 2 x probability that the
                            // action is at least as good as the best recorded action.
                            // Hence this is an action classifier, not a state valuer.
                            StateGathererActionClassifierFullTree sgac = new StateGathererActionClassifierFullTree(threshold, 12);
                            policy.setStateGatherer(sgac);
                            break;
                        case "treeRFC":
                            // Processes the whole tree, for all nodes with at least the threshold visits
                            // State and Action features used. The target is the value of the child node. So
                            // this values (s, a)
                            StateGathererFullTree sgft = new StateGathererFullTree(threshold, 12);
                            policy.setStateGatherer(sgft);
                            break;
                        case "treeRootOnly":
                            // As TreeRFC, but only ever processes the root node
                            sgft = new StateGathererFullTree(threshold, 0);
                            policy.setStateGatherer(sgft);
                            break;
                        default:
                            throw new AssertionError("dataStrategy not recognised: " + dataStrategy);
                    }
                }
                runner.addPlayer(player);
            }

            GameStats stats = runner.playGame(random.nextLong());
            scoreSummary.add(stats.score);
            timeSummary.add((double) stats.time / (double) stats.moves);
            System.out.println(String.format("Game %3d finished with score of %2d and %.0f ms per move", i, stats.score, (double) stats.time / stats.moves));
        }

        //print out the stats
        System.out.println(String.format("%s: Score Avg: %.2f, min: %.0f, max: %.0f, std err: %.2f, Time per move: %.1f ms",
                agentDescriptor,
                scoreSummary.getMean(),
                scoreSummary.getMin(),
                scoreSummary.getMax(),
                scoreSummary.getStdErr(),
                timeSummary.getMean()));

        System.out.println(StatsCollator.summaryString());
    }
}

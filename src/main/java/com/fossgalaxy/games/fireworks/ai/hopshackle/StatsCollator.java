package com.fossgalaxy.games.fireworks.ai.hopshackle;

import java.util.*;
import java.util.stream.Collectors;

public class StatsCollator {

    private static Map<String, Double> statistics = new HashMap<>();
    private static int N;

    public static void clear() {
        statistics = new HashMap<>();
        N = 0;
    }

    public static void addStatistics(Map<String, Double> newStats) {
        N++;
        newStats.forEach(
                (k, v) -> {
                    double oldV = statistics.getOrDefault(k, 0.00);
                    double newValue = oldV + v;
                    statistics.put(k, newValue);
                }
        );
    }

    public static String summaryString() {
        return statistics.entrySet().stream()
                .map(tuple -> String.format("%20s = %.2f\n", tuple.getKey(), tuple.getValue() / N))
                .collect(Collectors.joining());
    }
}

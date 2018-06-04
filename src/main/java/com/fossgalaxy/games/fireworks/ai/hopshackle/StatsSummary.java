package com.fossgalaxy.games.fireworks.ai.hopshackle;

public interface StatsSummary {


    double getMax();

    double getMin();

    double getRange();

    double getMean();

    double getStdErr();

    double getStdDev();

    void add(double score);

    int getN();
}

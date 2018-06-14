package com.fossgalaxy.games.fireworks.ai.hopshackle;

public class GameStats {
    public final String gameID;
    public final int nPlayers;
    public final int score;
    public final int lives;
    public final int moves;
    public final int information;
    public final int disqal;
    public final long time;

    public GameStats(String gameID, int players, int score, int lives, int moves, int information, int disqual, long totalTime) {
        this.gameID = gameID;
        this.nPlayers = players;
        this.score = score;
        this.lives = lives;
        this.moves = moves;
        this.information = information;
        this.disqal = disqual;
        this.time = totalTime;
    }

    public String toString() {
        return String.format("%d in %d moves (%d lives left)", this.score, this.moves, this.lives);
    }
}

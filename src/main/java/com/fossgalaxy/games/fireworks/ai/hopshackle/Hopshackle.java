package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.hopshackle.evalfn.EvalFnAgent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.*;
/*
// Entry for Mirror track
public class Hopshackle extends MCTSRuleInfoSetFullExpansion {

    public Hopshackle() {
        super(0.0, 10, 4, 950, "1|2|3|4|5|6|7|8|9|10|12|15", "YY",
                new EvalFnAgent("RESPlayers_5.params", 0.0, "1|2|3|4|5|6|7|8|9|10|12|15", "YY"));
    }

}
*/

// Entry for Mixed and Learning tracks
public class Hopshackle extends MCTSOppModelRollout {

    public Hopshackle() {
        super(0.3, 20, 4, 950, "RESOpponentModel_3.params");
    }

}

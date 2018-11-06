package com.fossgalaxy.games.fireworks.ai.hopshackle.stats;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSNode;

public interface TreeProcessor {

    public void processTree(MCTSNode root);
}

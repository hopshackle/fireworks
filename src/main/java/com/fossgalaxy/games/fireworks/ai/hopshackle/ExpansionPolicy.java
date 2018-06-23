package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;

public interface ExpansionPolicy {

    /**
     * Select a new action for the expansion node.
     *
     * @param state   the game state to travel from
     * @param agentID the AgentID to use for action selection
     * @param node    the Node to use for expansion
     * @return the next action to be added to the tree from this state.
     */
    public Action selectActionForExpand(GameState state, MCTSNode node, int agentID);

    public MCTSNode expand(MCTSNode parent, GameState state);

    public MCTSNode createNode(MCTSNode parent, int previousAgentID, Action moveTo, double C);
}

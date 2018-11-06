package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSNode;
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

    public default MCTSNode createNode(MCTSNode parent, int previousAgentID, Action moveTo, double C) {
        return createNode(parent, previousAgentID, moveTo, C, 0, 0.0);
    }

    public MCTSNode createNode(MCTSNode parent, int previousAgentID, Action moveTo, double C, int priorVisits, double priorMeanValue);
    /*
    To be used to create the root node in a tree...the one that does not have a parent
     */
    public MCTSNode createRoot(GameState startState, int previousAgentID, double C);
}

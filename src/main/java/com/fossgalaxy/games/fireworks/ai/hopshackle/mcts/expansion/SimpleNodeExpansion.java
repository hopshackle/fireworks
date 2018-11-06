package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSNode;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.ExpansionPolicy;
import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import org.slf4j.Logger;

import java.util.*;

public class SimpleNodeExpansion implements ExpansionPolicy {

    protected Logger logger;
    protected Random random;

    public SimpleNodeExpansion(Logger logger, Random random) {
        this.logger = logger;
        this.random = random;
    }


    @Override
    public MCTSNode createNode(MCTSNode parent, int previousAgentID, Action moveTo, double C, int priorVisits, double priorMeanValue) {
        GameState state = parent.getReferenceState();
        Collection<Action> allActions = Utils.generateAllActions((previousAgentID + 1) % state.getPlayerCount(), state.getPlayerCount());
        MCTSNode root = new MCTSNode(
                parent,
                previousAgentID,
                moveTo, C, allActions,
                priorVisits, priorMeanValue);
        return root;
    }

    @Override
    public MCTSNode createRoot(GameState refState, int previousAgentID, double C) {
        Collection<Action> allActions = Utils.generateAllActions((previousAgentID + 1) % refState.getPlayerCount(), refState.getPlayerCount());
        MCTSNode root = new MCTSNode(
                null,
                previousAgentID,
                null, C, allActions);
        return root;
    }

    @Override
    public Action selectActionForExpand(GameState state, MCTSNode node, int agentID) {
        Collection<Action> legalActions = node.getLegalUnexpandedMoves(state, agentID);
        if (legalActions.isEmpty()) {
            return null;
        }

        Iterator<Action> actionItr = legalActions.iterator();

        int selected = random.nextInt(legalActions.size());
        Action curr = actionItr.next();
        for (int i = 0; i < selected; i++) {
            curr = actionItr.next();
        }
        logger.trace("Selected action " + curr + " for expansion from node:");
        // node.printChildren();
        return curr;
    }

    @Override
    public MCTSNode expand(MCTSNode parent, GameState state) {
        int nextAgentID = (parent.getAgent() + 1) % state.getPlayerCount();
        Action action = selectActionForExpand(state, parent, nextAgentID);
        // It is possible it wasn't allowed
        if (action == null) {
            return parent;
        }
        if (parent.containsChild(action)) {
            // return the correct node instead
            return parent.getChild(action);
        }
        //XXX we may expand a node which we already visited? :S
        MCTSNode child = createNode(parent, nextAgentID, action, parent.expConst);
        parent.addChild(child);
        return child;
    }

}

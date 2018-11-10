package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion;

import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.MCTSNode;
import com.fossgalaxy.games.fireworks.ai.hopshackle.mcts.expansion.ExpansionPolicy;
import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.state.CardColour;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.*;
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
        Collection<Action> allActions = generateAllActions((previousAgentID + 1) % state.getPlayerCount(),
                state.getPlayerCount(),
                state.getInfomation());
        MCTSNode root = new MCTSNode(
                parent,
                previousAgentID,
                moveTo, C, allActions,
                priorVisits, priorMeanValue);
        return root;
    }

    @Override
    public MCTSNode createRoot(GameState refState, int previousAgentID, double C) {
        Collection<Action> allActions = generateAllActions((previousAgentID + 1) % refState.getPlayerCount(),
                refState.getPlayerCount(), refState.getInfomation());
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
        int nextAgentID = (parent.getAgentId() + 1) % state.getPlayerCount();
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

    private static final int[] HAND_SIZE = new int[]{-1, -1, 5, 5, 4, 4};


    public static Collection<Action> generateAllActions(int playerID, int numPlayers, int information) {
        HashSet<Action> list = new HashSet();

        int player;
        for (player = 0; player < HAND_SIZE[numPlayers]; ++player) {
            if (information < 8) list.add(new DiscardCard(player));
            list.add(new PlayCard(player));
        }

        for (player = 0; player < numPlayers; ++player) {
            if (player != playerID) {
                CardColour[] var4 = CardColour.values();
                int var5 = var4.length;

                for (int var6 = 0; var6 < var5; ++var6) {
                    CardColour colour = var4[var6];
                    list.add(new TellColour(player, colour));
                }

                for (int i = 1; i <= 5; ++i) {
                    list.add(new TellValue(player, i));
                }
            }
        }

        return list;
    }

}

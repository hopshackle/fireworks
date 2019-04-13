package com.fossgalaxy.games.fireworks.ai.hopshackle.mcts;


import com.fossgalaxy.games.fireworks.ai.Agent;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.LegalActionFilter;
import com.fossgalaxy.games.fireworks.ai.hopshackle.rules.RuleGenerator;
import com.fossgalaxy.games.fireworks.ai.rule.Rule;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.GameState;
import com.fossgalaxy.games.fireworks.state.actions.*;
import java.util.*;
import java.util.stream.Collectors;

public class RandomRule implements Agent {

    protected List<Rule> allRules;
    private Random rand = new Random(4);

    @AgentConstructor("hs-randomRule")
    public RandomRule(String rules) {
        allRules = RuleGenerator.generateRules(rules, "");
    }

    @Override
    public Action doMove(int playerID, GameState state) {
        List<Action> legalActions = allRules.stream()
          //      .filter(r -> r.canFire(playerID, state))...in practice this slows us down!
                .map(r -> r.execute(playerID, state))
                .filter(Objects::nonNull)
                .distinct()
                .filter(LegalActionFilter.isLegal(playerID, state))
                .collect(Collectors.toList());

        if (legalActions.isEmpty()) {
            for (int i = 0; i < state.getHandSize(); i++) {
                if (state.getHand(playerID).hasCard(i)) {
                    return new DiscardCard(i);
                }
            }
        }

        return legalActions.get(rand.nextInt(legalActions.size()));
    }
}


package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.osawa.rules.OsawaDiscard;
import com.fossgalaxy.games.fireworks.ai.rule.*;
import com.fossgalaxy.games.fireworks.ai.rule.TellAnyoneAboutUsefulCard;
import com.fossgalaxy.games.fireworks.ai.rule.TellIllInformed;
import com.fossgalaxy.games.fireworks.ai.rule.TellMostInformation;
import com.fossgalaxy.games.fireworks.annotations.AgentBuilderStatic;

/**
 * Created by webpigeon on 09/05/17.
 */
public class BoardGameGeekFactory {

    /**
     * A factory implementing CliveJ's ruleset
     *
     * @link https://www.boardgamegeek.com/article/23427635#23427635
     * @return
     */
    @AgentBuilderStatic("clivej2")
    public static ProductionRuleAgent buildCliveJ() {
        ProductionRuleAgent pra = new ProductionRuleAgent();
        pra.addRule(new TryToUnBlock());
        pra.addRule(new PlaySafeCard());

        pra.addRule(new TellIllInformed());

        pra.addRule(new OsawaDiscard());


        // ID on unknown
        pra.addRule(new CompleteTellUsefulCard());
        pra.addRule(new TellAnyoneAboutUsefulCard());
        pra.addRule(new TellAnyoneAboutUselessCard());
        pra.addRule(new TellMostInformation(true));

        pra.addRule(new DiscardUnidentifiedCard());
        pra.addRule(new DiscardHighest());
        pra.addRule(new DiscardLeastLikelyToBeNecessary());
        pra.addRule(new DiscardProbablyUselessCard(0.0));

        return pra;
    }

}

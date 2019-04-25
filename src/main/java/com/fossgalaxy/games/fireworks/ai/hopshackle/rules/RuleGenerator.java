package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

import com.fossgalaxy.games.fireworks.ai.rule.Rule;

import java.util.*;

public class RuleGenerator {

    public static List<Rule> generateRules(String ruleMnemonics, String conventionString) {
        List<Rule> retValue = new ArrayList<>();
        Conventions conventions = new Conventions(conventionString);
        String[] ruleArray = ruleMnemonics.split("[|]");
        for (String mnemonic : ruleArray) {
            Integer key = Integer.valueOf(mnemonic);
            switch (key) {
                case 1:
                    retValue.add(new TellAboutSingleUsefulCard(conventions));
                    break;
                case 2:
                    retValue.add(new TellMostInformation(conventions));
                    break;
                case 3:
                    retValue.add(new TellAnyoneAboutPlayableCard(conventions));
                    break;
                case 4:
                    retValue.add(new TellDispensable(conventions));
                    break;
                case 5:
                    retValue.add(new TellRedOrangeForPlayableNewCard(conventions));
                    break;
                case 6:
                    retValue.add(new CompleteTellPlayableCard(conventions));
                    break;
                case 7:
                    retValue.add(new CompleteTellDispensableCard(conventions));
                    break;
                case 8:
                    retValue.add(new CompleteTellCurrentlyNotPlayableCard(conventions));
                    break;
                case 9:
                    retValue.add(new PlayProbablySafeCard(conventions, 0.7));
                    break;
                case 10:
                    retValue.add(new PlayProbablySafeLateGameCard(conventions, 0.4, 5));
                    break;
                case 11:
                    retValue.add(new DiscardProbablyUselessCard(conventions, 0.8));
                    break;
                case 12:
                    retValue.add(new DiscardLeastLikelyToBeNecessary(conventions));
                    break;
                case 13:
                    retValue.add(new DiscardProbablyUselessCard(conventions, 0.0));
                    break;
                case 14:
                    retValue.add(new PlayBestCardIfTwoPlayerAndCannotDiscard(conventions));
                    break;
                case 15:
                    retValue.add(new TellNotDiscardable(conventions));
                    break;
                case 16:
                    retValue.add(new PlayProbablySafeCard(conventions, 0.0));
                    break;
                case 17:
                    retValue.add(new PlaySafestCardIfNoDiscardPossible(conventions));
                    break;
                default:
                    throw new AssertionError("Mnemonic for rule does not exist: " + mnemonic);
            }
        }
        return retValue;
    }

}

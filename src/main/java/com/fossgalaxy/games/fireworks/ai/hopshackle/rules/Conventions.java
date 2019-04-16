package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

public class Conventions {

    public final boolean singleTouchIsPlayable, redYellowMeansMostRecentIsPlayable;

    public Conventions(String conventionString) {
        singleTouchIsPlayable = conventionString.length() > 0 && conventionString.charAt(0) == 'Y';
        redYellowMeansMostRecentIsPlayable =  conventionString.length() > 1 && conventionString.charAt(1) == 'Y';
    }

}

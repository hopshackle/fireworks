package com.fossgalaxy.games.fireworks.ai.hopshackle.rules;

public class Conventions {

    public final boolean singleTouchIsPlayable;

    public Conventions(String conventionString) {
        singleTouchIsPlayable = conventionString.split("[|]")[0].equals("Y");
    }

}

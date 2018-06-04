package com.fossgalaxy.games.fireworks.ai.hopshackle;

import com.fossgalaxy.games.fireworks.ai.rule.logic.DeckUtils;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.Action;
import com.fossgalaxy.games.fireworks.state.actions.DiscardCard;
import com.fossgalaxy.games.fireworks.state.actions.PlayCard;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.nd4j.linalg.api.ndarray.*;
import org.nd4j.linalg.cpu.nativecpu.*;
import org.nd4j.linalg.dataset.*;
import org.slf4j.*;

import java.io.FileWriter;
import java.util.*;
import java.util.stream.*;

public class StateGathererMonteCarlo extends StateGatherer implements HasGameOverProcessing {

    private List<Map<String, Double>> experienceData = new ArrayList();

    @Override
    public void storeData(MCTSNode node, GameState gameState, int playerID) {
        Map<String, Double> features = extractFeatures(gameState, playerID);
        if (logger.isDebugEnabled()) logger.debug(asCSVLine(features));
        experienceData.add(features);
    }

    @Override
    public void onGameOver(double finalScore) {

        try {
            //      FileWriter writerJSON = new FileWriter(fileLocation + "/rawData.json", true);
            FileWriter writerCSV = new FileWriter(fileLocation + "/StateData.csv", true);
            for (Map<String, Double> tuple : experienceData) {
                String csvLine = asCSVLine(tuple);
                double scoreGain = finalScore / 25.0 - tuple.get("SCORE");
                tuple.put("RESULT", scoreGain);
                //           String jsonString = gson.toJson(tuple);
                //           String headerLine = tuple.keySet().stream().collect(Collectors.joining("\n"));
                //           System.out.println(headerLine);
                //              writerJSON.write(jsonString);
                writerCSV.write(String.format("%.3f\t%s\n", scoreGain, csvLine));
            }
            //       writerJSON.close();
            writerCSV.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

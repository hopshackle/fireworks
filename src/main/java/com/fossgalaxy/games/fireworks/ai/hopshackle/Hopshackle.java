package com.fossgalaxy.games.fireworks.ai.hopshackle;
import com.fossgalaxy.games.fireworks.ai.*;
import com.fossgalaxy.games.fireworks.ai.iggi.Utils;
import com.fossgalaxy.games.fireworks.annotations.AgentConstructor;
import com.fossgalaxy.games.fireworks.state.*;
import com.fossgalaxy.games.fireworks.state.actions.*;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.*;

public class Hopshackle implements Agent {

    static EvalFnAgent valueAgent;

    MCTSRuleInfoSetFullExpansion actualBrain;

    public Hopshackle() {
        if (valueAgent == null) {
            try {
                ClassLoader classLoader = getClass().getClassLoader();
                MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(classLoader.getResourceAsStream("Tree_rnd_02.model"));
                NormalizerSerializer ns = new NormalizerSerializer();
                ns.addStrategy(new StandardizeSerializerStrategy());
                NormalizerStandardize normalizer = ns.restore(classLoader.getResourceAsStream("Tree_rnd_02.model.normal"));
                valueAgent = new EvalFnAgent(model, normalizer);
            } catch (Exception e) {
                System.out.println("Error when reading in Model " + e.toString());
                e.printStackTrace();
            }
        }
        actualBrain = new MCTSRuleInfoSetFullExpansion(0.03, 100, 3, 30, valueAgent);
    }


    @Override
    public Action doMove(int i, GameState gameState) {
        return actualBrain.doMove(i, gameState);
    }
}

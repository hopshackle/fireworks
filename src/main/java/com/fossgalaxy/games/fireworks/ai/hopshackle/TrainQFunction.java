package com.fossgalaxy.games.fireworks.ai.hopshackle;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.NormalizerSerializer;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class TrainQFunction {

    private static Logger log = LoggerFactory.getLogger(TrainQFunction.class);
    private static int batchSize = 16;
    private static int hiddenNeurons = 10;
    private static double learningRate = 1e-4;
    private static int seed = 147;
    private static int epochs = 20;

    /*
    Takes a file as an argument, and then uses this to train a simple Neural Network
    which is written to a second location as a file

    We assume that the first column in the file is the target value
     */
    public static void main(String[] args) {
        if (args.length != 2) throw new AssertionError("Need two arguments for input and output locations");

        String inputLocation = args[0];
        String outputLocation = args[1];

        RecordReader recordReader = new CSVRecordReader('\t');
        try {
            recordReader.initialize(new FileSplit(new File(inputLocation)));
        } catch (Exception e) {
            throw new AssertionError("Error processing file " + inputLocation + ":\n" + e.toString());
        }
        int numberOfRules = MCTSRuleInfoSet.allRules.size();

        log.info("Starting...");
        DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader, batchSize, 0, numberOfRules - 1, true);

        NormalizerStandardize normalizer = new NormalizerStandardize();
        normalizer.fit(iterator);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
        iterator.setPreProcessor(normalizer); // then set this to pre-process the data

        // now load the test data
        File testFile = new File(inputLocation + "_test");
        DataSet testData = null;
        if (testFile.exists()) {
            RecordReader testReader = new CSVRecordReader('\t');
            try {
                testReader.initialize(new FileSplit(testFile));
            } catch (Exception e) {
                throw new AssertionError("Error processing file " + inputLocation + "_test:\n" + e.toString());
            }
            DataSetIterator testIterator = new RecordReaderDataSetIterator(testReader, 1000, 0, numberOfRules - 1, true);
            testData = testIterator.next();
        }


        log.info("Completed pre-processing...");
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .l2(1e-6)
                .updater(new Nesterovs(learningRate, 0.9))
                .list()
                .layer(0, new DenseLayer.Builder().nIn(iterator.inputColumns()).nOut(hiddenNeurons)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.TANH)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.IDENTITY)
                        .nIn(hiddenNeurons).nOut(numberOfRules).build())
                .pretrain(false).backprop(true).build();


        log.info("Building model...");
        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        if (testData != null) log.info(String.format("Before training the test error is %.3f", model.score(testData)));
        for (int n = 0; n < epochs; n++) {
            model.fit(iterator);
            double testScore = (testData != null) ? model.score(testData) : Double.NaN;
            log.info(String.format("Epoch %3d has error %.3f, and test error %.3f", n, model.score(), testScore));
        }

        //Save the model
        boolean saveUpdater = true;                                             //Updater: i.e., the state for Momentum, RMSProp, Adagrad etc. Save this if you want to train your network more in the future
        try {
            ModelSerializer.writeModel(model, outputLocation, saveUpdater);
        } catch (Exception e) {
            throw new AssertionError("Error writing file " + outputLocation + ":\n" + e.toString());
        }

        // Now we want to save the normalizer to a binary file. For doing this, one can use the NormalizerSerializer.
        NormalizerSerializer serializer = NormalizerSerializer.getDefault();

        // Save the normalizer to a temporary file
        try {
            serializer.write(normalizer, outputLocation + ".normal");
        } catch (Exception e) {
            throw new AssertionError("Error writing file " + outputLocation + ".normal:\n" + e.toString());

        }
    }
}

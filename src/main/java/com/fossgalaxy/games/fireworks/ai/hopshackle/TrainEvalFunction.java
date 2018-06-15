package com.fossgalaxy.games.fireworks.ai.hopshackle;

import org.datavec.api.records.reader.impl.collection.CollectionRecordReader;
import org.datavec.api.writable.Writable;
import org.deeplearning4j.datasets.datavec.*;
import org.datavec.api.records.reader.*;
import org.datavec.api.records.reader.impl.csv.*;
import org.datavec.api.split.*;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.dataset.api.preprocessor.serializer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;


public class TrainEvalFunction {

    private static Logger log = LoggerFactory.getLogger(TrainEvalFunction.class);
    private static int batchSize = 16;
    private static int hiddenNeurons = 30;
    private static double learningRate = 1e-3;
    private static int seed = 147;
    private static int epochs = 30;
    private static double trainingPercentage = 0.8;

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
        List<List<Writable>> allData = new ArrayList<>();
        while (recordReader.hasNext())
            allData.add(recordReader.next());

        List<List<Writable>> trainData = allData.subList(0, (int) (allData.size() * trainingPercentage));
        List<List<Writable>> testData = allData.subList((int) (allData.size() * trainingPercentage), allData.size());
        Collections.shuffle(trainData);

        CollectionRecordReader crrTrain = new CollectionRecordReader(trainData);
        CollectionRecordReader crrTest = new CollectionRecordReader(testData);

        log.info("Starting...");
//        DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader, batchSize, 0, 0, true);
        DataSetIterator iterator = new RecordReaderDataSetIterator(crrTrain, batchSize, 0, 0, true);


        NormalizerStandardize normalizer = new NormalizerStandardize();
        normalizer.fit(iterator);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
        iterator.setPreProcessor(normalizer); // then set this to pre-process the data

        // now load the test data
        DataSetIterator testIterator = new RecordReaderDataSetIterator(crrTest, testData.size(), 0, 0, true);
        testIterator.setPreProcessor(normalizer); // then set this to pre-process the test data too!

        log.info("Completed pre-processing...");
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                //            .l2(1e-6)
                .updater(new Nesterovs(learningRate, 0.9))
                .list()
                .layer(0, new DenseLayer.Builder().nIn(iterator.inputColumns()).nOut(hiddenNeurons)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.RECTIFIEDTANH)
                        .build())
                .layer(1, new OutputLayer.Builder(LossFunctions.LossFunction.L2)
                        .weightInit(WeightInit.XAVIER)
                        .activation(Activation.RELU)
                        .nIn(hiddenNeurons).nOut(1).build())
                .pretrain(false).backprop(true).build();


        log.info("Building model...");
        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();

        DataSet testDataSet = testData.isEmpty() ? null : ((RecordReaderDataSetIterator) testIterator).next();
        if (testData.size() > 0)
            log.info(String.format("Before training the test error is %.3f", model.score(testDataSet)));
        for (int n = 0; n < epochs; n++) {
            model.fit(iterator);
            double testScore = (testData.size() > 0) ? model.score(testDataSet) : Double.NaN;
            log.info(String.format("Epoch %3d has error %.4f, and test error %.4f", n, model.score(), testScore));
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

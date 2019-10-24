/*
 * Copyright 2019 Ben Rawlings
 */

package de.tuhh.luethke.odke.model;

import de.tuhh.luethke.okde.model.SampleModel;
import de.tuhh.luethke.okde.utility.LogFormatter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;

import org.ejml.simple.SimpleMatrix;
import org.junit.Assert;
import org.junit.Test;

import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Test the sample model
 *
 * Verify basic trivial cases work as expected
 * Check the performance for the updateDistribution method
 */
public class SampleModelTest {
	
	final Logger logger = Logger.getLogger(this.getClass().getSimpleName());
	{
		Path logsDir = Paths.get("test-logs");
		if (!logsDir.toFile().exists()) {
			logsDir.toFile().mkdirs();
		}
		logger.setLevel(Level.INFO);
		int limit = 10000;
		int count = 1;
		try {
			FileHandler handler = new FileHandler(logsDir.resolve(logger.getName()).toString(), limit, count);
			handler.setFormatter(new LogFormatter());
			logger.addHandler(handler);
		} catch (SecurityException | IOException e) {
			e.printStackTrace();
		}
	}

	final double forgettingFactor = 1.0;
	final double compressionThreshold = 0.02;
	
	/**
	 * Test the SampleModel for the trivial case in which there is just one sample
	 */
	@Test
	public void twoSampleValueTest() throws Exception {

		final SampleModel sampleModel = new SampleModel(forgettingFactor, compressionThreshold);

		final double[][] sample1 = {{1.0},{2.0}};
		final double[][] sample2 = {{3.0},{4.0}};
		final SimpleMatrix[] samplesMatrixArray = new SimpleMatrix[2];
		samplesMatrixArray[0] = new SimpleMatrix(sample1); 
		samplesMatrixArray[1] = new SimpleMatrix(sample2);
		
		final SimpleMatrix covariances = createEmptyCovarianceMatrix(2);
		final SimpleMatrix[] covariancesArray = {covariances, covariances};

		final double[] weights = {1.0, 1.0};

		sampleModel.updateDistribution(samplesMatrixArray, covariancesArray, weights);

		// Test the output distribution
		final double resultSample1 = sampleModel.evaluate(new SimpleMatrix(sample1));
		Assert.assertTrue(resultSample1 > 0.0);
		
		final double resultSample2 = sampleModel.evaluate(new SimpleMatrix(sample2));
		Assert.assertTrue(resultSample2 > 0.0);
		Assert.assertEquals(resultSample1, resultSample2, resultSample1 / 1000.0);
		
		double[][] randomValue = {{2.0}, {2.0}};
		final double resultRandomValue = sampleModel.evaluate(new SimpleMatrix(randomValue));
		Assert.assertTrue(resultRandomValue < resultSample1);
		Assert.assertTrue(resultRandomValue < resultSample2);
	}

	/**
	 * Test the performance for a KDE created with 1000 samples
	 * 
	 * @throws Exception 
	 */
	@Test
	public void singleUpdateRandomDataPerformanceTest() throws Exception {
		
		final int numberOfSamples = 1000;
		SampleModel sampleModel = createSampleModel(createSamples(createRandomSampleValues(numberOfSamples)));
		
		// Test the output
		
		// We don't expect there to be a big difference across the distribution;
		// Check that 0.1 to 0.9 are about the same
		ArrayList<SimpleMatrix> points = new ArrayList<>(9);
		for (double d = 0.1; d < 1.0; d+=0.1) {
			double [][] pointValue = {{d}, {d}};
			points.add(new SimpleMatrix(pointValue));
		}
		ArrayList<Double> probabilities = sampleModel.evaluate(points);
		
		double sum = 0.0;
		for (Double value : probabilities) sum += value;
		final double mean = sum / probabilities.size();
		
		double deviationSum = 0.0;
		for (Double value : probabilities)
		{
			deviationSum += (mean - value); 
		}
		
		logger.info("Average deviation was: " + deviationSum/probabilities.size());
		Assert.assertTrue("The deviation tolerance has been exceeded " + deviationSum, deviationSum < 0.05); 
	}

	/**
	 * Create a SampleModel with two samples and then update it 1000 times
	 * 
	 * @throws Exception
	 */
	@Test
	public void multipleUpdateRandomDataPerformanceTest() throws Exception {
		
		final int numberOfSamples = 1000;
		
		SampleModel sampleModel = createSampleModel(createSamples(createRandomSampleValues(2)));
		
		final SimpleMatrix covariances = createEmptyCovarianceMatrix(2);
		for (int counter = 0; counter < numberOfSamples; counter++) {
			double[][] sample = {{Math.random()}, {Math.random()}};
			sampleModel.updateDistribution(new SimpleMatrix(sample), covariances, 1.0);
		}
		
		// Test the output
		
		// We don't expect there to be a big difference across the distribution;
		// Check that 0.1 to 0.9 are about the same
		ArrayList<SimpleMatrix> points = new ArrayList<>(9);
		for (double d = 0.1; d < 1.0; d+=0.1) {
			double [][] pointValue = {{d}, {d}};
			points.add(new SimpleMatrix(pointValue));
		}
		ArrayList<Double> probabilities = sampleModel.evaluate(points);
		
		double sum = 0.0;
		for (Double value : probabilities) sum += value;
		final double mean = sum / probabilities.size();
		
		double deviationSum = 0.0;
		for (Double value : probabilities)
		{
			deviationSum += (mean - value); 
		}
		Assert.assertTrue("The deviation tolerance has been exceeded " + deviationSum, deviationSum < 0.05); 
	}

	/**
	 * Test the output from a single 1000 sample update is similar to one created with 2 and updated 998 times
	 * @throws Exception 
	 */
	@Test
	public void updateDistributionMethodTest() throws Exception {
	
		SimpleMatrix[] randomSamples = createSamples(createRandomSampleValues(1000));
		
		SampleModel singleUpdateModel = createSampleModel(randomSamples); 
		
		SimpleMatrix[] twoRandomSamples = new SimpleMatrix[2];
		twoRandomSamples[0] = randomSamples[0];
		twoRandomSamples[1] = randomSamples[1];
				
		SampleModel multipleUpdateModel = createSampleModel(twoRandomSamples);
		SimpleMatrix covariance = createEmptyCovarianceMatrix(2);
		for (int sampleNumber = 2; sampleNumber < 1000; sampleNumber++) {
			multipleUpdateModel.updateDistribution(randomSamples[sampleNumber], covariance, 1.0);
		}
		
		// We should have two samples models created from the same data!
		// so they should be the same!
		
		// test values between 0 and 1
		logger.info("Multi vs Single");
		NumberFormat formatter = NumberFormat.getPercentInstance();
		for (double value = 0.1; value < 1.0; value+= 0.1) {
			double[][] pointValue = {{value}, {value}};
			SimpleMatrix point = new SimpleMatrix(pointValue);
			final double singleUpdateProbability = singleUpdateModel.evaluate(point);
			final double multiUpdateProbablity = multipleUpdateModel.evaluate(point);
			logger.info("Multi = " + multiUpdateProbablity +  " - Diff = " + 
					formatter.format((singleUpdateProbability - multiUpdateProbablity) / multiUpdateProbablity));
		}
	}
	
	/**
	 * Create a covariance array of zeros
	 */
	private SimpleMatrix createEmptyCovarianceMatrix(final int dimensions) {

		final double[][] covariances = new double[dimensions][dimensions];
		for (int i = 0; i < dimensions; i++) {
			for (int j = 0; j < dimensions; j++) {
				covariances[i][j] = 0;
			}
		}
		return new SimpleMatrix(covariances);
	}

	private double[][][] createRandomSampleValues(final int numberOfSamples) {

		double[][][] sampleValues = new double[numberOfSamples][2][1];
		for (int counter = 0; counter < numberOfSamples; counter++) {
			double[][] sample = {{Math.random()}, {Math.random()}};
			sampleValues[counter] = sample; 
		}
		return sampleValues;
	}
	
	private SimpleMatrix[] createSamples(final double[][][] sampleValues) {
		
		final SimpleMatrix[] samplesMatrixArray = new SimpleMatrix[sampleValues.length];
		for (int counter = 0; counter < sampleValues.length; counter++) {
			samplesMatrixArray[counter] = new SimpleMatrix(sampleValues[counter]);
		}
		return samplesMatrixArray;
	}

	/**
	 * Create a SampleModel with a set of random numbers (from 0 to 1)
	 */
	private SampleModel createSampleModel(final SimpleMatrix[] samplesMatrixArray) throws Exception {

		SampleModel sampleModel = new SampleModel(forgettingFactor, compressionThreshold);

		final SimpleMatrix covariances = createEmptyCovarianceMatrix(2);
		final SimpleMatrix[] covariancesArray = new SimpleMatrix[samplesMatrixArray.length];
		final double[] weightsArray = new double[samplesMatrixArray.length];
		for (int counter = 0; counter < samplesMatrixArray.length; counter++) {
			covariancesArray[counter] = covariances;
			weightsArray[counter] = 1.0;
		}
		sampleModel.updateDistribution(samplesMatrixArray, covariancesArray, weightsArray);
		return sampleModel;
	}

}

package de.tuhh.luethke.okde.Compression;

import de.tuhh.luethke.okde.model.SampleModel;

import org.junit.Test;

/**
 * Test the compressor
 *
 * There seems to be a performance issue with the compress method 
 * for distributions with more than 1000 subdistributions
 */
public class CompressorTest {

	final double forgettingFactor = 1.0;
	final double compressionThreshold = 0.02;
	
	@Test
	public void compressTenDistributionsRegression() {

		SampleModel sampleModel = new SampleModel(this.forgettingFactor, this.compressionThreshold);
		
		
	}	
}

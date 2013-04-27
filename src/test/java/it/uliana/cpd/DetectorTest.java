package it.uliana.cpd;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Test;

public class DetectorTest {

	private final static Logger LOGGER = Logger.getLogger(DetectorTest.class);

	private String[] ignored = { "^\\.", "license", "\\.md$" };
	private String path = "src/test/resources/sample-bootstrap-js";
	private int kGramSize = 8;
	private int similarityThreshold = 20;

	@Test
	public void testGetDuplicates() throws DetectorException {

		List<Pair> duplicates = new Detector(ignored).getDuplicates(path,
				kGramSize, similarityThreshold);
		LOGGER.info(duplicates.size() + " CPDs detected");

		assertTrue(duplicates.size() > 0);
		for (Pair p : duplicates) {
			LOGGER.info(p);
		}
	}

}

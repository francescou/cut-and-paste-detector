package it.uliana.cpd;

import org.apache.log4j.Logger;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class DetectorTest {

	private final static Logger LOGGER = Logger.getLogger(DetectorTest.class);

	private String[] ignored = { "^\\.", "license", "\\.md$" };
	private String path = "src/test/resources/sample-bootstrap-js";
	private int kGramSize = 8;
	private int similarityThreshold = 20;

	@Test
	public void testGetDuplicates() throws IOException {



        final File docDir = new File(path);
        if (!docDir.exists() || !docDir.canRead()) {
            System.out
                    .println("Document directory '"
                            + docDir.getAbsolutePath()
                            + "' does not exist or is not readable, please check the path");
        }


        List<File> l = indexDocs(docDir);


        Map<String, InputStream> map = new HashMap<>();

        l.stream().forEach(f -> {
            String filePath = f.getPath();
            try {
                InputStream is = new FileInputStream(f);
                map.put(filePath, is);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

        });

		List<Pair> duplicates = new Detector().getDuplicates(kGramSize, similarityThreshold, map);
		LOGGER.info(duplicates.size() + " CPDs detected");

		assertTrue(duplicates.size() > 0);
		for (Pair p : duplicates) {
			LOGGER.info(p);
		}
	}



	/**
	 *
	 * Indexes the given file using the given writer, or if a directory is
	 * given, recurses over files and directories found under the given
	 * directory.
	 *
	 * @param file
	 *            The file to index, or the directory to recurse into to find
	 *            files to index
	 * @throws IOException
	 *             If there is a low-level I/O error
	 *
	 */
	private List<File> indexDocs(File file) throws IOException {

        List<File> accumulator = new ArrayList<File>();

		// do not try to index files that cannot be read
		if (file.canRead()) {
			if (file.isDirectory()) {
				String[] tmp = file.list();
				List<String> l = new ArrayList<String>();
				for (String s : tmp) {
					if (!isIgnored(s)) {
						l.add(s);
					}
				}
				String[] files = l.toArray(new String[l.size()]);
				// an IO error could occur
				if (files != null) {
					for (int i = 0; i < files.length; i++) {
                        List<File> ll = indexDocs(new File(file, files[i]));
                        accumulator.addAll(ll);
					}
				}
			} else {
				return Arrays.asList(file);
			}
		}

        return accumulator;

	}


	/**
	 *
	 * Check if file is to be ignored according to the IGNORED list
	 *
	 * @param file
	 *            the file to check
	 * @return true if the file is to be ignored
	 */
	private boolean isIgnored(String file) {
		for (String pattern : ignored) {
			Pattern p = Pattern.compile(pattern);
			Matcher m = p.matcher(file);
			if (m.find()) {
				return true;
			}
		}
		return false;
	}


}

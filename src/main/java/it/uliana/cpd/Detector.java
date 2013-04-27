package it.uliana.cpd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;

public class Detector {

	private final static Logger LOGGER = Logger.getLogger(Detector.class);

	private static final int MIN_DOC_FREQUENCY = 1;
	private static final String FIELD_CONTENTS = "contents";
	private static final String FIELD_PATH = "path";
	private static final String INDEX_PATH = "index";

	private String[] ignored = {};

	public Detector() {
	}

	/**
	 * 
	 * Constructor with a parameter to specify the file patterns to ignore.
	 * 
	 * @param ignored
	 *            the pattern to ignore
	 */
	public Detector(String[] ignored) {
		this.ignored = ignored;
	}

	/**
	 * 
	 * Detect duplicated content between pairs of files in a folder.
	 * 
	 * @param path
	 *            the path to scan
	 * @param kGramSize
	 *            the size of the kGram used to index the collection. Suggested
	 *            value: between 6 and 10.
	 * @param similarityThreshold
	 *            the similarity threshold above which two documents will be
	 *            considered "duplicated".
	 * @return a list containing all the document pairs who have duplicated
	 *         content
	 * @throws DetectorException
	 */
	public List<Pair> getDuplicates(String path, int kGramSize,
			int similarityThreshold) throws DetectorException {

		index(path, kGramSize);

		Date d = new Date();

		Map<String, Set<String>> collection;
		try {
			collection = convertDocumentsToMap();

			LOGGER.debug(new Date().getTime() - d.getTime()
					+ " milliseconds to create matrix");

			d = new Date();

			List<Pair> duplicates = checkCPD(collection, similarityThreshold);

			LOGGER.debug(duplicates.size() + " duplicated elements");

			LOGGER.debug(new Date().getTime() - d.getTime()
					+ " milliseconds to check duplicated content");

			return duplicates;
		} catch (IOException e) {
			throw new DetectorException(e);
		} finally {
			try {
				FileUtils.deleteDirectory(new File(INDEX_PATH));
			} catch (IOException e) {
				LOGGER.warn(e);
			}
		}
	}

	/**
	 * 
	 * Convert documents to Map in which keys are documents path and values are
	 * the set of n-grams.
	 * 
	 * @return Map in which keys are documents path and values are the set of
	 *         n-grams.
	 * 
	 * @throws IOException
	 */
	private Map<String, Set<String>> convertDocumentsToMap() throws IOException {

		IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(
				INDEX_PATH)));

		Map<String, Set<String>> collection = new HashMap<String, Set<String>>();

		for (int j = 0; j < reader.numDocs(); j++) {
			Set<String> terms = new HashSet<String>();
			String path = reader.document(j).getField(FIELD_PATH).stringValue();

			LOGGER.debug("DOCUMENT " + j + ": " + path);
			Terms tv = reader.getTermVector(j, FIELD_CONTENTS);

			if (tv == null) {
				continue;
			}

			TermsEnum i = tv.iterator(null);

			while (i.next() != null) {
				String term = i.term().utf8ToString();
				Term t = new Term(FIELD_CONTENTS, term);
				if (reader.docFreq(t) > MIN_DOC_FREQUENCY) {
					terms.add(term);
					LOGGER.debug(term + " " + reader.docFreq(t));
				}
			}
			collection.put(path, terms);
		}

		return collection;
	}

	/**
	 * 
	 * return duplicated
	 * 
	 * @param collection
	 * @param similarityThreshold
	 * @return
	 */
	private List<Pair> checkCPD(Map<String, Set<String>> collection,
			int similarityThreshold) {
		Set<String> paths = collection.keySet();
		List<Pair> duplicates = new ArrayList<Pair>();
		for (String d1 : paths) {
			for (String d2 : paths) {
				if (d1.compareTo(d2) < 0) { // prevent double comparison
					SetView<String> intersection = Sets.intersection(
							collection.get(d1), collection.get(d2));

					int score = intersection.size();
					if (score > similarityThreshold) {
						Pair p = new Pair(d1, d2, score);
						duplicates.add(p);
						LOGGER.debug(p);
						LOGGER.debug(intersection);
					}
				}
			}
		}

		return duplicates;
	}

	/**
	 * 
	 * Index all text files under a directory.
	 * 
	 * @param docsPath
	 *            the path to index
	 * 
	 */
	private void index(String docsPath, int kGramSize) {

		final File docDir = new File(docsPath);
		if (!docDir.exists() || !docDir.canRead()) {
			System.out
					.println("Document directory '"
							+ docDir.getAbsolutePath()
							+ "' does not exist or is not readable, please check the path");
			System.exit(1);
		}

		Date start = new Date();
		try {
			LOGGER.info("Indexing '" + docsPath + "' to directory '"
					+ INDEX_PATH + "'...");

			Directory dir = FSDirectory.open(new File(INDEX_PATH));
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_42);

			// index shingles
			analyzer = new ShingleAnalyzerWrapper(analyzer, kGramSize,
					kGramSize, ShingleFilter.TOKEN_SEPARATOR, false, false);

			IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_42,
					analyzer);

			iwc.setOpenMode(OpenMode.CREATE);

			// iwc.setRAMBufferSizeMB(256.0);

			IndexWriter writer = new IndexWriter(dir, iwc);
			indexDocs(writer, docDir);

			writer.close();

			Date end = new Date();
			LOGGER.info(end.getTime() - start.getTime()
					+ " milliseconds to index collection");

		} catch (IOException e) {
			LOGGER.info(" caught a " + e.getClass() + "\n with message: "
					+ e.getMessage());
		}
	}

	/**
	 * 
	 * Indexes the given file using the given writer, or if a directory is
	 * given, recurses over files and directories found under the given
	 * directory.
	 * 
	 * @param writer
	 *            Writer to the index where the given file/dir info will be
	 *            stored
	 * @param file
	 *            The file to index, or the directory to recurse into to find
	 *            files to index
	 * @throws IOException
	 *             If there is a low-level I/O error
	 * 
	 * @see org.apache.lucene.demo.IndexFiles#indexDocs(IndexWriter, File)
	 */
	private void indexDocs(IndexWriter writer, File file) throws IOException {
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
						indexDocs(writer, new File(file, files[i]));
					}
				}
			} else {

				FileInputStream fis;
				try {
					fis = new FileInputStream(file);
				} catch (FileNotFoundException fnfe) {
					return;
				}

				try {

					// make a new, empty document
					Document doc = new Document();

					// Add the path of the file as a field named "path". Use a
					// field that is indexed (i.e. searchable), but don't
					// tokenize
					// the field into separate words and don't index term
					// frequency
					// or positional information:
					Field pathField = new StringField(FIELD_PATH,
							file.getPath(), Field.Store.YES);
					doc.add(pathField);

					// custom field type
					FieldType type = new FieldType();
					type.setIndexed(true);
					type.setTokenized(true);
					type.setStored(true);
					type.setStoreTermVectors(true);

					Field f = new Field(FIELD_CONTENTS, IOUtils.toString(fis),
							type);

					doc.add(f);

					LOGGER.debug("adding " + file);
					writer.addDocument(doc);

				} finally {
					fis.close();
				}
			}
		}
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

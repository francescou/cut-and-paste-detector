package it.uliana.cpd;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.shingle.ShingleAnalyzerWrapper;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class Detector {

    private final static Logger LOGGER = LoggerFactory.getLogger(Detector.class);

    private static final int MIN_DOC_FREQUENCY = 1;
    private static final String FIELD_CONTENTS = "contents";
    private static final String FIELD_PATH = "path";

    public Detector() {
    }

    /**
     * Detect duplicated content between pairs of files in a folder.
     *
     * @param kGramSize           the size of the kGram used to index the collection. Suggested
     *                            value: between 6 and 10.
     * @param similarityThreshold the similarity threshold above which two documents will be
     *                            considered "duplicated".
     * @return a list containing all the document pairs who have duplicated
     * content
     */
    public List<Pair> getDuplicates(int kGramSize,
                                    int similarityThreshold, Map<String, InputStream> files) {

        Directory directory = new RAMDirectory();
        index(files, kGramSize, directory);

        Date d = new Date();

        Map<String, Set<String>> collection;
        try {
            collection = convertDocumentsToMap(directory);

            LOGGER.debug(new Date().getTime() - d.getTime()
                    + " milliseconds to create matrix");

            d = new Date();

            List<Pair> duplicates = checkCPD(collection, similarityThreshold);

            LOGGER.debug(duplicates.size() + " duplicated elements");

            LOGGER.debug(new Date().getTime() - d.getTime()
                    + " milliseconds to check duplicated content");

            return duplicates;
        } catch (IOException e) {
            throw new RuntimeException("error while creating document maps", e);
        }
    }

    /**
     * Convert documents to Map in which keys are documents path and values are
     * the set of n-grams.
     *
     * @return Map in which keys are documents path and values are the set of
     * n-grams.
     * @throws IOException
     */
    private Map<String, Set<String>> convertDocumentsToMap(Directory directory) throws IOException {

        IndexReader reader = DirectoryReader.open(directory);

        Map<String, Set<String>> collection = new HashMap<String, Set<String>>();

        for (int j = 0; j < reader.numDocs(); j++) {
            Set<String> terms = new HashSet<String>();
            String path = reader.document(j).getField(FIELD_PATH).stringValue();

            LOGGER.debug("DOCUMENT " + j + ": " + path);
            Terms tv = reader.getTermVector(j, FIELD_CONTENTS);

            if (tv == null) {
                continue;
            }

            TermsEnum i = tv.iterator();

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

                    Set<String> intersection = collection.get(d1).stream().filter(collection.get(d2)::contains).collect(Collectors.toSet());

                    int score = intersection.size();
                    if (score > similarityThreshold) {
                        Pair p = new Pair(d1, d2, score);
                        duplicates.add(p);
                        LOGGER.debug(p.toString());
                        LOGGER.debug(intersection.toString());
                    }
                }
            }
        }

        return duplicates;
    }

    /**
     * Index all text files under a directory.
     */
    private void index(Map<String, InputStream> files, int kGramSize, Directory directory) {

        Date start = new Date();
        try {
            LOGGER.info("Indexing");

            Analyzer analyzer = new StandardAnalyzer();

            // index shingles
            analyzer = new ShingleAnalyzerWrapper(analyzer, kGramSize,
                    kGramSize, ShingleFilter.DEFAULT_TOKEN_SEPARATOR, false, false, "_");

            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);

            iwc.setOpenMode(OpenMode.CREATE);

            // iwc.setRAMBufferSizeMB(256.0);

            IndexWriter writer = new IndexWriter(directory, iwc);

            for (Map.Entry<String, InputStream> f : files.entrySet()) {
                indexDocs(writer, f.getValue(), f.getKey());
            }

            writer.close();

            Date end = new Date();
            LOGGER.info(end.getTime() - start.getTime()
                    + " milliseconds to index collection");

        } catch (IOException e) {
            LOGGER.info(" caught a " + e.getClass() + "\n with message: "
                    + e.getMessage());
        }
    }

    private void indexDocs(IndexWriter writer, InputStream inputStream, String id) throws IOException {


        try {

            // make a new, empty document
            Document doc = new Document();

            // Add the path of the file as a field named "path". Use a
            // field that is indexed (i.e. searchable), but don't
            // tokenize
            // the field into separate words and don't index term
            // frequency
            // or positional information:
            Field pathField = new StringField(FIELD_PATH, id, Field.Store.YES);
            doc.add(pathField);

            // custom field type
            FieldType type = new FieldType();
            type.setIndexOptions(IndexOptions.DOCS); //TODO: scegliere bene
            type.setTokenized(true);
            type.setStored(true);
            type.setStoreTermVectors(true);

            Field f = new Field(FIELD_CONTENTS, IOUtils.toString(inputStream), type);

            doc.add(f);

            LOGGER.debug("adding " + id);
            writer.addDocument(doc);

        } finally {
            inputStream.close();
        }

    }


}

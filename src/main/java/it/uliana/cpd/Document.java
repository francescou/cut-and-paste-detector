package it.uliana.cpd;

import java.util.Set;

/**
 * Created by francesco on 14/12/15.
 */
public class Document {

    private int vocabularySize;

    private Set<String> terms;


    public Document(int vocabularySize, Set<String> terms) {
        this.vocabularySize = vocabularySize;
        this.terms = terms;
    }

    public int getVocabularySize() {
        return vocabularySize;
    }

    public Set<String> getTerms() {
        return terms;
    }

}

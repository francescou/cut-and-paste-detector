package it.uliana.cpd;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.AnalyzerWrapper;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.KeywordTokenizer;

/**
 * Created by francesco on 01/12/15.
 */
public class MinLengthAnalyzer extends AnalyzerWrapper {

    private final Analyzer delegate;
    private int min;

    protected MinLengthAnalyzer(Analyzer delegate, int min) {
        super(delegate.getReuseStrategy());
        this.delegate = delegate;
        this.min = min;
    }

    @Override
    protected Analyzer getWrappedAnalyzer(String fieldName) {
        return delegate;
    }

    @Override
    protected TokenStreamComponents wrapComponents(String fieldName, TokenStreamComponents components) {
        MinLengthFilter filter = new MinLengthFilter(components.getTokenStream(), min);
        Tokenizer tokenizer = components.getTokenizer();
        return new TokenStreamComponents(tokenizer, filter);
    }

}

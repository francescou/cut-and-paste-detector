package it.uliana.cpd;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl;
import org.apache.lucene.analysis.util.FilteringTokenFilter;

import java.io.IOException;

/**
 * Created by francesco on 01/12/15.
 */
public class MinLengthFilter extends FilteringTokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private int min;

    public MinLengthFilter(TokenStream in, int min) {
        super(in);
        this.min = min;
    }

    @Override
    protected boolean accept() throws IOException {
        int length = ((PackedTokenAttributeImpl) termAtt).length();
        return length >= min;
    }
}

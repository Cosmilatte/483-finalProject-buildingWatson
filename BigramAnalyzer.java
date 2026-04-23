package javaFinal;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;

public class BigramAnalyzer extends Analyzer {
    @Override
    protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tokenizer = new StandardTokenizer();
        TokenStream stream = new LowerCaseFilter(tokenizer);
        ShingleFilter shingle = new ShingleFilter(stream, 2, 2); // bigrams only
        shingle.setOutputUnigrams(false); // only bigrams
        return new TokenStreamComponents(tokenizer, shingle);
    }
}

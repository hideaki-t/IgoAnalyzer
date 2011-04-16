package net.sf.igoanalyzer;

import java.io.Reader;
import java.util.Set;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseBasicFormFilter;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilter;
import org.apache.lucene.util.Version;

/**
 *
 * @author hideaki
 */
public class IgoAnalyzer extends StopwordAnalyzerBase {
    private final Set<String> stopTags;

    public IgoAnalyzer(Version version, Set<String> stopWords,
            Set<String> stopTags) {
        super(version, stopWords);
        this.stopTags = stopTags;
    }

    @Override
    protected TokenStreamComponents createComponents(String field, Reader reader) {
        Tokenizer tokenizer = new IgoTokenizer(reader);
        TokenStream stream = new org.apache.lucene.analysis.ja.JapanesePunctuationFilter(true, tokenizer);
        stream = new JapaneseBasicFormFilter(tokenizer);
        stream = new JapanesePartOfSpeechStopFilter(true, tokenizer, stopTags);
        stream = new StopFilter(matchVersion, stream, stopwords);
        stream = new LowerCaseFilter(matchVersion, stream);
        stream = new JapaneseKatakanaStemFilter(stream);
        return new TokenStreamComponents(tokenizer, stream);
    }
}

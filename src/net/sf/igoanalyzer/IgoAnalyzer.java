package net.sf.igoanalyzer;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import org.apache.lucene.analysis.KeywordMarkerFilter;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.StopwordAnalyzerBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.JapaneseBasicFormFilter;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.apache.lucene.analysis.ja.JapanesePartOfSpeechStopFilter;
import org.apache.lucene.analysis.ja.JapanesePunctuationFilter;
import org.apache.lucene.analysis.ja.JapaneseWidthFilter;
import org.apache.lucene.util.Version;

/**
 * @author Takahashi Hideaki <mymelo@gmail.com>
 */
public class IgoAnalyzer extends StopwordAnalyzerBase {

    private final Set<String> stopTags;
    private final Set<String> stemExclusionSet;

    public IgoAnalyzer(Version version, Set<String> stopWords,
            Set<String> stopTags, Set<String> stemExclusionSet) {
        super(version, stopWords);
        this.stopTags = stopTags;
        this.stemExclusionSet = stemExclusionSet;
    }

    @Override
    protected TokenStreamComponents createComponents(String field, Reader reader) {
        try {
            Tokenizer tokenizer = new IgoTokenizer(reader);
            TokenStream stream = new JapaneseWidthFilter(tokenizer);
            stream = new JapanesePunctuationFilter(true, stream);
            stream = new JapanesePartOfSpeechStopFilter(true, stream, stopTags);
            stream = new StopFilter(matchVersion, stream, stopwords);
            if (!stemExclusionSet.isEmpty()) {
                stream = new KeywordMarkerFilter(stream, stemExclusionSet);
            }
            stream = new JapaneseBasicFormFilter(stream);
            stream = new JapaneseKatakanaStemFilter(stream);
            stream = new LowerCaseFilter(matchVersion, stream);
            return new TokenStreamComponents(tokenizer, stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

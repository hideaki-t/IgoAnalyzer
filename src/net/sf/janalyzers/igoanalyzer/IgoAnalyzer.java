package net.sf.janalyzers.igoanalyzer;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;
import net.reduls.igo.Tagger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.Version;

/**
 *
 * @author hideaki
 */
public class IgoAnalyzer extends Analyzer {
    private final Version matchVersion;
    private final Set<?> stopTable;
    private final Tagger tagger;

    public IgoAnalyzer(Version matchVersion, String path, Set<String> stopWords) throws IOException {
        this.matchVersion = matchVersion;
        stopTable = CharArraySet.unmodifiableSet(new CharArraySet(stopWords, true));
//        stopTable = CharArraySet.unmodifiableSet(CharArraySet.copy(stopWords));
        tagger = new Tagger(path);
    }

    private class SavedStreams {
        final Tokenizer source;
        final TokenStream result;

        public SavedStreams(Tokenizer source, TokenStream result) {
            this.source = source;
            this.result = result;
        }
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return new StopFilter(StopFilter.getEnablePositionIncrementsVersionDefault(matchVersion),
                new IgoTokenizer(reader, tagger), stopTable);
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
        SavedStreams streams = (SavedStreams)getPreviousTokenStream();
        if (streams == null) {
            IgoTokenizer i = new IgoTokenizer(reader, tagger);
            TokenStream s = new StopFilter(StopFilter.getEnablePositionIncrementsVersionDefault(matchVersion), i, stopTable);
            streams = new SavedStreams(i, s);
            setPreviousTokenStream(streams);
        } else {
            streams.source.reset(reader);
        }
        return streams.result;
    }
}


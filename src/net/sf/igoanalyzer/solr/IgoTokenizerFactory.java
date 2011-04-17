package net.sf.igoanalyzer.solr;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import net.sf.igoanalyzer.IgoTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.solr.analysis.BaseTokenizerFactory;

/**
 * Tokenizer factory for IgoTokenizer.
 * @author hideaki
 */
public class IgoTokenizerFactory extends BaseTokenizerFactory {

    /**
     * path of dictionary.
     */
    private String dicPath;

    @Override
    public void init(final Map<String, String> args) {
        super.init(args);
        dicPath = args.get("dicPath");
    }

    @Override
    public Tokenizer create(final Reader reader) {
        try {
            if (dicPath == null) {
                return new IgoTokenizer(reader);
            }
            return new IgoTokenizer(reader, dicPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

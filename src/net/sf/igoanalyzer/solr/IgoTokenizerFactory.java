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
            return new IgoTokenizer(reader, dicPath);
        } catch (IOException e) {
            throw new FactoryException(e);
        }
    }

    /**
     * Thrown if instantiation failed.
     */
    private static final class FactoryException extends RuntimeException {

        /**
         * @param cause Nested exception.
         */
        FactoryException(final Exception cause) {
            super(cause);
        }
    }
}

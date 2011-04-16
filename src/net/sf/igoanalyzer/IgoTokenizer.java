/**
 * Takahashi Hideaki <mymelo@gmail.com>
 */
package net.sf.igoanalyzer;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.reduls.igo.Morpheme;
import net.reduls.igo.Tagger;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.NormalizeReader;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 * Igoを使ったTokenizer.
 */
public final class IgoTokenizer extends Tokenizer {

    /** 形態素解析器. */
    private final Tagger tagger;
    /** 1フレーズ分の形態素解析結果. */
    private final LinkedList<Morpheme> remainMorphemes = new LinkedList<Morpheme>();

    // attributes
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
    private final TypeAttribute typeAttr = addAttribute(TypeAttribute.class);
    //private final PositionIncrementAttribute incrAttr = addAttribute(PositionIncrementAttribute.class);
    
    private final Pattern punctuation = Pattern.compile(".+\\p{Po}");
    /** 今のオフセット */
    private int offset;
    /** 次回のオフセット */
    private int nextOffset;
    private final StringBuilder buf = new StringBuilder();

    /**
     * Igoで使うバイナリ辞書の場所を指定してインスタンスを作成する
     * @param input
     * @param path バイナリ辞書のパス
     */
    public IgoTokenizer(final Reader input, final String path) {
        super(new NormalizeReader(input));
        try {
            this.tagger = new Tagger(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public IgoTokenizer(final Reader input) {
        super(new NormalizeReader(input));
        try {
            String path = Tagger.class.getResource("/ipadic").getPath();
            this.tagger = new Tagger(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 形態素解析器のインスタンスを指定してインスタンスを作成する
     * @param input
     * @param tagger 形態素解析器のインスタンス
     */
    public IgoTokenizer(final Reader input, final Tagger tagger) {
        super(input);
        this.tagger = tagger;
    }

    /**
     * 形態素解析器のインスタンスを指定してインスタンスを作成する
     * @param source
     * @param input
     * @param tagger 形態素解析器のインスタンス
     */
    public IgoTokenizer(final AttributeSource source,
            final Reader input, final Tagger tagger) {
        super(source, input);
        this.tagger = tagger;
    }

    /**
     * 形態素解析器のインスタンスを指定してインスタンスを作成する
     * @param factory
     * @param input
     * @param tagger 形態素解析器のインスタンス
     */
    public IgoTokenizer(final AttributeFactory factory,
            final Reader input, final Tagger tagger) {
        super(factory, input);
        this.tagger = tagger;
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        if (remainMorphemes.isEmpty()) {
            if (!parse()) {
                return false;
            }
        }
        final Morpheme morpheme = remainMorphemes.removeFirst();
        final int start = offset + morpheme.start;
        final int end = start + morpheme.surface.length();
        termAttr.setEmpty().append(morpheme.surface);
        offsetAttr.setOffset(
                correctOffset(start),
                correctOffset(end));
        typeAttr.setType(morpheme.feature);
        //incrAttr.setPositionIncrement(1);
        // FlagsAttribute
        // PayloadAttribute
        return true;
    }

    @Override
    public void end() throws IOException {
        final int endOffset = correctOffset(nextOffset);
        offsetAttr.setOffset(endOffset, endOffset);
    }

    @Override
    public void reset(final Reader input) throws IOException {
        super.reset(input);
        remainMorphemes.clear();
        buf.setLength(0);
        offset = 0;
        nextOffset = 0;
    }

    private final boolean read(final Matcher matcher) throws IOException {
        final char[] tbuf = new char[8];
        boolean eof = false;
        while (!eof && !matcher.find()) {
            final int r = input.read(tbuf);
            if (r == -1) {
                eof = true;
            } else {
                buf.append(tbuf, 0, r);
                matcher.reset(buf);
            }
        }
        return eof;
    }

    private void p(final Matcher matcher) {
        tagger.parse(matcher.group(), remainMorphemes);
        offset = nextOffset;
        nextOffset = offset + matcher.group().length();
        buf.delete(0, matcher.end());
    }

    /**
     * ストリームからデータを読み込み、形態素解析をする。
     * @return 解析済み形態素が存在するならture.
     * @throws IOException
     */
    private boolean parse() throws IOException {
        final Matcher matcher = punctuation.matcher(buf);
        if (matcher.find()) {
            p(matcher);
        } else {
            if (read(matcher)) {
                if (buf.length() != 0) {
                    tagger.parse(buf, remainMorphemes);
                    offset = nextOffset;
                    nextOffset = offset + buf.length();
                    buf.setLength(0);
                }
            } else {
                p(matcher);
            }
        }
        return !remainMorphemes.isEmpty();
    }
}

/**
 * Takahashi Hideaki <mymelo@gmail.com>
 */
package net.sf.igoanalyzer;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.reduls.igo.Morpheme;
import net.reduls.igo.Tagger;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
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
    private final OffsetAttribute offsetAttr;
    private final TermAttribute termAttr;
    private final PositionIncrementAttribute incrAttr;
    private final TypeAttribute typeAttr;
    private final Pattern punctuation = Pattern.compile(".+\\p{Po}");
    /** 今のオフセット */
    private int offset;
    /** 次回のオフセット */
    private int nextOffset;
    private final StringBuilder buf = new StringBuilder();
    // TODO: to be configurable

    /**
     * Igoで使うバイナリ辞書の場所を指定してインスタンスを作成する
     * @param input
     * @param path バイナリ辞書のパス
     * @throws IOException バイナリ辞書読み込み中にエラーが起きると発生する
     */
    public IgoTokenizer(final Reader input, final String path) throws IOException {
        this(input, new Tagger(path));
    }

    /**
     * 形態素解析器のインスタンスを指定してインスタンスを作成する
     * @param input
     * @param tagger 形態素解析器のインスタンス
     */
    public IgoTokenizer(final Reader input, final Tagger tagger) {
        super(input);
        this.tagger = tagger;
        offsetAttr = (OffsetAttribute) addAttribute(OffsetAttribute.class);
        termAttr = (TermAttribute) addAttribute(TermAttribute.class);
        incrAttr = (PositionIncrementAttribute) addAttribute(PositionIncrementAttribute.class);
        typeAttr = (TypeAttribute) addAttribute(TypeAttribute.class);
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
        offsetAttr = (OffsetAttribute) addAttribute(OffsetAttribute.class);
        termAttr = (TermAttribute) addAttribute(TermAttribute.class);
        incrAttr = (PositionIncrementAttribute) addAttribute(PositionIncrementAttribute.class);
        typeAttr = (TypeAttribute) addAttribute(TypeAttribute.class);
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
        offsetAttr = (OffsetAttribute) addAttribute(OffsetAttribute.class);
        termAttr = (TermAttribute) addAttribute(TermAttribute.class);
        incrAttr = (PositionIncrementAttribute) addAttribute(PositionIncrementAttribute.class);
        typeAttr = (TypeAttribute) addAttribute(TypeAttribute.class);
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
        final int len = morpheme.surface.length();
        char[] buffer = termAttr.termBuffer();
        if (len > buffer.length) {
            buffer = termAttr.resizeTermBuffer(len);
        }
        morpheme.surface.getChars(0, len, buffer, 0);
        termAttr.setTermLength(len);
        final int start = offset + morpheme.start;
        final int end = start + len;
        offsetAttr.setOffset(
                correctOffset(start),
                correctOffset(end));
        incrAttr.setPositionIncrement(1);
        typeAttr.setType(morpheme.feature);
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
    public void reset() throws IOException {
        super.reset();
        remainMorphemes.clear();
        buf.setLength(0);
        offset = 0;
        nextOffset = 0;
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

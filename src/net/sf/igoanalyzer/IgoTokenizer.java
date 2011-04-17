package net.sf.igoanalyzer;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.reduls.igo.Morpheme;
import net.reduls.igo.Tagger;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ja.tokenAttributes.BasicFormAttribute;
import org.apache.lucene.analysis.ja.tokenAttributes.ConjugationAttribute;
import org.apache.lucene.analysis.ja.tokenAttributes.PartOfSpeechAttribute;
import org.apache.lucene.analysis.ja.tokenAttributes.PronunciationsAttribute;
import org.apache.lucene.analysis.ja.tokenAttributes.ReadingsAttribute;
import org.apache.lucene.analysis.ja.tokenAttributes.SentenceStartAttribute;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * Igoを使ったTokenizer.
 * @author Takahashi Hideaki <mymelo@gmail.com>
 */
public final class IgoTokenizer extends Tokenizer {

    /** 形態素解析器. */
    private final Tagger tagger;
    /** 1フレーズ分の形態素解析結果. */
    private final LinkedList<MorphemeHolder> remainMorphemes = new LinkedList<MorphemeHolder>();

    // attributes
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);
    private final OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
    private final TypeAttribute typeAttr = addAttribute(TypeAttribute.class);
    //private final PositionIncrementAttribute incrAttr = addAttribute(PositionIncrementAttribute.class);

    // lucene-gosen attribues
    private final BasicFormAttribute basicFormAttr = addAttribute(BasicFormAttribute.class);
    private final ConjugationAttribute conjugationAttr = addAttribute(ConjugationAttribute.class);
    private final PartOfSpeechAttribute partOfSpeechAttr = addAttribute(PartOfSpeechAttribute.class);
    private final PronunciationsAttribute pronunciationsAttr = addAttribute(PronunciationsAttribute.class);
    private final ReadingsAttribute readingsAttr = addAttribute(ReadingsAttribute.class);

    private final SentenceStartAttribute sentenceAttr = addAttribute(SentenceStartAttribute.class);

    private final Pattern punctuation = Pattern.compile(".+?\\p{Po}+");
    /** 今のオフセット */
    private int offset;
    /** 次回のオフセット */
    private int nextOffset;
    private final StringBuilder buf = new StringBuilder(1024);

    /**
     * Igoで使うバイナリ辞書の場所を指定してインスタンスを作成する
     * @param input
     * @param path バイナリ辞書のパス
     * @throws IOException
     */
    public IgoTokenizer(final Reader input, final String path) throws IOException {
        super(input);
        this.tagger = new Tagger(path);
    }

    /**
     * Igoで使うバイナリ辞書をクラスパスから探して、インスタンスを作成する
     * @param input
     * @throws IOException
     */
    public IgoTokenizer(final Reader input) throws IOException {
        super(input);
        String path = Tagger.class.getResource("/ipadic").getPath();
        this.tagger = new Tagger(path);
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

    @Override
    public boolean incrementToken() throws IOException {
        if (remainMorphemes.isEmpty() && !parse()) {
            return false;
        }
        clearAttributes();
        MorphemeHolder holder = remainMorphemes.removeFirst();
        final Morpheme morpheme = holder.morpheme;
        final int start = offset + morpheme.start;
        final int end = start + morpheme.surface.length();
        termAttr.setEmpty().append(morpheme.surface);
        offsetAttr.setOffset(
                correctOffset(start),
                correctOffset(end));
        //incrAttr.setPositionIncrement(1);
        setMorphologicalAttributes(morpheme);
        typeAttr.setType(partOfSpeechAttr.getPartOfSpeech());
        sentenceAttr.setSentenceStart(holder.sentenceStart);
        // FlagsAttribute
        // PayloadAttribute
        return true;
    }

    private void setMorphologicalAttributes(Morpheme morpheme) {
        // 品詞,品詞細分類1,品詞細分類2,品詞細分類3,活用形,活用型,原形,読み,発音
        StringTokenizer st = new StringTokenizer(morpheme.feature, ",");
        StringBuilder pos = new StringBuilder(st.nextToken());
        pos.append('-').append(st.nextToken());
        pos.append('-').append(st.nextToken());
        pos.append('-').append(st.nextToken());
        partOfSpeechAttr.setPartOfSpeech(pos.toString().replaceAll("-\\*", ""));
        conjugationAttr.setConjugationalType(st.nextToken());
        conjugationAttr.setConjugationalForm(st.nextToken());
        basicFormAttr.setBasicForm(st.nextToken());
        readingsAttr.setReadings(Arrays.asList(st.nextToken()));
        pronunciationsAttr.setPronunciations(Arrays.asList(st.nextToken()));
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

    private boolean read(final Matcher matcher) throws IOException {
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

    private void parse(final CharSequence seq) {
        List<Morpheme> ml = tagger.parse(seq);
        if (!ml.isEmpty()) {
            remainMorphemes.add(new MorphemeHolder(ml.remove(0), true));
        }
        for (Morpheme m: ml) {
            remainMorphemes.add(new MorphemeHolder(m, false));
        }
    }

    private void parse(final Matcher matcher) {
        parse(matcher.group());
        offset = nextOffset;
        nextOffset = offset + matcher.group().length();
        buf.delete(0, matcher.end());
    }

    private void parseAtEof(final StringBuilder buf) {
        parse(buf);
        offset = nextOffset;
        nextOffset = offset + buf.length();
        buf.setLength(0);
    }

    /**
     * ストリームからデータを読み込み、形態素解析をする。
     * @return 解析済み形態素が存在するならture.
     * @throws IOException
     */
    private boolean parse() throws IOException {
        final Matcher matcher = punctuation.matcher(buf);
        if (matcher.find()) {
            parse(matcher);
        } else {
            if (read(matcher)) {
                // EOF
                if (buf.length() != 0) {
                    parseAtEof(buf);
                }
            } else {
                parse(matcher);
            }
        }
        return !remainMorphemes.isEmpty();
    }
}

package net.sf.igoanalyzer;

import net.reduls.igo.Morpheme;

/**
 * @author Takahashi Hideaki <mymelo@gmail.com>
 */
public class MorphemeHolder {
    public final Morpheme morpheme;
    public final boolean sentenceStart;

    public MorphemeHolder(Morpheme morpheme, boolean sentenceStart) {
        this.morpheme = morpheme;
        this.sentenceStart = sentenceStart;
    }
}

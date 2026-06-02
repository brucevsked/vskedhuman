package com.vsked.human.dna.dict.leu;

import com.vsked.human.dna.word.Codon;

public class TTA {
    private Codon leu;

    public TTA(Codon leu) {
        String word = leu.getWord();
        if (!word.equals("TTA")) {
            throw new IllegalArgumentException("Leu (Leucine) must be TTA, TTG, CTT, CTC, CTA, CTG");
        }
        this.leu = leu;
    }

    public Codon getLeu() {
        return leu;
    }
}

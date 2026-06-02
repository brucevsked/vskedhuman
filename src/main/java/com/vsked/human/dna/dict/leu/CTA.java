package com.vsked.human.dna.dict.leu;

import com.vsked.human.dna.word.Codon;

public class CTA {
    private Codon leu;

    public CTA(Codon leu) {
        String word = leu.getWord();
        if (!word.equals("CTA")) {
            throw new IllegalArgumentException("Leu (Leucine) must be TTA, TTG, CTT, CTC, CTA, CTG");
        }
        this.leu = leu;
    }

    public Codon getLeu() {
        return leu;
    }
}

package com.vsked.human.dna.dict.leu;

import com.vsked.human.dna.word.Codon;

public class CTC {
    private Codon leu;

    public CTC(Codon leu) {
        String word = leu.getWord();
        if (!word.equals("CTC")) {
            throw new IllegalArgumentException("Leu (Leucine) must be TTA, TTG, CTT, CTC, CTA, CTG");
        }
        this.leu = leu;
    }

    public Codon getLeu() {
        return leu;
    }
}

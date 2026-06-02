package com.vsked.human.dna.dict.leu;

import com.vsked.human.dna.word.Codon;

public class CTG {
    private Codon leu;

    public CTG(Codon leu) {
        String word = leu.getWord();
        if (!word.equals("CTG")) {
            throw new IllegalArgumentException("Leu (Leucine) must be TTA, TTG, CTT, CTC, CTA, CTG");
        }
        this.leu = leu;
    }

    public Codon getLeu() {
        return leu;
    }
}

package com.vsked.human.dna.dict.tyr;

import com.vsked.human.dna.word.Codon;

public class TAC {
    private Codon tyr;

    public TAC(Codon tyr) {
        String word = tyr.getWord();
        if (!word.equals("TAC")) {
            throw new IllegalArgumentException("Tyr (Tyrosine) must be TAT, TAC");
        }
        this.tyr = tyr;
    }

    public Codon getTyr() {
        return tyr;
    }
}

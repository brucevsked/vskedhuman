package com.vsked.human.dna.dict.tyr;

import com.vsked.human.dna.word.Codon;

public class TAT {
    private Codon tyr;

    public TAT(Codon tyr) {
        String word = tyr.getWord();
        if (!word.equals("TAT")) {
            throw new IllegalArgumentException("Tyr (Tyrosine) must be TAT, TAC");
        }
        this.tyr = tyr;
    }

    public Codon getTyr() {
        return tyr;
    }
}

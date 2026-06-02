package com.vsked.human.dna.dict.thr;

import com.vsked.human.dna.word.Codon;

public class ACT {
    private Codon thr;

    public ACT(Codon thr) {
        String word = thr.getWord();
        if (!word.equals("ACT")) {
            throw new IllegalArgumentException("Thr must be ACT, ACC, ACA, ACG");
        }
        this.thr = thr;
    }

    public Codon getThr() {
        return thr;
    }
}

package com.vsked.human.dna.dict.thr;

import com.vsked.human.dna.word.Codon;

public class ACG {
    private Codon thr;

    public ACG(Codon thr) {
        String word = thr.getWord();
        if (!word.equals("ACG")) {
            throw new IllegalArgumentException("Thr must be ACT, ACC, ACA, ACG");
        }
        this.thr = thr;
    }

    public Codon getThr() {
        return thr;
    }
}

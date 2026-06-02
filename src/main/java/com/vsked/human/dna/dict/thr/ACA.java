package com.vsked.human.dna.dict.thr;

import com.vsked.human.dna.word.Codon;

public class ACA {
    private Codon thr;

    public ACA(Codon thr) {
        String word = thr.getWord();
        if (!word.equals("ACA")) {
            throw new IllegalArgumentException("Thr must be ACT, ACC, ACA, ACG");
        }
        this.thr = thr;
    }

    public Codon getThr() {
        return thr;
    }
}

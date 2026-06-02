package com.vsked.human.dna.dict.met;

import com.vsked.human.dna.word.Codon;

/**
 * start signal
 */
public class ATG {
    private Codon met;

    public ATG(Codon met) {
        String word = met.getWord();
        if (!word.equals("ATG")) {
            throw new IllegalArgumentException("Met must be ATG");
        }
        this.met = met;
    }

    public Codon getMet() {
        return met;
    }
}

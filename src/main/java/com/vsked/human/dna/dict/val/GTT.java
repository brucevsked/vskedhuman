package com.vsked.human.dna.dict.val;

import com.vsked.human.dna.word.Codon;

public class GTT {
    private Codon val;

    public GTT(Codon val) {
        String word = val.getWord();
        if (!word.equals("GTT")) {
            throw new IllegalArgumentException("Val GTT must be GTT, GTC, GTA, GTG");
        }
        this.val = val;
    }

    public Codon getVal() {
        return val;
    }
}

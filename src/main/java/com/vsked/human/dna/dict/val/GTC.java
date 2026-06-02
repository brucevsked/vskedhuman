package com.vsked.human.dna.dict.val;

import com.vsked.human.dna.word.Codon;

public class GTC {
    private Codon val;

    public GTC(Codon val) {
        String word = val.getWord();
        if (!word.equals("GTC")) {
            throw new IllegalArgumentException("Val GTC must be GTT, GTC, GTA, or GTG");
        }
        this.val = val;
    }

    public Codon getVal() {
        return val;
    }
}

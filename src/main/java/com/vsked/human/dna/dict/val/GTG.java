package com.vsked.human.dna.dict.val;

import com.vsked.human.dna.word.Codon;

public class GTG {
    private Codon val;

    public GTG(Codon val) {
        String word = val.getWord();
        if (!word.equals("GTG")) {
            throw new IllegalArgumentException("Val GTG must be GTT, GTC, GTA, or GTG");
        }
        this.val = val;
    }

    public Codon getVal() {
        return val;
    }
}

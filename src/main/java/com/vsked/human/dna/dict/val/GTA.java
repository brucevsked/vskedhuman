package com.vsked.human.dna.dict.val;

import com.vsked.human.dna.word.Codon;

public class GTA {
    private Codon val;

    public GTA(Codon val) {
        String word = val.getWord();
        if (!word.equals("GTA")) {
            throw new IllegalArgumentException("Val GTA must be GTT, GTC, GTA, or GTG");
        }
        this.val = val;
    }

    public Codon getVal() {
        return val;
    }
}

package com.vsked.human.dna.dict.phe;

import com.vsked.human.dna.word.Codon;

public class TTT {
    private Codon phe;

    public TTT(Codon phe) {
        String word = phe.getWord();
        if (!word.equals("TTT")) {
            throw new IllegalArgumentException("Phe must be TTT or TTC");
        }
        this.phe = phe;
    }

    public Codon getPhe() {
        return phe;
    }
}

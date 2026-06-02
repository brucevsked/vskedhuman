package com.vsked.human.dna.dict.phe;

import com.vsked.human.dna.word.Codon;

public class TTC {
    private Codon phe;

    public TTC(Codon phe) {
        String word = phe.getWord();
        if (!word.equals("TTC")) {
            throw new IllegalArgumentException("Phe must be TTC or TTT");
        }
        this.phe = phe;
    }

    public Codon getPhe() {
        return phe;
    }
}

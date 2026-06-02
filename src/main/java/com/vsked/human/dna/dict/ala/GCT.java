package com.vsked.human.dna.dict.ala;

import com.vsked.human.dna.word.Codon;

public class GCT {
    private Codon ala;

    public GCT(Codon ala) {
        String word = ala.getWord();
        if (!word.equals("GCT")) {
            throw new IllegalArgumentException("Ala (Alanine) must be GCT, GCC, GCA, GCG");
        }
        this.ala = ala;
    }

    public Codon getAla() {
        return ala;
    }
}

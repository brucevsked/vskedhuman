package com.vsked.human.dna.dict.ala;

import com.vsked.human.dna.word.Codon;

public class GCC {
    private Codon ala;

    public GCC(Codon ala) {
        String word = ala.getWord();
        if (!word.equals("GCC")) {
            throw new IllegalArgumentException("Ala (Alanine) must be GCT, GCC, GCA, GCG");
        }
        this.ala = ala;
    }

    public Codon getAla() {
        return ala;
    }
}

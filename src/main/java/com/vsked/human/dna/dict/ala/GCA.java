package com.vsked.human.dna.dict.ala;

import com.vsked.human.dna.word.Codon;

public class GCA {
    private Codon ala;

    public GCA(Codon ala) {
        String word = ala.getWord();
        if (!word.equals("GCA")) {
            throw new IllegalArgumentException("Ala (Alanine) must be GCT, GCC, GCA, GCG");
        }
        this.ala = ala;
    }

    public Codon getAla() {
        return ala;
    }
}

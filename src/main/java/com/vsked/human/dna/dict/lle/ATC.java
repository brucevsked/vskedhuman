package com.vsked.human.dna.dict.lle;

import com.vsked.human.dna.word.Codon;

public class ATC {
    private Codon lle;

    public ATC(Codon lle) {
        String word = lle.getWord();
        if (!word.equals("ATC")) {
            throw new IllegalArgumentException("Ile must be ATT, ATC, or ATA");
        }
        this.lle = lle;
    }

    public Codon getLle() {
        return lle;
    }
}


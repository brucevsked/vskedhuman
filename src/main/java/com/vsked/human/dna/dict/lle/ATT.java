package com.vsked.human.dna.dict.lle;

import com.vsked.human.dna.word.Codon;

public class ATT {
    private Codon lle;

    public ATT(Codon lle) {
        String word = lle.getWord();
        if (!word.equals("ATT")) {
            throw new IllegalArgumentException("Ile must be ATT, ATC, ATA");
        }
        this.lle = lle;
    }

    public Codon getLle() {
        return lle;
    }
}

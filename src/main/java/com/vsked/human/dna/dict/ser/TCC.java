package com.vsked.human.dna.dict.ser;

import com.vsked.human.dna.word.Codon;

public class TCC {
    private Codon ser;

    public TCC(Codon ser) {
        String word = ser.getWord();
        if (!word.equals("TCC")) {
            throw new IllegalArgumentException("Ser must be TCT, TCC, TCA, TCG, AGT, AGC");
        }
        this.ser = ser;
    }

    public Codon getSer() {
        return ser;
    }
}

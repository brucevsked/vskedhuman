package com.vsked.human.dna.dict.ser;

import com.vsked.human.dna.word.Codon;

public class TCA {
    private Codon ser;

    public TCA(Codon ser) {
        String word = ser.getWord();
        if (!word.equals("TCA")) {
            throw new IllegalArgumentException("Ser must be TCT, TCC, TCA, TCG, AGT, AGC");
        }
        this.ser = ser;
    }

    public Codon getSer() {
        return ser;
    }
}

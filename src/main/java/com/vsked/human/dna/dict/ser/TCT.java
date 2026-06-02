package com.vsked.human.dna.dict.ser;

import com.vsked.human.dna.word.Codon;

public class TCT {
    private Codon ser;

    public TCT(Codon ser) {
        String word = ser.getWord();
        if (!word.equals("TCT")) {
            throw new IllegalArgumentException("Ser must be TCT, TCC, TCA, TCG, AGT, AGC");
        }
        this.ser = ser;
    }

    public Codon getSer() {
        return ser;
    }
}

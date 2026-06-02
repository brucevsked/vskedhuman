package com.vsked.human.dna.dict.ser;

import com.vsked.human.dna.word.Codon;

public class AGC {
    private Codon ser;

    public AGC(Codon ser) {
        String word = ser.getWord();
        if (!word.equals("AGC")) {
            throw new IllegalArgumentException("Ser must be TCT, TCC, TCA, TCG, AGT, AGC");
        }
        this.ser = ser;
    }

    public Codon getSer() {
        return ser;
    }
}

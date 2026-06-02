package com.vsked.human.dna.dict.gln;

import com.vsked.human.dna.word.Codon;

public class CAA {
    private Codon gln;

    public CAA(Codon gln) {
        String word = gln.getWord();
        if (!word.equals("CAA")) {
            throw new IllegalArgumentException("Gln (Glutamine) must be CAA, CAG");
        }
        this.gln = gln;
    }

    public Codon getGln() {
        return gln;
    }
}

package com.vsked.human.dna.dict.gln;

import com.vsked.human.dna.word.Codon;

public class CAG {
    private Codon gln;

    public CAG(Codon gln) {
        String word = gln.getWord();
        if (!word.equals("CAG")) {
            throw new IllegalArgumentException("Gln (Glutamine) must be CAA, CAG");
        }
        this.gln = gln;
    }

    public Codon getGln() {
        return gln;
    }
}

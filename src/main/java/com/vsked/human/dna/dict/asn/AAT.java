package com.vsked.human.dna.dict.asn;

import com.vsked.human.dna.word.Codon;

public class AAT {
    private Codon asn;

    public AAT(Codon asn) {
        String word = asn.getWord();
        if (!word.equals("AAT")) {
            throw new IllegalArgumentException("Asn (Asparagine) must be AAT, AAC");
        }
        this.asn = asn;
    }

    public Codon getAsn() {
        return asn;
    }
}

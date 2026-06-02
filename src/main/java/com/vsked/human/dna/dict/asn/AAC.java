package com.vsked.human.dna.dict.asn;

import com.vsked.human.dna.word.Codon;

public class AAC {
    private Codon asn;

    public AAC(Codon asn) {
        String word = asn.getWord();
        if (!word.equals("AAC")) {
            throw new IllegalArgumentException("Asn (Asparagine) must be AAT, AAC");
        }
        this.asn = asn;
    }

    public Codon getAsn() {
        return asn;
    }
}

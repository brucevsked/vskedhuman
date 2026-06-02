package com.vsked.human.dna.dict.pro;

import com.vsked.human.dna.word.Codon;

public class CCG {
    private Codon pro;

    public CCG(Codon pro) {
        String word = pro.getWord();
        if (!word.equals("CCG")) {
            throw new IllegalArgumentException("Pro must be CCT, CCC, CCA, CCG");
        }
        this.pro = pro;
    }

    public Codon getPro() {
        return pro;
    }
}

package com.vsked.human.dna.dict.his;

import com.vsked.human.dna.word.Codon;

public class CAT {
    private Codon his;

    public CAT(Codon his) {
        String word = his.getWord();
        if (!word.equals("CAT")) {
            throw new IllegalArgumentException("His (Histidine) must be CAT, CAC");
        }
        this.his = his;
    }

    public Codon getHis() {
        return his;
    }
}

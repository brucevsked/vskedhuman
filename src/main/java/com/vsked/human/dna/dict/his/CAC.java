package com.vsked.human.dna.dict.his;

import com.vsked.human.dna.word.Codon;

public class CAC {
    private Codon his;

    public CAC(Codon his) {
        String word = his.getWord();
        if (!word.equals("CAC")) {
            throw new IllegalArgumentException("His (Histidine) must be CAT, CAC");
        }
        this.his = his;
    }

    public Codon getHis() {
        return his;
    }
}

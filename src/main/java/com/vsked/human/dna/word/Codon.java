package com.vsked.human.dna.word;

public class Codon {

    private String word;

    public Codon(String word) {
        if (word.length() != 3) {
            throw new IllegalArgumentException("Codon must be 3 characters long");
        }
        this.word = word;
    }

    public String getWord() {
        return word;
    }
}

package datastructure;

import htsjdk.variant.variantcontext.Allele;

import java.util.List;

public class AlleleCount {
    List<Allele> alleles;
    int[] counts;

    // Constructor
    public AlleleCount(List<Allele> alleles, int[] counts) {
        this.alleles = alleles;
        this. counts = counts;
    }

    // Getter
    public List<Allele> getAlleles() {
        return this.alleles;
    }

    public int[] getCounts() {
        return this.counts;
    }

}

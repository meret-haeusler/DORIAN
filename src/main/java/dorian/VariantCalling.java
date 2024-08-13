package dorian;

import datastructure.AlleleCount;
import datastructure.Fasta;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static dorian.dorian.cor_mode;

public class VariantCalling {

    /**
     * Calls a variant
     *
     * @param baseFreq    Map of base and base frequencies
     * @param ref         Reference genome
     * @param ref_pos     1-based position in reference genome
     * @param sample_name Name of the sample
     * @return Variant for given reference position
     */
    public static VariantContext makeVariantCall(Map<Character, Double> baseFreq, Fasta ref, int ref_pos, String sample_name) {

        AlleleCount alleles_counts = getAlleleCounts(baseFreq, ref.getSequence().charAt(ref_pos - 1));

        // Build Genotype from base_counts
        GenotypeBuilder genotype = new GenotypeBuilder(sample_name + "_" + cor_mode.getShortName());
        genotype.alleles(alleles_counts.getAlleles());
        genotype.AD(alleles_counts.getCounts());
        genotype.DP(IntStream.of(alleles_counts.getCounts()).sum());
        genotype.noGQ();
        genotype.noPL();

        // Create contig name for reference
        String contig = ref.getHeader().split(" ")[0].replace(">", "");

        // Create VariantContext from genotype
        VariantContextBuilder variant = new VariantContextBuilder();
        variant.chr(contig);
        variant.start(ref_pos);
        variant.stop(ref_pos);
        variant.alleles(alleles_counts.getAlleles());
        variant.genotypes(genotype.make());


        return variant.make();
    }


    /**
     * Get a List of Alleles and their respective counts
     *
     * @param base_counts Counts of base occurrences at current position
     * @param ref         Reference base
     * @return List of Alleles and allele counts in same order
     */
    private static AlleleCount getAlleleCounts(Map<Character, Double> base_counts, Character ref) {
        // Initialise datastructures
        List<Allele> alleles = new ArrayList<>();
        List<Integer> count_list = new LinkedList<>();

        // Put ref as first element
        alleles.add(Allele.create(ref.toString(), true));
        count_list.add((int) Math.round(base_counts.getOrDefault(ref, 0.0)));

        // Add all remaining bases to output
        for (Character b : base_counts.keySet()) {
            if (base_counts.get(b) >= 0.5 && b != ref) {
                alleles.add(Allele.create(b.toString(), false));
                count_list.add((int) Math.round(base_counts.get(b)));
            }
        }

        // Covert count list to array
        int[] counts = count_list.stream().mapToInt(Integer::intValue).toArray();

        // Return as AlleleCount object
        return new AlleleCount(alleles, counts);
    }

}

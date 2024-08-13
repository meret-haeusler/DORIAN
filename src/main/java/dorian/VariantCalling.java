package dorian;

import datastructure.*;
import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.GenotypeBuilder;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;

import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static dorian.BaseCalling.*;
import static dorian.DamageCorrection.correctDamage;
import static dorian.dorian.*;

public class VariantCalling {

    static DecimalFormat df = new DecimalFormat("#.##");

    /**
     * Calls a variant
     * @param mapping_reads List of mapping reads
     * @param ref   Reference genome
     * @param ref_pos   1-based position in reference genome
     * @param sample_name   Name of the sample
     * @return  Variant for given reference position
     */
    public static VariantContext makeVariantCall(ArrayList<MappingPosition> mapping_reads, Fasta ref, int ref_pos, String sample_name) {
        Map<Character, Double> base_counts_uncor = countBaseFrequencies(mapping_reads);
        // Check if C & T from forward reads (or G & A from reverse reads) are present
        // and correct damage if necessary
        final VariantType variant_type = getVariantType(mapping_reads);
        ArrayList<MappingPosition> corrected_reads = switch (variant_type) {
            case CT, GA -> correctDamage(mapping_reads, variant_type);
            case NONE -> cloneList(mapping_reads);
        };

        // Count base occurrences
        Map<Character, Double> base_counts = countBaseFrequencies(corrected_reads);
        AlleleCount alleles_counts = getAlleleCounts(base_counts, ref.getSequence().charAt(ref_pos-1));

        // Build Genotype from base_counts
        GenotypeBuilder genotype = new GenotypeBuilder(sample_name + "_" + dorian.cor_mode.getShortName());
        genotype.alleles(alleles_counts.getAlleles());
        genotype.AD(alleles_counts.getCounts());
        genotype.DP(mapping_reads.size());  //TODO: or IntStream.of(alleles_counts.getCounts()).sum()
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

        // Add info to logger if reads were corrected
        if (variant_type.isVariant()) {
            // Get base call frequency
            double call_freq = (base_call.equals('N')) ? -1.0 : MAX_FREQ;

            file_logger.info(contig + "\t" + ref_pos + "\t" + ref.getSequence().charAt(ref_pos-1) + "\t"
                    + mapping_reads.size() + "\t" + MapToString(base_counts_uncor) + "\t" + MapToString(base_counts) + "\t"
                    + base_call + "\t" + df.format(call_freq));

            int roi_start = Math.max(ref_pos - 3, 0);
            int roi_end = Math.min(ref_pos + 2, ref.getSequence().length());
            roi_tab.info(contig + "\t" + roi_start + "\t" + roi_end + "\t" + "CORRECTED_POS:" + ref_pos);
        }

        //TODO: Remove when no longer needed
        if (cor_mode.equals(CorrectionMode.NO_COR)){
            file_logger.info(ref_pos + "\t" + base_call + "\t" + df.format(MAX_FREQ));
        }

        return variant.make();
    }


    /**
     * Get a List of Alleles and their respective counts
     * @param base_counts Counts of base occurrences at current position
     * @param ref   Reference base
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


    /**
     * Converts HashMap entries to a formatted string
     *
     * @param map Map of allele counts
     * @return String of allele counts
     */
    private static String MapToString(Map<Character, Double> map) {
        return map.keySet().stream()
                .map(key -> key + "=" + df.format(map.get(key)))
                .collect(Collectors.joining(","));
    }

    public static ArrayList<MappingPosition> cloneList(ArrayList<MappingPosition> list){
        ArrayList<MappingPosition> clonedList = new ArrayList<>();

        for (MappingPosition mp : list) {
            MappingPosition newMP = new MappingPosition(mp.base, mp.read_idx, mp.read_length, mp.is_reverse, mp.weight);
            clonedList.add(newMP);
        }

        return clonedList;
    }
}

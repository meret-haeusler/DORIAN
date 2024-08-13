package dorian;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import datastructure.*;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.SamLocusIterator;
import htsjdk.variant.variantcontext.VariantContext;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static dorian.dorian.cor_mode;

/**
 * Determines a base call based on coverage and frequency of the bases at a given position.
 *
 * @author Meret Häusler
 * @version 1.0
 * @since 2024-02-15
 */
public class BaseCalling {

    public static double MAX_FREQ;


    /**
     * Determines a base call based on coverage, frequency and damage level of the bases at a given position.
     * @param mapping_reads     List of mapping reads
     * @param min_cov           Minimal coverage threshold
     * @param min_freq          Minimal base frequency threshold
     * @return  Base call for specified ref position according to filter criteria
     */
    public static ReturnTuple makeBaseCall(File reads, int min_cov, double min_freq,
                                           Fasta ref, String sampleName) throws IOException {
        // Initialise output
        StringBuilder consensus_sequence = new StringBuilder();
        List<VariantContext> variant_calls = new ArrayList<>();

        // Iterate over bam file
        try (SamReader reader = SamReaderFactory.makeDefault().open(reads)) {
            // Initialize SamLocusIterator
            SamLocusIterator locusIterator = new SamLocusIterator(reader);
            // Iterate over each position
            for (SamLocusIterator.LocusInfo locusInfo : locusIterator) {
                int referencePosition = locusInfo.getPosition();
                ArrayList<MappingPosition> mappingReads = new ArrayList<>();

                // GET MAPPING READS //
                for (SamLocusIterator.RecordAndOffset recordAndOffset : locusInfo.getRecordAndOffsets()) {
                    SAMRecord record = recordAndOffset.getRecord();
                    mappingReads.add(MappingPosition.createMappingPosition(record, referencePosition));
                }

                // BASE CALLING //
                Character base_call;
                Map<Character, Double> cntBases = countBaseFrequencies(mappingReads);

                // Check if coverage parameter is fulfilled
                if (mappingReads.size() < min_cov) {
                    // Add variant object and make non-informative base call
                    variant_calls.add(VariantCalling.makeVariantCall(cntBases, ref, referencePosition, sampleName));
                    base_call = 'N';
                } else {

                    // TODO:
                    //  - Add switch case for correction mode
                    //  - Remove 'N' case from 'countBaseFrequencies'

                    ArrayList<MappingPosition> mappingReadsCor = new ArrayList<>(); //TODO
                    /*
                    // Check if C & T from forward reads (or G & A from reverse reads) are present
                    // and correct damage if necessary
                    final VariantType variant_type = getVariantType(mapping_reads);
                        mappingReadsCor = switch (variant_type) {
                        case CT, GA -> correctDamage(mapping_reads, variant_type);
                        case NONE -> cloneList(mapping_reads);
                    };
                     */

                    // Count base occurrences after correction
                    Map<Character, Double> cntBasesCor = countBaseFrequencies(mappingReadsCor);

                    // Get base and count of most occurring base
                    Character max_base = getMostOccurringBase(cntBasesCor);
                    Double max_count = cntBasesCor.get(max_base);

                    // Determine frequency of most occurring base
                    double weightSum = getCountSum(cntBasesCor);
                    MAX_FREQ = max_count / weightSum;

                    // Add variant object from corrected calls
                    variant_calls.add(VariantCalling.makeVariantCall(cntBasesCor, ref, referencePosition, sampleName));

                    if (MAX_FREQ < min_freq) {
                        //TODO: Add log file entry
                        base_call = 'N';
                    } else {
                        // TODO: Add log file entry
                        base_call = max_base;
                    }
                }

                // Add final base call to sequence
                consensus_sequence.append(base_call);
            }
        }

        return new ReturnTuple(consensus_sequence, variant_calls);
    }


    /**
     * Counts the occurrence of each base in the mapping reads.
     *
     * @param mapping_reads Mapping reads
     * @return Base frequencies
     */
    public static Map<Character, Double> countBaseFrequencies(ArrayList<MappingPosition> mapping_reads) {
        Map<Character, Double> base_freq = new java.util.HashMap<>(Map.of('C', 0.0,
                                                                            'T', 0.0,
                                                                            'G', 0.0,
                                                                            'A', 0.0));
        for (MappingPosition mp : mapping_reads) {
            if (mp.base == 'C') {
                base_freq.put('C', base_freq.get('C') + mp.weight);
            } else if (mp.base == 'T') {
                base_freq.put('T', base_freq.get('T') + mp.weight);
            } else if (mp.base == 'G') {
                base_freq.put('G', base_freq.get('G') + mp.weight);
            } else if (mp.base == 'A') {
                base_freq.put('A', base_freq.get('A') + mp.weight);
            }
        }
        return base_freq;
    }


    /**
     * Determines the variant type based on the bases present in the reads.
     *
     * @param mapping_reads Mapping reads
     * @return Variant type
     */
    public static VariantType getVariantType(ArrayList<MappingPosition> mapping_reads) {
        boolean C = false;
        boolean T_forward = false;
        boolean G = false;
        boolean A_reverse = false;

        if (cor_mode.equals(CorrectionMode.NO_COR)) {
            return VariantType.NONE;
        }

        // Check which bases are present in the reads
        for (MappingPosition mp : mapping_reads) {
            if (!mp.is_reverse && mp.base == 'T'){
                T_forward = true;
            } else if (mp.is_reverse && mp.base == 'A')  {
                A_reverse = true;
            } else if (mp.base == 'C') {
                C = true;
            } else if (mp.base == 'G') {
                G = true;
            }
        }

        // Determine variant type
        if (C && T_forward) {
            return VariantType.CT;
        } else if (G && A_reverse) {
            return VariantType.GA;
        } else {
            return VariantType.NONE;
        }
    }


    /**
     * Sums the values in a Hashmap
     * @param base_count_map Map of base counts
     * @return Sum of all base weights
     */
    private static double getCountSum(Map<Character, Double> base_count_map){
        double cnt = 0.0;
        for (double val: base_count_map.values()){
                cnt += val;
        }
        return cnt;
    }


    /**
     * Find the base with the highest count in a map (excl. N).
     * @param base_count_map Map of Bases and corresponding counts
     * @return Most occurring base in Map
     */
    public static Character getMostOccurringBase(Map<Character, Double> base_count_map){
        // Create reverse representation of base_count-map – excl. N
        // Count as key; Base as value
        Multimap<Double, Character> rev_map = HashMultimap.create();
        for (var entry : base_count_map.entrySet()){
            if (entry.getKey() != 'N') {
                rev_map.put(entry.getValue(), entry.getKey());
            }
        }

        // Get max count and all bases with max count
        Double max_count = Collections.max(rev_map.entries(), Map.Entry.comparingByKey()).getKey();
        Collection<Character> max_base = rev_map.get(max_count);

        // Return most occurring base
        return max_base.iterator().next();
    }

}
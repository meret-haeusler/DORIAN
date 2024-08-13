package dorian;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import datastructure.CorrectionMode;
import datastructure.MappingPosition;
import datastructure.VariantType;
import htsjdk.samtools.SAMRecord;

import java.util.*;

import static dorian.DamageCorrection.correctDamage;
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
     * @param mapping_reads   List of mapping reads
     * @param min_cov_1   Lower coverage threshold
     * @param min_freq  Lower base frequency threshold
     * @return  Base call for specified ref position according to filter criteria
     */
    public static Character makeBaseCall(ArrayList<MappingPosition> mapping_reads, int min_cov_1, int min_cov_2, double min_freq) {
        // Check if coverage parameter is fulfilled
        ArrayList<MappingPosition> corrected_reads;
        if (mapping_reads.size() < min_cov_1) {
            return 'N';
        } else {
            // Check if C & T from forward reads (or G & A from reverse reads) are present
            // and correct damage if necessary
            final VariantType variant_type = getVariantType(mapping_reads);
            corrected_reads = switch (variant_type) {
                case CT, GA -> correctDamage(mapping_reads, variant_type);
                case NONE -> VariantCalling.cloneList(mapping_reads);
            };
        }

        // Count base frequencies
        Map<Character, Double> base_freq = countBaseFrequencies(corrected_reads);

        // Check if #non-N bases > min_cov_2
        double nonN_count = countNonNBases(base_freq);
        if (nonN_count < min_cov_2){
            return 'N';
        }

        // Get base and count of most occurring base
        Character max_base = getMostOccurringBase(base_freq);
        Double max_count = base_freq.get(max_base);

        // Check if base frequencies are above threshold
        MAX_FREQ = max_count / nonN_count;
        if (MAX_FREQ < min_freq) {
            return 'N';
        } else {
            return max_base;
        }
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
                                                                            'A', 0.0,
                                                                            'N', 0.0));
        for (MappingPosition mp : mapping_reads) {
            if (mp.base == 'C') {
                base_freq.put('C', base_freq.get('C') + mp.weight);
            } else if (mp.base == 'T') {
                base_freq.put('T', base_freq.get('T') + mp.weight);
            } else if (mp.base == 'G') {
                base_freq.put('G', base_freq.get('G') + mp.weight);
            } else if (mp.base == 'A') {
                base_freq.put('A', base_freq.get('A') + mp.weight);
            } else if (mp.base == 'N') {
                base_freq.put('N', base_freq.get('N') + mp.weight);
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
     * Extracts all reads mapping to a specified reference position from a set of reads
     *
     * @param reads Set of reads
     * @param ref_pos   1-based reference position
     * @return  Set of reads mapping to the specified position
     */
    public static ArrayList<MappingPosition> getMappingReads(List<SAMRecord> reads, int ref_pos) {
        // Extract mapping reads
        ArrayList<MappingPosition> mapping_reads = new ArrayList<>();
        for (SAMRecord read : reads) {
            // Check if read maps to reference position
            int read_idx = read.getReadPositionAtReferencePosition(ref_pos) - 1;
            if (read_idx != -1) {
                mapping_reads.add(new MappingPosition(read.getReadString().charAt(read_idx), read_idx,
                        read.getReadLength(), read.getReadNegativeStrandFlag(), 1.0));
            }
        }
        return mapping_reads;
    }


    /**
     * Counts the number of non-N bases in a Map
     * @param base_count_map Map of base counts
     * @return Number of non-N bases
     */
    private static double countNonNBases(Map<Character, Double> base_count_map){
        double cnt = 0.0;
        for (Character key: base_count_map.keySet()){
            if (key != 'N'){
                cnt += base_count_map.get(key);
            }
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
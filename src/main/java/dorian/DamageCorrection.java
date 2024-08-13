package dorian;

import datastructure.MappingPosition;
import datastructure.VariantType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Corrects damage in ancient DNA reads utilising the damage profiles of the 5' and 3' ends.
 *
 * @author Meret Häusler
 * @version 1.0
 * @since 2024-02-15
 */
public class DamageCorrection {

    public static ArrayList<MappingPosition> correctDamage(ArrayList<MappingPosition> mapping_reads, VariantType variant_type) {
        ArrayList<MappingPosition> corrected_reads = VariantCalling.cloneList(mapping_reads);

        // Initialise counter for REF upvote
        Character compl_nuc = (variant_type.equals(VariantType.CT)) ? 'C' : 'G';
        MappingPosition upvote_counter = new MappingPosition(compl_nuc, 0.0);


        // Correct base at mapping position
        for (MappingPosition cur_mp : corrected_reads) {
            switch (dorian.cor_mode) {
                // Correction Mode: Silence Damage
                // Put 'N' instead of 'T' on forward reads (or 'A' on reverse reads)
                case DAM_SIL:
                    if (variant_type.equals(VariantType.CT) && cur_mp.base == 'T' && !cur_mp.is_reverse) {
                        cur_mp.setBase('N');
                    } else if (variant_type.equals(VariantType.GA) && cur_mp.base == 'A' && cur_mp.is_reverse) {
                        cur_mp.setBase('N');
                    }
                    break;

                // Correction Mode: Weighted Consensus Calling
                // Down-weight 'T' on forward reads (or 'A' on reverse reads) by damage observed at the read position
                case WCC:
                    if (variant_type.equals(VariantType.CT) && cur_mp.base == 'T' && !cur_mp.is_reverse) {
                        List<Double> dp = mapDamageToRead(cur_mp.read_length, dorian.dp5, dorian.dp3);
                        double dam = dp.get(cur_mp.read_idx);
                        double cor_weight = 1-dam;
                        cur_mp.setWeight(cor_weight);

                    } else if (variant_type.equals(VariantType.GA) && cur_mp.base == 'A' && cur_mp.is_reverse) {
                        // Reverse damage profiles to match reverse reads
                        List<Double> rev_dp5 = new ArrayList<>(List.copyOf(dorian.dp5));
                        Collections.reverse(rev_dp5);
                        List<Double> rev_dp3 = new ArrayList<>(List.copyOf(dorian.dp3));
                        Collections.reverse(rev_dp3);

                        List<Double> dp = mapDamageToRead(cur_mp.read_length, rev_dp3, rev_dp5);
                        double dam = dp.get(cur_mp.read_idx);
                        double cor_weight = 1-dam;
                        cur_mp.setWeight(cor_weight);
                    }
                    break;

                // Correction Mode: Weighted Consensus Calling with Upvote
                // Down-weight 'T' on forward reads (or 'A' on reverse reads) by damage observed at the read position
                // Up-weight REF allele by the same amount
                case WCC_UPVOTE:
                    if (variant_type.equals(VariantType.CT) && cur_mp.base == 'T' && !cur_mp.is_reverse) {
                        List<Double> dp = mapDamageToRead(cur_mp.read_length, dorian.dp5, dorian.dp3);
                        double dam = dp.get(cur_mp.read_idx);
                        double cor_weight = 1-dam;
                        cur_mp.setWeight(cor_weight);
                        upvote_counter.addWeight(dam);

                    } else if (variant_type.equals(VariantType.GA) && cur_mp.base == 'A' && cur_mp.is_reverse) {
                        // Reverse damage profiles to match reverse reads
                        List<Double> rev_dp5 = new ArrayList<>(List.copyOf(dorian.dp5));
                        Collections.reverse(rev_dp5);
                        List<Double> rev_dp3 = new ArrayList<>(List.copyOf(dorian.dp3));
                        Collections.reverse(rev_dp3);

                        List<Double> dp = mapDamageToRead(cur_mp.read_length, rev_dp3, rev_dp5);
                        double dam = dp.get(cur_mp.read_idx);
                        double cor_weight = 1-dam;
                        cur_mp.setWeight(cor_weight);
                        upvote_counter.addWeight(dam);
                    }
                    break;
            }
        }

        // Add upvote counter to output (won't affect the result if cor_mode is not WCC_UPVOTE)
        corrected_reads.add(upvote_counter);

        return corrected_reads;
    }

    /**
     * Maps the given damage profiles to the read characters
     *
     * @param read_length Read length
     * @param dp5  Damage profile of 5' end
     * @param dp3  Damage profile of 3' end
     * @return Tuple with read string and mapped damage profile as list
     */
    private static List<Double> mapDamageToRead(int read_length, List<Double> dp5, List<Double> dp3) {
        // Initialise damage list
        List<Double> dam_list = new ArrayList<>(List.copyOf(dp5));

        // Check if damage profiles overlap in read
        int overlap = read_length - (dp5.size() + dp3.size());

        // If damage profiles overlap --> Merge damage profiles to read length
        if (overlap <= 0) {
            dam_list.addAll(dp3.subList(Math.abs(overlap), dp3.size()));

        } else {
            // Else --> Fill uncovered nucleotides with damage = 0
            dam_list.addAll(Collections.nCopies(Math.abs(overlap), 0.0));
            dam_list.addAll(dp3);
        }

        return dam_list;
    }

}
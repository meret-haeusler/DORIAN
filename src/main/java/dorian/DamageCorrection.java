package dorian;

import datastructure.DamageType;
import datastructure.MappingPosition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static utils.ListCloner.cloneList;

/**
 * Corrects damage in ancient DNA reads utilising the damage profiles of the 5' and 3' ends.
 *
 * @author Meret Häusler
 * @version 1.0
 * @since 2024-02-15
 */
public class DamageCorrection {

    /**
     * Silences all forward mapping Ts (reverse mapping As) in a list of mapping reads
     * @param mappingReads  List of mapping reads
     * @param damType       Specification of damage type (CT or AG)
     * @return  List of silenced reads
     */
    public static ArrayList<MappingPosition> silenceDamage(ArrayList<MappingPosition> mappingReads, DamageType damType) {
        // Initialise output list
        ArrayList<MappingPosition> silencedReads = cloneList(mappingReads);

        // Iterate over all mapping positions
        for (MappingPosition mp : silencedReads) {
            //Silence position if..
            if (damType.equals(DamageType.CT) && mp.base == 'T' && !mp.is_reverse) {
                // ..base is forward mapping T
                mp.setBase('N');
            } else if (damType.equals(DamageType.GA) && mp.base == 'A' && mp.is_reverse) {
                // ..base is reverse mapping A
                mp.setBase('N');
            }
        }

        return silencedReads;
    }


    /**
     * Performs a damage-ware weighting of a read list. DamType specifies whether forward mapping Ts are down-weight
     * and Cs are up-weight, or reverse mapping As are down-weight and Gs are up-weight.
     * @param mappingReads  List of mapping reads
     * @param damType       Specification of damage type (CT or AG)
     * @return  List of reads with down-weighted forward mapping Ts (reverse mapping As) and up-voted Cs (Gs)
     */
    public static ArrayList<MappingPosition> weightDamage(ArrayList<MappingPosition> mappingReads, DamageType damType) {
        // Initialise output list
        ArrayList<MappingPosition> weightedReads = cloneList(mappingReads);

        // Initialise counter for REF upvote
        Character compl_nuc = (damType.equals(DamageType.CT)) ? 'C' : 'G';
        MappingPosition upvote_counter = new MappingPosition(compl_nuc, 0.0);

        // Iterate over mapping positions
        for (MappingPosition mp : weightedReads) {
            if (damType.equals(DamageType.CT) && mp.base == 'T' && !mp.is_reverse) {
                List<Double> dp = mapDamageToRead(mp.read_length, dorian.dp5, dorian.dp3);
                double dam = dp.get(mp.read_idx);
                double cor_weight = 1 - dam;
                mp.setWeight(cor_weight);
                upvote_counter.addWeight(dam);

            } else if (damType.equals(DamageType.GA) && mp.base == 'A' && mp.is_reverse) {
                // Reverse damage profiles to match reverse reads
                List<Double> rev_dp5 = new ArrayList<>(List.copyOf(dorian.dp5));
                Collections.reverse(rev_dp5);
                List<Double> rev_dp3 = new ArrayList<>(List.copyOf(dorian.dp3));
                Collections.reverse(rev_dp3);

                List<Double> dp = mapDamageToRead(mp.read_length, rev_dp3, rev_dp5);
                double dam = dp.get(mp.read_idx);
                double cor_weight = 1 - dam;
                mp.setWeight(cor_weight);
                upvote_counter.addWeight(dam);
            }
        }

        //Add upvote counter to output
        weightedReads.add(upvote_counter);

        return weightedReads;
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
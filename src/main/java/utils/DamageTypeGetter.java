package utils;

import datastructure.DamageType;
import datastructure.MappingPosition;

import java.util.List;

public class DamageTypeGetter {

    /**
     * Determine if a position needs correction and which type of damage is present
     * @param mappingReads  List of MappingPositions
     * @param refBase       Base of reference sequence at given position
     * @return  DamageType for the given data
     */
    public static DamageType getDamageTypeRefbased(List<MappingPosition> mappingReads, char refBase) {
        boolean T_forward = false;
        boolean A_reverse = false;

        // Check which bases are present in the reads
        for (MappingPosition mp : mappingReads) {
            if (!mp.is_reverse && mp.base == 'T') {
                T_forward = true;
            } else if (mp.is_reverse && mp.base == 'A') {
                A_reverse = true;
            }
        }

        // Determine damage type
        if (refBase == 'C' && T_forward) {
            return DamageType.CT;
        } else if (refBase == 'G' && A_reverse) {
            return DamageType.GA;
        } else {
            return DamageType.NONE;
        }
    }


    /**
     * Determines if a position is damaged and which damage pattern is present
     * @param mappingReads Mapping reads
     * @return DamageType
     */
    public static DamageType getDamageTypeReffree(List<MappingPosition> mappingReads){
        boolean C = false;
        boolean T_forward = false;
        boolean G = false;
        boolean A_reverse = false;

        // Check which bases are present in the reads
        for (MappingPosition mp : mappingReads) {
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

        // Determine damage type
        if (C && T_forward) {
            return DamageType.CT;
        } else if (G && A_reverse) {
            return DamageType.GA;
        } else {
            return DamageType.NONE;
        }
    }
}

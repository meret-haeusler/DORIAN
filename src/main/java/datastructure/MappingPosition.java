package datastructure;

import htsjdk.samtools.SAMRecord;

/**
 * Represents a mapping position in a read.
 *
 * @author Meret Häusler
 * @version 1.0
 * @since 2024-02-15
 */

public class MappingPosition {
    public Character base;
    public int read_idx;
    public int read_length;
    public boolean is_reverse;
    public double weight;
    public String read_group;

    // Constructor
    public MappingPosition(Character base, double weight) {
        this.base = base;
        this.weight = weight;
    }

    /**
     * Creates a MappingPosition object
     *
     * @param base        Character at read_idx in read
     * @param read_idx    Index of base in read (0-based)
     * @param read_length Length of read
     * @param is_reverse  False if read is forward mapping; True if read is reverse mapping
     * @param weight      Weight of base contributing to base call
     */
    public MappingPosition(Character base, int read_idx, int read_length, boolean is_reverse, double weight, String read_group) {
        this.base = base;
        this.read_idx = read_idx;
        this.read_length = read_length;
        this.is_reverse = is_reverse;
        this.weight = weight;
        this.read_group = read_group;
    }


    // Setters
    public void setBase(Character base) {
        this.base = base;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void addWeight(double additionalWeight) {
        this.weight += additionalWeight;
    }


    /**
     * Extracts information of SAMrecord and creates MappingPosition
     *
     * @param record  A read record
     * @param ref_pos Reference position (1-based)
     * @return MappingPosition object: base, read_idx (0-based), read_length, is_reverse, weight
     */
    public static MappingPosition createMappingPosition(SAMRecord record, int ref_pos) {
        int read_idx = record.getReadPositionAtReferencePosition(ref_pos) - 1;
        // Check if position is a deletion
        if (read_idx == -1) {
            return null;
        } else {
            return new MappingPosition(record.getReadString().charAt(read_idx),
                    read_idx,
                    record.getReadLength(),
                    record.getReadNegativeStrandFlag(),
                    1.0,
                    record.getReadGroup().getReadGroupId());
        }
    }
}

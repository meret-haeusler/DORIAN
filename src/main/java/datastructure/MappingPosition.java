package datastructure;

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

    // Constructor
    public MappingPosition(Character base, double weight) {
        this.base = base;
        this.weight = weight;
    }

    public MappingPosition(Character base, int read_idx, int read_length, boolean is_reverse, double weight) {
        this.base = base;
        this.read_idx = read_idx;
        this.read_length = read_length;
        this.is_reverse = is_reverse;
        this.weight = weight;
    }


    // Setters
    public void setBase(Character base) {
        this.base = base;
    }

    public void setReadIdx(int read_idx) {
        this.read_idx = read_idx;
    }

    public void setReadLength(int read_length) {
        this.read_length = read_length;
    }

    public void setIsReverse(boolean is_reverse) {
        this.is_reverse = is_reverse;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public void addWeight(double additionalWeight) {
        this.weight += additionalWeight;
    }

    // Getters
    public Character getBase() {
        return this.base;
    }

    public int getReadIdx() {
        return this.read_idx;
    }

    public int getReadLength() {
        return this.read_length;
    }

    public boolean getIsReverse() {
        return this.is_reverse;
    }

    public double getWeight() {
        return this.weight;
    }


}

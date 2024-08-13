package datastructure;

/**
 * Represents a sequence in FASTA format.
 *
 * @author Meret Häusler
 * @version 1.0
 * @since 2024-02-16
 */

public class Fasta {
    String header;
    String sequence;

    // Constructor
    public Fasta(String header, String sequence) {
        this.header = header;
        this.sequence = sequence;
    }

    // Getters
    public String getHeader() {
        return header;
    }

    public String getSequence() {
        return sequence;
    }

    // Setters
    public void setHeader(String header) {
        this.header = header;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
}

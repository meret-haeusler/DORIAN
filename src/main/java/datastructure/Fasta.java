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

    /**
     * Strips the reference name of the FASTA header from leading ">" and removes everything after the first space.
     *
     * @return The stripped reference name.
     */
    public String getHeaderID() {
        // Remove leading ">" and everything after the first space
        return header.replaceFirst("^>", "").split(" ")[0];
    }
}

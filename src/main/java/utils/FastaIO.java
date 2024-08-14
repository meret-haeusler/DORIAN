package utils;

import datastructure.Fasta;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Meret Häusler
 * @version 1.0
 * @since 2023-04-28
 */

public class FastaIO {
    /**
     * Method to read in a fasta file. It generates a new fasta objects for each entry.
     * This function is also needed for Exercise 3, so be sure to start on time with it.
     */
    public static List<Fasta> readFasta(String filepath) throws Exception {
        // Define output list
        List<Fasta> fastaEntries = new ArrayList<>();

        // Define buffered reader to read file line by line
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        String curLine;

        // Define tmp variables for header and sequence entries
        String headerBuffer = "";
        StringBuilder seqBuffer = new StringBuilder();


        while((curLine = br.readLine()) != null){
            // Check if line is header
            if(curLine.startsWith(">")) {
                // If headerBuffer has entry add header and seq to list of entries
                if (!headerBuffer.isEmpty()) {
                    fastaEntries.add(new Fasta(headerBuffer, seqBuffer.toString()));
                }
                // Add new header to headerBuffer and clear seqBuffer
                headerBuffer = curLine;
                seqBuffer = new StringBuilder();
            }
            // If line starts not with ">" add line to seq
            else {
                seqBuffer.append(curLine.strip());
            }
        }
        // Add last fasta object to list manually
        if (!headerBuffer.isEmpty()) {
            fastaEntries.add(new Fasta(headerBuffer, seqBuffer.toString()));
        }
        // Return list with fasta entries
        return fastaEntries;
    }


    /**
     * Writes a Fasta object to a given file path. Must adhere to fasta format conventions
     * @param fasta     Object containing header and sequence
     * @param filepath  Name for output file
     */
    public static void writeFasta(Fasta fasta, String filepath) throws Exception{
        // Define buffer and output file
        BufferedWriter bw = new BufferedWriter(new FileWriter(filepath, false));

        // Write header sequence
        bw.write(fasta.getHeader() + "\n");

        // Write sequence in fasta format (new line every 70 characters)
        bw.write(fasta.getSequence().replaceAll(".{70}", "$0\n") + "\n");

        bw.close();

    }

}

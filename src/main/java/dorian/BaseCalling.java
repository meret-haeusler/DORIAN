package dorian;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import datastructure.*;
import htsjdk.samtools.*;
import htsjdk.samtools.util.CloseableIterator;
import htsjdk.variant.variantcontext.VariantContext;
import utils.DamageTypeGetter;
import utils.ListCloner;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static dorian.dorian.cor_mode;
import static dorian.dorian.dam_det;
import static utils.LogWriter.addLog;

/**
 * Determines a base call based on coverage and frequency of the bases at a given position.
 *
 * @author Meret Häusler
 * @version 1.0
 * @since 2024-02-15
 */
public class BaseCalling {


    /**
     * Builds a consensus_sequence and makes variant calls of a set of reads
     *
     * @param reads      Bam file of reads
     * @param minCov     Minimal coverage for consensus calling
     * @param minFreq    Minimal frequency for consensus calling
     * @param ref        Fasta record for reference file
     * @param sampleName Name of sample
     * @return StringBuilder with consensus sequence and List of VariantContext for variant calls
     */
    public static Tuple<StringBuilder, List<VariantContext>> consensusCalling(File reads, int minCov, double minFreq,
                                                                              Fasta ref, String sampleName, Boolean vcf) throws IOException {
        // Initialise output
        StringBuilder consensusSequence = new StringBuilder();
        List<VariantContext> variantCalls = new ArrayList<>();

        // TODO:
        //  - [x] Modify this for-loop to a do/while loop with condition if locusIterator.hasNext()
        //  - [x] Initialize reference pointer outside loop
        //  - [x] Create method to add whole read to MappingPositionTree
        //  - [x] Create method to reconstruct position given reference pointer and MappingPositionTree
        //       (Basically everything after "// BASE CALLING //" to "// Add final base call to sequence")

        // Initialise helper variables
        int referencePointer = 0;
        MappingPositionTree mappingPositionTree = new MappingPositionTree();

        // Iterate through each record in the BAM file
        try (SamReader reader = SamReaderFactory.makeDefault().open(reads)) {
            try (CloseableIterator<SAMRecord> iterator = reader.iterator()) {
                do {
                    // Extract read
                    SAMRecord record = iterator.next();

                    // Add read information to mappingPositionTree
                    mappingPositionTree.addReadRecord(record);

                    // If read-start is larger than referencePointer, reconstruct all positions smaller than read-start
                    while (record.getReferenceIndex() > referencePointer) {

                        // TODO: Write reconstruction method reconstructPosition()
                        Tuple<Character, VariantContext> reconstructedPosition = reconstructPosition(
                                mappingPositionTree.getMappingPositionList(referencePointer + "." + 0),
                                minCov, minFreq, ref, sampleName, referencePointer + 1);
                        // Add reconstructed sequence to consensus sequence by g
                        consensusSequence.append(reconstructedPosition.getFirst());
                        // Add reconstructed variant calls to variant calls
                        if (vcf) {
                            variantCalls.add(reconstructedPosition.getSecond());
                        }

                        // Remove reconstructed positions from mappingPositionTree
                        mappingPositionTree.removeKey(referencePointer + "." + 0);
                        referencePointer++;
                    }

                } while (iterator.hasNext()); // Stop if no more records are available
                // TODO: Add final base call to sequence
                //  - while referencePointer < ref.getSequence().length()
            }
        }

        return new Tuple<>(consensusSequence, variantCalls);
    }


    private static Tuple<Character, VariantContext> reconstructPosition(ArrayList<MappingPosition> mappingReads, int minCov, double minFreq, Fasta ref,
                                                                        String sampleName, int referencePosition) {
        // BASE CALLING //
        Character baseCall;
        VariantContext variantCall;
        Map<Character, Double> cntBases = countBaseFrequencies(mappingReads);

        // Check if coverage parameter is fulfilled
        if (mappingReads.size() < minCov) {
            // Make non-informative base call
            baseCall = 'N';
            // Add variant object
            variantCall = VariantCalling.makeVariantCall(cntBases, ref, referencePosition, sampleName);
            // Create log entry if correction mode is 'no correction'
            if (cor_mode.equals(CorrectionMode.NO_COR)) {
                addLog(ref, referencePosition, mappingReads.size(), cntBases, cntBases, baseCall, -1.0);
            }
        } else {
            // Determine if correction is necessary
            DamageType damPos = switch (dam_det) {
                case NO_COR -> DamageType.NONE;
                case BASED ->
                        DamageTypeGetter.getDamageTypeRefbased(mappingReads, ref.getSequence().charAt(referencePosition - 1));
                case FREE -> DamageTypeGetter.getDamageTypeReffree(mappingReads);
            };

            // Create new instance for corrected reads
            ArrayList<MappingPosition> mappingReadsCor;
            if (!damPos.needsCorrection()) {
                //If no correction is necessary, copy initial read set
                mappingReadsCor = ListCloner.cloneList(mappingReads);
            } else {
                mappingReadsCor = switch (cor_mode) {
                    case NO_COR -> ListCloner.cloneList(mappingReads);
                    //If correction mode is Silencing, silence forward mapping Ts (reverse mapping As)
                    case SILENCING -> DamageCorrection.silenceDamage(mappingReads, damPos);
                    //If correction mode is Weighting, down-weight forward mapping Ts (reverse mapping As) / up-weight Cs (Gs)
                    case WEIGHTING -> DamageCorrection.weightDamage(mappingReads, damPos);
                };
            }

            // Count base occurrences after correction
            Map<Character, Double> cntBasesCor = countBaseFrequencies(mappingReadsCor);

            // Get base and count of most occurring base
            Character maxBase = getMostOccurringBase(cntBasesCor);
            Double maxCount = cntBasesCor.get(maxBase);

            // Determine frequency of most occurring base
            double weightSum = sumHashmapValues(cntBasesCor);
            double maxFreq = maxCount / weightSum;

            // Add variant object from corrected calls
            variantCall = VariantCalling.makeVariantCall(cntBasesCor, ref, referencePosition, sampleName);

            // Check if minimal frequency parameter is fulfilled, if not put call to 'N'
            if (maxFreq < minFreq || weightSum < minCov) {
                maxBase = 'N';
                maxFreq = -1.0;
            }

            // If position was corrected, add info to log file
            if (damPos.needsCorrection() || cor_mode.equals(CorrectionMode.NO_COR)) {
                addLog(ref, referencePosition, mappingReads.size(), cntBases, cntBasesCor, maxBase, maxFreq);
            }

            // Add base call
            baseCall = maxBase;
        }

        return new Tuple<>(baseCall, variantCall);

    }


    /**
     * Counts the occurrence of each base in the mapping reads.
     *
     * @param mapping_reads Mapping reads
     * @return Base frequencies
     */
    private static Map<Character, Double> countBaseFrequencies(ArrayList<MappingPosition> mapping_reads) {
        Map<Character, Double> base_freq = new HashMap<>(Map.of('C', 0.0,
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
     * Sums the values in a Hashmap
     *
     * @param base_count_map Map of base counts
     * @return Sum of all base weights
     */
    private static double sumHashmapValues(Map<Character, Double> base_count_map) {
        double cnt = 0.0;
        for (double val : base_count_map.values()) {
            cnt += val;
        }
        return cnt;
    }


    /**
     * Find the base with the highest count in a map (excl. N).
     *
     * @param base_count_map Map of Bases and corresponding counts
     * @return Most occurring base in Map
     */
    private static Character getMostOccurringBase(Map<Character, Double> base_count_map) {
        // Create reverse representation of base_count-map – excl. N
        // Count as key; Base as value
        Multimap<Double, Character> rev_map = HashMultimap.create();
        for (var entry : base_count_map.entrySet()) {
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
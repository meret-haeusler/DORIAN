package dorian;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import datastructure.*;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.SamLocusIterator;
import htsjdk.variant.variantcontext.VariantContext;
import utils.DamageTypeGetter;
import utils.ListCloner;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static dorian.dorian.cor_mode;
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
     * @throws IOException
     */
    public static ReturnTuple consensusCalling(File reads, int minCov, double minFreq,
                                               Fasta ref, String sampleName) throws IOException {
        // Initialise output
        StringBuilder consensusSequence = new StringBuilder();
        List<VariantContext> variantCalls = new ArrayList<>();

        // Iterate over bam file
        try (SamReader reader = SamReaderFactory.makeDefault().open(reads)) {
            // Initialize SamLocusIterator
            SamLocusIterator locusIterator = new SamLocusIterator(reader);
            // Iterate over each position
            for (SamLocusIterator.LocusInfo locusInfo : locusIterator) {
                int referencePosition = locusInfo.getPosition();
                ArrayList<MappingPosition> mappingReads = new ArrayList<>();

                // GET MAPPING READS //
                for (SamLocusIterator.RecordAndOffset recordAndOffset : locusInfo.getRecordAndOffsets()) {
                    SAMRecord record = recordAndOffset.getRecord();
                    mappingReads.add(MappingPosition.createMappingPosition(record, referencePosition));
                }

                // BASE CALLING //
                Character baseCall;
                Map<Character, Double> cntBases = countBaseFrequencies(mappingReads);

                // Check if coverage parameter is fulfilled
                if (mappingReads.size() < minCov) {
                    // Add variant object and make non-informative base call
                    variantCalls.add(VariantCalling.makeVariantCall(cntBases, ref, referencePosition, sampleName));
                    baseCall = 'N';

                    if (cor_mode.equals(CorrectionMode.NO_COR)) {
                        addLog(locusInfo, ref, mappingReads.size(), cntBases, cntBases, baseCall, -1.0);
                    }
                } else {
                    // Determine if correction is necessary
                    DamageType damPos = switch (cor_mode) {
                        case NO_COR -> DamageType.NONE;
                        case REFBASED_SIL ->
                                DamageTypeGetter.getDamageTypeRefbased(mappingReads, ref.getSequence().charAt(referencePosition - 1));
                        case REFFREE_SIL, REFFREE_WEI -> DamageTypeGetter.getDamageTypeReffree(mappingReads);
                    };

                    // Create new instance for corrected reads
                    ArrayList<MappingPosition> mappingReadsCor;
                    if (!damPos.needsCorrection()) {
                        //If no correction is necessary, copy inital read set
                        mappingReadsCor = ListCloner.cloneList(mappingReads);
                    } else {
                        if (!cor_mode.needsDP()) {
                            //If correction mode is Refbased or Reffree Silencing, silence forward mapping Ts (reverse mapping As)
                            mappingReadsCor = DamageCorrection.silenceDamage(mappingReads, damPos);
                        } else {
                            //If correction mode is Reffree Weighting, down-weight forward mapping Ts (reverse mapping As) / up-weight Cs (Gs)
                            mappingReadsCor = DamageCorrection.weightDamage(mappingReads, damPos);
                        }
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
                    variantCalls.add(VariantCalling.makeVariantCall(cntBasesCor, ref, referencePosition, sampleName));

                    // Check if minimal frequency parameter is fulfilled, if not put call to 'N'
                    if (maxFreq < minFreq) {
                        maxBase = 'N';
                        maxFreq = -1.0;
                    }

                    // If position was corrected, add info to log file
                    if (damPos.needsCorrection() || cor_mode.equals(CorrectionMode.NO_COR)) {
                        addLog(locusInfo, ref, mappingReads.size(), cntBases, cntBasesCor, maxBase, maxFreq);
                    }

                    // Add base call
                    baseCall = maxBase;
                }

                // Add final base call to sequence
                consensusSequence.append(baseCall);
            }
        }

        return new ReturnTuple(consensusSequence, variantCalls);
    }


    /**
     * Counts the occurrence of each base in the mapping reads.
     *
     * @param mapping_reads Mapping reads
     * @return Base frequencies
     */
    public static Map<Character, Double> countBaseFrequencies(ArrayList<MappingPosition> mapping_reads) {
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
    public static Character getMostOccurringBase(Map<Character, Double> base_count_map) {
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
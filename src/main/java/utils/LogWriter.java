package utils;

import datastructure.CorrectionMode;
import datastructure.Fasta;
import htsjdk.samtools.util.SamLocusIterator;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.stream.Collectors;

import static dorian.dorian.*;

public class LogWriter {

    private static final DecimalFormat df = new DecimalFormat("####0.00");

    /**
     * Adds a line to the log file documenting the determination of a base call
     * @param refInfo   Information on the current reference position
     * @param ref       Reference as fasta object
     * @param cov       Observed read coverage at the position
     * @param cnts      Base counts before correction
     * @param cntsCor   Base counts after correction
     * @param call      Final base call
     * @param callFreq  Frequency of final base call (-1 if call is 'N')
     */
    public static void addLog(SamLocusIterator.LocusInfo refInfo, Fasta ref, int cov, Map<Character, Double> cnts,
                              Map<Character, Double> cntsCor, Character call, Double callFreq) {

        // Get reference infos
        String chrom = refInfo.getSequenceName();
        int refPos = refInfo.getPosition();
        char refBase = ref.getSequence().charAt(refPos-1);

        // Add to log file
        // Uncorrected: CHROM POS REF COV ALLELE_COUNTS BASE_CALL BASE_FREQ
        // Corrected:   CHROM POS REF COV ALLELE_COUNTS_PRIOR ALLELE_COUNTS_CORRECTED BASE_CALL BASE_FREQ
        if (cor_mode.equals(CorrectionMode.NO_COR)) {
            file_logger.info("{}\t{}\t{}\t{}\t{}\t{}\t{}",
                    chrom, refPos, refBase, cov, MapToString(cnts), call, df.format(callFreq));
        } else {
            // Log file
            file_logger.info("{}\t{}\t{}\t{}\t{}\t{}\t{}\t{}",
                    chrom, refPos, refBase, cov, MapToString(cnts), MapToString(cntsCor), call, df.format(callFreq));

            // ROI file
            int roi_start = Math.max(refPos - 3, 0);
            int roi_end = Math.min(refPos + 2, ref.getSequence().length());
            roi_tab.info("{}\t{}\t{}\tCORRECTED_POS:{}", chrom, roi_start, roi_end, refPos);
        }
    }


    /**
     * Converts HashMap entries to a formatted string
     *
     * @param map Map of allele counts
     * @return String of allele counts
     */
    private static String MapToString(Map<Character, Double> map) {
        DecimalFormat df = new DecimalFormat("#.##");
        return map.keySet().stream()
                .map(key -> key + "=" + df.format(map.get(key)))
                .collect(Collectors.joining(","));
    }
}

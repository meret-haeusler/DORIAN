package utils;

import datastructure.CorrectionMode;

import java.text.DecimalFormat;

import static dorian.BaseCalling.MAX_FREQ;
import static dorian.dorian.*;
import static dorian.dorian.base_call;

public class LogWriter {


    static DecimalFormat df = new DecimalFormat("#.##");

    public static void addLog(String message) {

        // Add info to logger if reads were corrected
        if (variant_type.isVariant()) {
            // Get base call frequency
            double call_freq = (base_call.equals('N')) ? -1.0 : MAX_FREQ;

            file_logger.info(contig + "\t" + ref_pos + "\t" + ref.getSequence().charAt(ref_pos-1) + "\t"
                    + mapping_reads.size() + "\t" + MapToString(base_counts_uncor) + "\t" + MapToString(base_counts) + "\t"
                    + base_call + "\t" + df.format(call_freq));

            int roi_start = Math.max(ref_pos - 3, 0);
            int roi_end = Math.min(ref_pos + 2, ref.getSequence().length());
            roi_tab.info(contig + "\t" + roi_start + "\t" + roi_end + "\t" + "CORRECTED_POS:" + ref_pos);
        }

        //TODO: Remove when no longer needed
        if (cor_mode.equals(CorrectionMode.NO_COR)){
            file_logger.info(ref_pos + "\t" + base_call + "\t" + df.format(MAX_FREQ));
        }
    }
}

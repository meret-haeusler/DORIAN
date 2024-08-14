package dorian;

import cli.CLIParser;
import datastructure.CorrectionMode;
import datastructure.Fasta;
import datastructure.ReturnTuple;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.FastaIO;
import utils.VCFFileWriter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static dorian.BaseCalling.consensusCalling;

/**
 * @author Meret Häusler
 * @version 3.0
 * @since 2024-08-13
 */
public class dorian {
    public static Logger logger = LogManager.getLogger(dorian.class.getName());
    public static Logger file_logger = LogManager.getLogger("file." + dorian.class.getName());
    public static Logger roi_tab = LogManager.getLogger("roi." + dorian.class.getName());
    public static CorrectionMode cor_mode;
    public static List<Double> dp5;
    public static List<Double> dp3;
    public static double freq;
    public static int cov;

    public static void main(String[] args) throws Exception {

        // LOGGING //
        Date log_date = new Date();
        String time_stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(log_date);
        logger.info("Starting DORIAN\n");
        file_logger.info("DORIAN – REPORT\nRun: "
                + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(log_date) + "\n");

        // PARSING INPUT FILES //
        CLIParser cli_parser = new CLIParser(args);

        // BAM file
        File reads = cli_parser.BAM;
        // Correction mode
        cor_mode = cli_parser.COR_MODE;
        // Damage profiles
        dp5 = cor_mode.needsDP() ? cli_parser.DP5 : dp5;
        dp3 = cor_mode.needsDP() ? cli_parser.DP3 : dp3;
        // Output directory
        Path out_path = cli_parser.OUT;
        // Minimal Coverage
        cov = cli_parser.MIN_COV;
        // Minimal Frequency
        freq = cli_parser.MIN_FREQ;
        // Reference
        Fasta ref = cli_parser.REF;
        // Sample name
        String sample_name = cli_parser.SAMPLE_NAME;


        // PREPARE LOG FILES //
        if (cor_mode.equals(CorrectionMode.NO_COR)) {
            file_logger.info("\nCalls:");
            file_logger.info("CHROM\tPOS\tREF\tCOV\tALLELE_COUNTS\tBASE_CALL\tBASE_FREQ");
        }
        if (!cor_mode.equals(CorrectionMode.NO_COR)) {
            file_logger.info("\nCorrected variants:");
            file_logger.info("CHROM\tPOS\tREF\tCOV\tALLELE_COUNTS_PRIOR\tALLELE_COUNTS_CORRECTED\tBASE_CALL\tBASE_FREQ");

            roi_tab.info("#CHROM=chromosome or scaffold name");
            roi_tab.info("#ROI_START=0-based start position of ROI");
            roi_tab.info("#ROI_END=1-based end position of ROI");
            roi_tab.info("#CORRECTED_POS=1-based position of corrected variant");
            roi_tab.info("#CHROM\tROI_START\tROI_END\tCORRECTED_POS");
        }


        // ADD STATUS BAR //
        // Start the updating message in a separate thread
        Thread updatingMessage = getUpdatingMessage();


        // MAIN PROGRAMME //
        //Add BaseCalling call and resolve returns
        ReturnTuple BaseCallingOut = consensusCalling(reads, cov, freq, ref, sample_name);
        StringBuilder consensus_sequence = BaseCallingOut.getSeq();
        List<VariantContext> variant_calls = BaseCallingOut.getVariants();


        // OUTPUT //
        // Define Fasta output
        Fasta consensus_record = new Fasta(">" + sample_name + "_" + cor_mode.getShortName(), consensus_sequence.toString());
        String fasta_path = out_path.toString() + "/" + sample_name + "_" + cor_mode.getShortName() + ".fasta";
        FastaIO.writeFasta(consensus_record, fasta_path);

        // Define vcf output
        String vcf_out = out_path + "/" + sample_name + "_" + cor_mode.getShortName() + ".vcf";
        VCFHeader vcf_header = VCFFileWriter.defaultHeader(ref, sample_name + "_" + cor_mode.getShortName());
        VCFFileWriter.writeVCFFile(vcf_out, vcf_header, variant_calls);


        // OUTPUT INFO //
        // Move log file and ROI to output directory
        Files.move(Path.of("file.log"),
                Path.of(out_path + "/" + time_stamp + "_" + sample_name + "_" + cor_mode.getShortName() + ".log"));
        if (!cor_mode.equals(CorrectionMode.NO_COR)) {
            Files.move(Path.of("roi.bed"),
                    Path.of(out_path + "/" + time_stamp + "_" + sample_name + "_" + cor_mode.getShortName() + ".bed"));
        }

        // Update status bar
        updatingMessage.interrupt();
        System.out.println("\rDORIAN completed successfully.\n");

        // Output result paths
        logger.info("Result files:");
        logger.info("Corrected variants written to: " + vcf_out);
        logger.info("Reconstructed genome written to: " + fasta_path);
        logger.info("Log file written to: " + out_path + "/" + time_stamp + "_" + sample_name + "_" + cor_mode.getShortName() + ".log");

        // If exists, print ROI file path
        if (!cor_mode.equals(CorrectionMode.NO_COR)) {
            logger.info("ROI table (IGV format) for corrected variants written to: " +
                    out_path + "/" + time_stamp + "_" + sample_name + "_" + cor_mode.getShortName() + ".bed");
        }
    }

    /**
     * @return Update message while main programme runs
     */
    private static Thread getUpdatingMessage() {
        Thread updatingMessage = new Thread(() -> {
            String[] messages = {"Running", "Running.", "Running..", "Running..."};
            int i = 0;

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // Print the message and overwrite it on the same line
                    System.out.print("\r" + messages[i % messages.length]);
                    i++;
                    Thread.sleep(500);  // Update every 500 milliseconds
                } catch (InterruptedException e) {
                    // Stop the updating message if interrupted
                    break;
                }
            }
        });

        updatingMessage.start();
        return updatingMessage;
    }
}
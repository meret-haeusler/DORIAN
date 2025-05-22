package dorian;

import cli.CLIParser;
import datastructure.*;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFHeader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.DamageProfileParser;
import utils.FastaIO;
import utils.InputValidationService;
import utils.VCFFileWriter;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static dorian.BaseCalling.consensusCalling;

/**
 * @author Meret Häusler
 * @version 3.0
 * @since 2025-05-20
 */
public class dorian {
    public static Logger logger = LogManager.getLogger(dorian.class.getName());
    public static Logger file_logger;
    public static Logger roi_tab;
    public static DetectionMode dam_det;
    public static CorrectionMode cor_mode;
    public static List<Double> dp5;
    public static List<Double> dp3;
    public static double freq;
    public static int cov;

    public static void main(String[] args) {

        // LOGGING //
        Date log_date = new Date();
        String time_stamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(log_date);
        logger.info("Starting DORIAN\n");

        CommandLine cmd = CLIParser.parseArguments(args);


        try {
            // PARSING INPUT FILES //
            InputValidationService.validateInputs(cmd);

            // BAM file
            File reads = new File(cmd.getOptionValue("bam"));
            // Correction mode
            cor_mode = CorrectionMode.fromShortName(cmd.getOptionValue("correction"));
            // Damage detection
            if (!cor_mode.equals(CorrectionMode.NO_COR)) {
                dam_det = DetectionMode.fromShortName(cmd.getOptionValue("detection"));
            } else {
                dam_det = DetectionMode.NO_COR;
            }
            // Damage profiles
            if (cor_mode.equals(CorrectionMode.WEIGHTING)) {
                dp5 = DamageProfileParser.parseDamageProfile(cmd.getOptionValue("dp5"));
                dp3 = DamageProfileParser.parseDamageProfile(cmd.getOptionValue("dp3"));
            }
            // Output directory
            Path out_path = Paths.get(cmd.getOptionValue("out"));
            // Minimal Coverage
            cov = Integer.parseInt(cmd.getOptionValue("cov"));
            // Minimal Frequency
            freq = Double.parseDouble(cmd.getOptionValue("freq"));
            // Reference
            Fasta ref = FastaIO.readFasta(cmd.getOptionValue("reference")).get(0);
            // Sample name
            String sample_name = FilenameUtils.removeExtension(reads.getName());

            // LOG: FILE PARSING //
            writeCLItoLog(logger, cmd);

            // PREPARE LOG FILES //
            file_logger = LogManager.getLogger("file." + dorian.class.getName());
            file_logger.info("DORIAN – REPORT\nRun: {}\n", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(log_date));
            writeCLItoLog(file_logger, cmd);

            if (cor_mode.equals(CorrectionMode.NO_COR)) {
                file_logger.info("\nCalls:");
                file_logger.info("CHROM\tPOS\tREF\tCOV\tALLELE_COUNTS\tBASE_CALL\tBASE_FREQ");
            }
            if (!cor_mode.equals(CorrectionMode.NO_COR)) {
                file_logger.info("\nCorrected positions:");
                file_logger.info("CHROM\tPOS\tREF\tCOV\tALLELE_COUNTS_PRIOR\tALLELE_COUNTS_CORRECTED\tBASE_CALL\tBASE_FREQ");
            }
            if (cmd.hasOption("bed")) {
                roi_tab = LogManager.getLogger("roi." + dorian.class.getName());
                roi_tab.info("#CHROM=chromosome or scaffold name");
                roi_tab.info("#ROI_START=0-based start position of ROI");
                roi_tab.info("#ROI_END=1-based end position of ROI");
                roi_tab.info("#CORRECTED_POS=1-based position that were corrected");
                roi_tab.info("#CHROM\tROI_START\tROI_END\tCORRECTED_POS");
            }

            logger.info("Analysis started successfully with valid input arguments.\n");

            // ADD STATUS BAR //
            // Start the updating message in a separate thread
            Thread updatingMessage = getUpdatingMessage();


            // MAIN PROGRAMME //
            //Add BaseCalling call and resolve returns
            Tuple<StringBuilder, List<VariantContext>> BaseCallingOut = consensusCalling(reads, cov, freq, ref, sample_name, cmd.hasOption("vcf"));
            StringBuilder consensus_sequence = BaseCallingOut.getFirst();
            List<VariantContext> variant_calls = BaseCallingOut.getSecond();


            // OUTPUT //
            updatingMessage.interrupt();
            logger.info("\rDORIAN completed successfully.\n");
            logger.info("Result files written to:");

            // Put method name together
            String method = cor_mode.equals(CorrectionMode.NO_COR)
                ? cor_mode.getModeName()
                : dam_det.getDetectionMode() + "-" + cor_mode.getModeName();

            // Move log file to output directory
            Files.move(Path.of("file.log"),
                    Path.of(out_path + "/" + time_stamp + "_" + sample_name + "_" + method + ".log"));
            logger.info("– Log file:\t\t{}/{}_{}_{}.log", out_path, time_stamp, sample_name, method);

            // Define Fasta output
            Fasta consensus_record = new Fasta(">" + sample_name + "_" + method, consensus_sequence.toString());
            String fasta_path = out_path + "/" + sample_name + "_" + method + ".fasta";
            FastaIO.writeFasta(consensus_record, fasta_path);
            logger.info("– Fasta file:\t{}", fasta_path);

            // Define vcf output
            if (cmd.hasOption("vcf")) {
                String vcf_out = out_path + "/" + sample_name + "_" + method + ".vcf";
                VCFHeader vcf_header = VCFFileWriter.defaultHeader(ref, sample_name + "_" + method, method);
                VCFFileWriter.writeVCFFile(vcf_out, vcf_header, variant_calls);
                logger.info("– VCF file:\t\t{}", vcf_out);
            }

            // Define bed output
            if (cmd.hasOption("bed")) {
                Files.move(Path.of("roi.bed"),
                        Path.of(out_path + "/" + sample_name + "_" + method + ".bed"),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                logger.info("– ROI table:\t\t{}/{}_{}.bed", out_path, sample_name, method);
            }


        } catch (Exception e) {
            logger.error("Runtime error:", e);
            System.exit(1);
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

    private static void writeCLItoLog(Logger log, CommandLine cmd) {
        log.info("Parsed arguments:");
        log.info("BAM file:              \t{}", cmd.getOptionValue("bam"));
        log.info("Reference file:        \t{}", cmd.getOptionValue("reference"));
        log.info("Output directory:      \t{}", cmd.getOptionValue("out"));
        log.info("Minimum coverage:      \t{}", cmd.getOptionValue("cov"));
        log.info("Minimum frequency:     \t{}", cmd.getOptionValue("freq"));
        if (!cor_mode.equals(CorrectionMode.NO_COR)) {
            log.info("Damage Detection mode:\t{}", dam_det.getDetectionMode());
        }
        log.info("Damage Correction mode:\t{}", cor_mode.getModeName());
        if (cor_mode.equals(CorrectionMode.WEIGHTING)) {
            log.info("Damage profiles:      \t{}", cmd.getOptionValue("dp5"));
            log.info("                      \t{}", cmd.getOptionValue("dp3"));
        }
    }
}
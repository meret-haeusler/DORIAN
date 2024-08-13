package dorian;

import cli.CLIParser;
import datastructure.*;
import htsjdk.samtools.SAMFileHeader;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.util.SamLocusIterator;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import me.tongfei.progressbar.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import static dorian.BaseCalling.makeBaseCall;

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
    public static int cov;
    public static double freq;
    public static Character base_call;

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
        if (cor_mode.needsCorrection()) {
            dp5 = cli_parser.DP5;
            dp3 = cli_parser.DP3;
        }
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


        // MAIN PROGRAMME //
        if (cor_mode.equals(CorrectionMode.NO_COR)) {
            file_logger.info("\nCalls:");
            file_logger.info("POS\tBASE_CALL\tBASE_FREQ");
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

        // Initialise Progress bar
        StringBuilder consensus_sequence = null;
        List<VariantContext> variant_calls = List.of();
        try (ProgressBar pb = new ProgressBarBuilder()
                .setInitialMax(ref.getSequence().length())
                .setStyle(ProgressBarStyle.ASCII)
                .setTaskName("[" + time_stamp + "] INFO Variant Calling:")
                .setMaxRenderedLength(120)
                .setUpdateIntervalMillis(100)
                .build()) {

            //TODO: Add BaseCalling call and resolve returns
            ReturnTuple BaseCallingOut = makeBaseCall(reads, cov, freq, ref, "dummy");
            consensus_sequence = BaseCallingOut.getSeq();
            variant_calls = BaseCallingOut.getVariants();

            pb.step();

        } catch (Exception e){
            logger.error("Variant Calling ran into an issue: " + e.getMessage());
            System.exit(-1);
        }


        // OUTPUT //
        // Define Fasta output
        Fasta consensus_record = new Fasta(">" + sample_name + "_" + cor_mode.getShortName(), consensus_sequence.toString());
        String fasta_path = out_path.toString() + "/" + sample_name + "_" + cor_mode.getShortName() + ".fasta";
        FastaIO.writeFasta(consensus_record, fasta_path);

        // Define vcf output
        String vcf_out = out_path + "/" + sample_name + "_" + cor_mode.getShortName() + ".vcf";
        VCFHeader vcf_header = defaultHeader(ref, sample_name+ "_" + cor_mode.getShortName());
        VCFEncoder vcf_writer = new VCFEncoder(vcf_header, true, true);
        try (FileWriter writer = new FileWriter(vcf_out, false)) {
            BufferedWriter bw = new BufferedWriter(writer);

            // Write VCF header to the file
            for (VCFHeaderLine headerline : vcf_header.getMetaDataInSortedOrder()) {
                bw.write("##" + headerline);
                bw.newLine();
            }

            // Write column identifiers
            writeColumnHeader(bw, vcf_header);

            // Write each VariantContext to the VCF file
            for (VariantContext variant : variant_calls) {
                vcf_writer.write(bw, variant);
                bw.newLine();
            }

            bw.close();

        } catch (Exception e) {
            logger.error(e.getMessage());
        }


        // OUTPUT INFO //
        // Move log and roi file to output directory
        Files.move(Path.of("file.log"),
                Path.of(out_path + "/" + time_stamp + "_" + sample_name + "_" + cor_mode.getShortName() + ".log"));

        // Output result paths
        System.out.println();
        logger.info("Result files:");
        logger.info("Corrected variants written to: " + vcf_out);
        logger.info("Reconstructed genome written to: " + fasta_path);
        logger.info("Log file written to: " + out_path + "/" + time_stamp + "_" + sample_name + "_" + cor_mode.getShortName() + ".log");
        if (!cor_mode.equals(CorrectionMode.NO_COR)){
            Files.move(Path.of("roi.bed"),
                    Path.of(out_path + "/" + time_stamp + "_" + sample_name + "_" + cor_mode.getShortName() + ".bed"));
            logger.info("ROI table (IGV format) for corrected variants written to: " +
                    out_path + "/" + time_stamp + "_" + sample_name + "_" + cor_mode.getShortName() + ".bed");
        }
        System.out.println();
        logger.info("DORIAN completed successfully.");
    }

    /**
     * Writes the VCF column header to the specified output file
     *
     * @param bw        Writer for current file
     * @param vcfHeader VCF header of output data
     * @throws IOException Throws exception if fails
     */
    private static void writeColumnHeader(BufferedWriter bw, VCFHeader vcfHeader) throws IOException {
        bw.write("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT");
        for (String sample : vcfHeader.getSampleNamesInOrder()) {
            bw.write("\t" + sample);
        }
        bw.newLine();
    }

    private static VCFHeader defaultHeader(Fasta ref, String sample_name){

        VCFHeaderVersion version = VCFHeaderVersion.valueOf("VCF4_2");

        Map<String, String> contig_map =  Map.of(
                "ID", ref.getHeader().split(" ")[0].replace(">", ""),
                "length", Integer.toString(ref.getSequence().length()));
        VCFHeaderLine contig = new VCFContigHeaderLine(contig_map,0);
        VCFHeaderLine ad = new VCFFormatHeaderLine("AD", VCFHeaderLineCount.R, VCFHeaderLineType.Integer,
                "Allelic depths for the ref and alt alleles in the order listed");
        VCFHeaderLine dp = new VCFFormatHeaderLine("DP", 1, VCFHeaderLineType.Integer,
                "Approximate read depth (reads with MQ=255 or with bad mates are filtered)");
        VCFHeaderLine cor_mode_filter = new VCFInfoHeaderLine("COR_MODE", 1, VCFHeaderLineType.String,
                "Used correction mode: " + dorian.cor_mode.getModeName());
        VCFHeaderLine cov_1_filter = new VCFInfoHeaderLine("MIN_COV_1", 2, VCFHeaderLineType.String,
                "Minimal coverage filter (incl. N): " + dorian.cov);
        VCFHeaderLine freq_filter = new VCFInfoHeaderLine("MIN_FREQ", 3, VCFHeaderLineType.String,
                "Minimal base frequency filter: " + dorian.freq);

        Set<VCFHeaderLine> meta_data = new HashSet<>(Arrays.asList(contig, ad, dp, cor_mode_filter, cov_1_filter, freq_filter));

        VCFHeader header = new VCFHeader(meta_data, new ArrayList<>(Collections.singleton(sample_name)));

        header.setVCFHeaderVersion(version);

        return header;
    }
}
package cli;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;
import datastructure.CorrectionMode;
import datastructure.Fasta;
import datastructure.FastaIO;
import org.apache.commons.cli.*;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static dorian.dorian.file_logger;
import static dorian.dorian.logger;


/**
 * Command line parser
 * Parses command line arguments and validates user input.
 *
 * @author Meret Häusler
 * @version 1.0
 * @since 2024-02-15
 */

public class CLIParser {
    /**
     * Description for each command line argument
     */
    private final static String[] HELP_FLAG = new String[]{"h", "help", "Print help message"};
    private final static String[] BAM_INPUT = new String[]{"b", "bam", "BAM file of mapped reads"};
    private final static String[] DP5_INPUT = new String[]{"dp5", "damageprofile5", "Path to DamageProfile of 5' end"};
    private final static String[] DP3_INPUT = new String[]{"dp3", "damageprofile3", "Path to DamageProfile of 3' end"};
    private final static String[] OUT_PATH = new String[]{"o", "out", "Path to output directory"};
    private final static String[] COV = new String[]{"c", "coverage", "Minimum coverage for consensus calling"};
    private final static String[] FREQ = new String[]{"f", "minfreq", "Minimum frequency for consensus calling (excluding N's)"};
    private final static String[] REF_FILE = new String[]{"r", "ref-file", "Reference genome"};
    private final static String[] COR = new String[]{"m", "mode", """
                                                                        Correction modes:
                                                                        1=no correction
                                                                        2=ref-based silencing
                                                                        3=ref-free silencing
                                                                        4=ref-free weighting"""};


    public CommandLine cmd;
    public File BAM;
    public String SAMPLE_NAME;
    public CorrectionMode COR_MODE;
    public List<Double> DP5;
    public List<Double> DP3;
    public Path OUT;
    public int MIN_COV;
    public double MIN_FREQ;
    public Fasta REF;

    public CLIParser(String[] args) {

        // Initialise option parser
        Options options = new Options();
        setOptions(options);

        // Parse command line input
        CommandLineParser cliparser = new DefaultParser();
        try {
            cmd = cliparser.parse(options, args);
            // Print help message if requested
            if (cmd.hasOption(HELP_FLAG[0]) || args.length == 0) {
                printHelp(options);
                System.exit(0);
            }
            logger.info("Parsing command line input.");
            file_logger.info("CLI Parameters:");
        } catch (Exception e) {
            logger.error("Parsing failed. Reason: " + e.getMessage());
            file_logger.error("Parameter parsing failed. Reason: " + e.getMessage());
            printHelp(options);
            System.exit(-1);
        }

        // PARSING CLI //
        // BAM file
        try {
            File bam_file = new File(cmd.getOptionValue("bam"));
            logger.info("BAM file:\t\t " + bam_file);
            file_logger.info("BAM file:\t\t\t" + bam_file);
            checkExistence(bam_file);
            SAMPLE_NAME = FilenameUtils.removeExtension(bam_file.getName());
            BAM = bam_file;
        } catch (Exception e) {
            logger.error(e.getMessage());
            file_logger.error(e.getMessage());
            System.exit(-1);
        }

        // Reference
        try {
            File ref_file = new File(cmd.getOptionValue("ref-file"));
            REF = FastaIO.readFasta(cmd.getOptionValue("ref-file")).get(0);
            logger.info("Reference file: " + ref_file);
            file_logger.info("Reference file:\t\t" + ref_file);
        } catch (Exception e) {
            logger.error(e.getMessage());
            file_logger.error(e.getMessage());
            System.exit(-1);
        }

        // Output directory
        try {
            OUT = Path.of(cmd.getOptionValue("out"));
            logger.info("Output directory: " + OUT);
            file_logger.info("Output directory:\t" + OUT);
        } catch (Exception e){
            logger.error(e.getMessage());
            file_logger.error(e.getMessage());
            System.exit(-1);
        }

        // Correction mode
        try{
            int cor_idx = ((Number)cmd.getParsedOptionValue("mode")).intValue() - 1;
            COR_MODE = CorrectionMode.values()[cor_idx];
            logger.info("Correction mode:  " + COR_MODE.getModeName());
            file_logger.info("Correction mode:\t" + COR_MODE.getModeName());
        } catch (Exception e){
            logger.error("Specification for correction mode (-m) not available.\nPossible " + COR[2] + "\nGiven: " + COR_MODE);
            file_logger.error("\nERROR\tSpecification for correction mode (-m) not available.\nPossible " + COR[2] + "\nGiven: " + COR_MODE);
            System.exit(-1);
        }

        // Damage profiles
        if (COR_MODE.needsDP()) {
            try {
                Path dp5_file = Paths.get(cmd.getOptionValue("dp5"));
                Path dp3_file = Paths.get(cmd.getOptionValue("dp3"));
                logger.info("Damage profiles:  " + dp5_file + "\n\t\t\t\t\t\t\t\t\t\t\t " + dp3_file);
                file_logger.info("Damage profiles:\t" + dp5_file + "\n\t\t\t\t\t" + dp3_file);
                checkExistence(dp5_file.toFile());
                checkExistence(dp3_file.toFile());

                // Configure the TsvParser settings
                TsvParserSettings settings = new TsvParserSettings();
                settings.getFormat().setLineSeparator("\n");
                settings.setHeaderExtractionEnabled(false);

                // Create a TsvParser instance with the configured settings
                TsvParser parser = new TsvParser(settings);

                // Parse the TSV file and get the list of records
                List<String[]> dp5_list = parser.parseAll(Files.newBufferedReader(dp5_file, StandardCharsets.UTF_8));
                List<String[]> dp3_list = parser.parseAll(Files.newBufferedReader(dp3_file, StandardCharsets.UTF_8));
                DP5 = parseDamageProfile(dp5_list);
                DP3 = parseDamageProfile(dp3_list);

            } catch (Exception e) {
                logger.error(e.getMessage());
                file_logger.error(e.getMessage());
                System.exit(-1);
            }
        }

        // Minimum coverage
        try {
            MIN_COV = Integer.parseInt(cmd.getOptionValue("coverage"));
            logger.info("Minimum coverage: " + MIN_COV);
            file_logger.info("Minimum coverage:\t" + MIN_COV);
        } catch (Exception e) {
            logger.error("Coverage parameter must be an integer. Given: " + cmd.getOptionValue("coverage"));
            file_logger.error("Coverage parameter must be an integer. Given: " + cmd.getOptionValue("coverage"));
            System.exit(-1);
        }


        
        // Minimum frequency
        try {
            MIN_FREQ = Double.parseDouble(cmd.getOptionValue("minfreq"));
            logger.info("Minimum frequency: " + MIN_FREQ);
            file_logger.info("Minimum frequency:\t" + MIN_FREQ);
            if (MIN_FREQ < 0 || MIN_FREQ > 1) {
                throw new Exception();
            }
        } catch (Exception e) {
            logger.error("Frequency parameter must be between 0 and 1. Given: " + cmd.getOptionValue("minfreq"));
            file_logger.error("Frequency parameter must be between 0 and 1. Given: " + cmd.getOptionValue("minfreq"));
            System.exit(-1);
        }


        logger.info("Parsing of input files completed.\n");
    }


    /**
     * Prints help message to command line
     * see <a href="https://www.tutorialspoint.com/commons_cli/commons_cli_help_example.html">...</a>
     *
     * @param options Options for command line input
     */
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar dorian.jar [options]\n", options);
    }

    /**
     * Sets the options for command line input
     *
     * @param op Options for command line input
     */
    private static void setOptions(Options op){
        op.addOption(Option.builder()
                .argName("FILE")
                .option(BAM_INPUT[0])
                .longOpt(BAM_INPUT[1])
                .hasArg()
                .required(false)
                .desc(BAM_INPUT[2])
                .build());
        op.addOption(Option.builder()
                .argName("FILE")
                .option(DP5_INPUT[0])
                .longOpt(DP5_INPUT[1])
                .hasArg()
                .required(false)
                .desc(DP5_INPUT[2])
                .build());
        op.addOption(Option.builder()
                .argName("FILE")
                .option(DP3_INPUT[0])
                .longOpt(DP3_INPUT[1])
                .hasArg()
                .required(false)
                .desc(DP3_INPUT[2])
                .build());
        op.addOption(Option.builder()
                .argName("PATH")
                .option(OUT_PATH[0])
                .longOpt(OUT_PATH[1])
                .hasArg()
                .required(false)
                .desc(OUT_PATH[2])
                .build());
        op.addOption(Option.builder()
                .argName("INT")
                .option(COR[0])
                .longOpt(COR[1])
                .type(Number.class)
                .hasArg()
                .required(false)
                .desc(COR[2])
                .build());
        op.addOption(Option.builder()
                .argName("INT")
                .option(COV[0])
                .longOpt(COV[1])
                .hasArg()
                .required(false)
                .desc(COV[2])
                .build());
        op.addOption(Option.builder()
                .argName("DOUBLE")
                .option(FREQ[0])
                .longOpt(FREQ[1])
                .hasArg()
                .required(false)
                .desc(FREQ[2])
                .build());
        op.addOption(Option.builder()
                .argName("FILE")
                .option(REF_FILE[0])
                .longOpt(REF_FILE[1])
                .hasArg()
                .required(false)
                .desc(REF_FILE[2])
                .build());
        op.addOption(Option.builder()
                .option(HELP_FLAG[0])
                .longOpt(HELP_FLAG[1])
                .desc(HELP_FLAG[2])
                .build());
    }

    /**
     * Checks if the given file exists, throws error and terminates if not
     *
     * @param file File to check
     */
    private static void checkExistence(File file) {
        if (!file.exists()) {
            logger.error("File " + file + " does not exist.");
            file_logger.error("File " + file + " does not exist.");
            System.exit(-1);
        }
    }

    /**
     * Parses the tsv damage profile from the given file
     *
     * @param damProfile Damage profile as list of string arrays
     * @return Damage profile as list of doubles
     */
    private static List<Double> parseDamageProfile(List<String[]> damProfile) {
        List<Double> parsedProfile = new ArrayList<>();
        int col_idx = (int) Double.NEGATIVE_INFINITY;

        // Get index for C>T damage column
        for (String col_id : damProfile.get(0)) {
            if (col_id.equals("C>T")) {
                col_idx = 1;
                break;
            }
        }

        // Get entries from C>T damage column
        for (int row_idx = 1; row_idx < damProfile.size(); row_idx++) {
            parsedProfile.add(Double.valueOf(damProfile.get(row_idx)[col_idx]));
        }

        return parsedProfile;
    }

}
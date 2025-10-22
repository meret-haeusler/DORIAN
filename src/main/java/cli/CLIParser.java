package cli;

import org.apache.commons.cli.*;


/**
 * Command line parser
 * Parses command line arguments and validates user input.
 *
 * @author Meret Häusler
 * @version 1.1
 * @since 2025-05-20
 */

public class CLIParser {
    /**
     * Description for each command line argument
     */
    private final static String[] HELP_FLAG = new String[]{"h", "help", "Print help message"};
    private final static String[] BAM_INPUT = new String[]{"b", "bam", "BAM file of mapped reads"};
    private final static String[] OUTPUT_DIR = new String[]{"o", "out", "Output directory"};
    private final static String[] REFERENCE = new String[]{"r", "reference", "FASTA reference file"};
    private final static String[] DETECTION_MODE = new String[]{null, "detection", "Damage detection mode (only required if correction is s or w):\npb (Polarization-Based) or pf (Polarization-Free)"};
    private final static String[] CORRECTION_MODE = new String[]{null, "correction", "Damage correction mode: s (Silencing), w (Weighting), or nc (no correction)"};
    private final static String[] VCF_OUTPUT = new String[]{null, "vcf", "Generate VCF file for all positions with corrected bases"};
    private final static String[] BED_OUTPUT = new String[]{null, "bed", "Generate ROI table in IGV format for corrected positions"};
    private final static String[] COV = new String[]{"c", "cov", "Coverage (integer)"};
    private final static String[] FREQ = new String[]{"f", "freq", "Frequency (between 0 and 1)"};
    private final static String[] DP5_INPUT = new String[]{null, "dp5", "DP5 input file (required if correction = w)"};
    private final static String[] DP3_INPUT = new String[]{null, "dp3", "DP3 input file (required if correction = w)"};
    private final static String[] DP_FILE = new String[]{null, "dp_file", "TSV file to specify DP5 and DP3 for individual read groups (optional, overrides --dp5 and --dp3)"};


    public static CommandLine parseArguments(String[] args) {

        // Initialise option parser
        Options options = new Options();
        options.addOption(Option.builder(HELP_FLAG[0]).longOpt(HELP_FLAG[1]).desc(HELP_FLAG[2]).build());
        options.addOption(Option.builder(BAM_INPUT[0]).longOpt(BAM_INPUT[1]).hasArg().desc(BAM_INPUT[2]).build());
        options.addOption(Option.builder(OUTPUT_DIR[0]).longOpt(OUTPUT_DIR[1]).hasArg().desc(OUTPUT_DIR[2]).build());
        options.addOption(Option.builder(REFERENCE[0]).longOpt(REFERENCE[1]).hasArg().desc(REFERENCE[2]).build());
        options.addOption(Option.builder().longOpt(DETECTION_MODE[1]).hasArg().desc(DETECTION_MODE[2]).build());
        options.addOption(Option.builder().longOpt(CORRECTION_MODE[1]).hasArg().desc(CORRECTION_MODE[2]).build());
        options.addOption(Option.builder().longOpt(VCF_OUTPUT[1]).desc(VCF_OUTPUT[2]).build());
        options.addOption(Option.builder().longOpt(BED_OUTPUT[1]).desc(BED_OUTPUT[2]).build());
        options.addOption(Option.builder(COV[0]).longOpt(COV[1]).hasArg().desc(COV[2]).build());
        options.addOption(Option.builder(FREQ[0]).longOpt(FREQ[1]).hasArg().desc(FREQ[2]).build());
        options.addOption(Option.builder().longOpt(DP5_INPUT[1]).hasArg().desc(DP5_INPUT[2]).build());
        options.addOption(Option.builder().longOpt(DP3_INPUT[1]).hasArg().desc(DP3_INPUT[2]).build());
        options.addOption(Option.builder().longOpt(DP_FILE[1]).hasArg().desc(DP_FILE[2]).build());


        // Parse command line input
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);

            // Check for help flag
            if (cmd.hasOption(HELP_FLAG[0])) {
                formatter.printHelp("dorian", options);
                System.exit(0);
            }

            // Check for required arguments
            if (!cmd.hasOption(BAM_INPUT[1]) || !cmd.hasOption(OUTPUT_DIR[1]) || !cmd.hasOption(REFERENCE[1]) ||
                    !cmd.hasOption(CORRECTION_MODE[1]) || !cmd.hasOption(COV[1]) || !cmd.hasOption(FREQ[1])) {

                System.err.println("Error: Required arguments are missing.");
                formatter.printHelp("dorian", options);
                System.exit(1);
            }

            // Check that valid correction mode is provided
            String correction = cmd.getOptionValue(CORRECTION_MODE[1]);
            if (!correction.equals("s") && !correction.equals("w") && !correction.equals("nc")) {
                System.err.println("Error: Invalid correction mode. Use s, w, or nc.");
                System.exit(1);
            }

            // If correction mode is 's' or 'w', check that valid detection mode is provided
            if (!correction.equals("nc")) {
                if (!cmd.hasOption(DETECTION_MODE[1])) {
                    System.err.println("Error: Detection mode required unless correction mode is 'nc'.");
                    System.exit(1);
                } else {
                    String detection = cmd.getOptionValue(DETECTION_MODE[1]);
                    if (!detection.equals("pb") && !detection.equals("pf")) {
                        System.err.println("Error: Invalid detection mode. Use pb or pf.");
                        System.exit(1);
                    }
                }
            }

            // If correction mode is 'w', check that valid damage profile files are provided
            if (correction.equals("w")) {
                // Make dp_file, and dp5 and dp3 mutually exclusive
                if (cmd.hasOption(DP_FILE[1]) && (cmd.hasOption(DP5_INPUT[1]) || cmd.hasOption(DP3_INPUT[1]))) {
                    System.err.println("Error: Cannot use --dp_file with --dp5 or --dp3. Specify either --dp_file or both --dp5 and --dp3.");
                    System.exit(1);
                }
                // If dp5 is specified, dp3 must also be specified
                if (!cmd.hasOption(DP_FILE[1]) && (!cmd.hasOption(DP5_INPUT[1]) || !cmd.hasOption(DP3_INPUT[1]))) {
                    System.err.println("Error: --dp5 and --dp3 are both required when --dp_file is not used.");
                    System.exit(1);
                }
                // If correction mode is 'w', either dp5 and dp3, or dp_file must be specified
                if (!cmd.hasOption(DP5_INPUT[1]) && !cmd.hasOption(DP3_INPUT[1]) && !cmd.hasOption(DP_FILE[1])) {
                    System.err.println("Error: Damage profile files are required when correction mode is 'w'.");
                    System.exit(1);
                }
            }

            return cmd;

        } catch (ParseException e) {
            System.err.println("Parsing failed. Reason: " + e.getMessage());
            formatter.printHelp("dorian", options);
            System.exit(1);
            return null;
        }
    }
}

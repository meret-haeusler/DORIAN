package utils;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static dorian.dorian.logger;

public class InputValidationService {
    public static void validateInputs(CommandLine cmd) {
        checkFile(cmd, "reference");

        // Check if BAM file and BAM index file exist; Create index if missing
        checkFile(cmd, "bam");
        if (!checkBamIndex(cmd.getOptionValue("bam"))) {
            // If index file does not exist, create with samtools
            logger.info("BAM index file not found. Creating index using samtools.");
            try {
                ProcessBuilder pb = new ProcessBuilder("samtools", "index", cmd.getOptionValue("bam"));
                Process process = pb.start();
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    logger.error("Failed to create BAM index file.");
                    System.exit(1);
                } else {
                    logger.info("BAM index file successfully created.\n");
                }
            } catch (Exception e) {
                logger.error("Error while creating BAM index file: {}", e.getMessage());
                System.exit(1);
            }
        }

        // Check validity of correction mode and required damage profile files
        String correction = cmd.getOptionValue("correction");
        if (correction.equals("w")) {
            if (cmd.hasOption("dp3") && cmd.hasOption("dp5")) {
                // If both dp3 and dp5 are specified, check if they exist
                checkFile(cmd, "dp3");
                checkFile(cmd, "dp5");
            } else if (cmd.hasOption("dp_file")) {
                // If dp_file is specified, check if it exists
                checkFile(cmd, "dp_file");
            } else {
                logger.error("Damage profile files are required when correction mode is 'w'.");
                System.exit(1);
            }
        }

        // Check if frequency is between 0 and 1
        String freqStr = cmd.getOptionValue("freq");
        if (freqStr != null) {
            try {
                double freq = Double.parseDouble(freqStr);
                if (freq < 0 || freq > 1) {
                    logger.error("Frequency must be between 0 and 1.");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                logger.error("Invalid frequency value: {}", freqStr);
                System.exit(1);
            }
        }
    }

    private static void checkFile(CommandLine cmd, String option) {
        String value = cmd.getOptionValue(option);
        if (value == null) {
            logger.error("Missing required argument: --{}", option);
            System.exit(1);
        }
        Path path = Paths.get(value);
        if (!Files.exists(path)) {
            logger.error("File not found: {}", value);
            System.exit(1);
        }
    }

    private static boolean checkBamIndex(String bamPath) {
        String bamFilePath = FilenameUtils.getFullPath(bamPath);
        String bamFileBasename = FilenameUtils.getBaseName(bamPath);

        Path dir = Paths.get(bamFilePath);
        String pattern = bamFileBasename + "*.bai"; // e.g., sample*.bai

        // --- Check if any .bai file exists matching the prefix ---
        boolean indexExists = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, pattern)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    indexExists = true;
                    break;
                }
            }
        } catch (IOException e) {
            logger.error("Error reading directory {}: {}", bamFilePath, e.getMessage());
        }
        return indexExists;
    }
}

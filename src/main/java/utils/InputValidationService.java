package utils;

import org.apache.commons.cli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static dorian.dorian.logger;

public class InputValidationService {
    public static void validateInputs(CommandLine cmd) {
        checkFile(cmd, "bam");
        checkFile(cmd, "reference");

        String correction = cmd.getOptionValue("correction");
        if (correction.equals("w")) {
            checkFile(cmd, "dp3");
            checkFile(cmd, "dp5");
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
}

package utils;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DamageProfileParser {

    /**
     * Parses the tsv damage profile from the given file
     *
     * @param FilePath Path to damage profile
     * @return Damage profile as list of doubles
     */
    public static List<Double> parseDamageProfile(String FilePath) throws IOException {
        // Initialise output
        List<Double> parsedProfile = new ArrayList<>();

        // Configure the TsvParser settings
        TsvParserSettings settings = new TsvParserSettings();
        settings.getFormat().setLineSeparator("\n");
        settings.setHeaderExtractionEnabled(false);

        // Create a TsvParser instance with the configured settings
        TsvParser parser = new TsvParser(settings);

        // Parse the TSV file and get the list of records
        Path dp_path = Paths.get(FilePath);
        List<String[]> damProfile = parser.parseAll(Files.newBufferedReader(dp_path, StandardCharsets.UTF_8));

        // Get index for C>T damage column
        int col_idx = (int) Double.NEGATIVE_INFINITY;
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

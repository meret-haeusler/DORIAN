package utils;

import com.univocity.parsers.tsv.TsvParser;
import com.univocity.parsers.tsv.TsvParserSettings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DamageProfileParser {

    /**
     * Parses damage profiles if only 5' and 3' damage profiles are given.
     * @param dp5_file Path to 5' damage profile file
     * @param dp3_file Path to 3' damage profile file
     * @return Map with default read group as key and a map of damage profiles
     * @throws IOException
     */
    public static Map<String, Map<String, ArrayList<Double>>> damageProfile(String dp5_file, String dp3_file) throws IOException {

        // Parse 5' and 3' damage profile
        ArrayList<Double> dp5 = parseDamageProfile(dp5_file);
        ArrayList<Double> dp3 = parseDamageProfile(dp3_file);

        // Create a map to hold the parsed damage profiles
        Map<String, ArrayList<Double>> dpMap = new HashMap<>();
        dpMap.put("dp5", dp5);
        dpMap.put("dp3", dp3);

        // Add to parsedDP map with read group as key
        Map<String, Map<String, ArrayList<Double>>> parsedDP = new HashMap<>();
        parsedDP.put("default", dpMap);

        return parsedDP;
    }

    public static Map<String, Map<String, ArrayList<Double>>> damageProfile(String dp_file) throws IOException {
        // Initialise output map
        Map<String, Map<String, ArrayList<Double>>> parsedDP = new HashMap<>();

        // Configure the TsvParser settings
        TsvParserSettings settings = new TsvParserSettings();
        settings.getFormat().setLineSeparator("\n");
        settings.setHeaderExtractionEnabled(false);
        // Create a TsvParser instance with the configured settings
        TsvParser parser = new TsvParser(settings);
        // Parse the TSV file and get the list of records
        Path dp_path = Paths.get(dp_file);
        List<String[]> dp_list = parser.parseAll(Files.newBufferedReader(dp_path, StandardCharsets.UTF_8));

        // Iterate through each record in the parsed list (read_group, path to dp5, path to dp3)
        for (String[] record : dp_list) {
            if (record.length < 3) {
                throw new IOException("Invalid damage profile record: " + String.join("\t", record));
            }
            String read_group = record[0];
            String dp5_path = record[1];
            String dp3_path = record[2];

            // Check if the paths exist
            if (!Files.exists(Paths.get(dp5_path)) || !Files.exists(Paths.get(dp3_path))) {
                throw new IOException("Damage profile files not found: " + dp5_path + ", " + dp3_path);
            }

            // Parse the damage profiles for the read group
            ArrayList<Double> dp5 = parseDamageProfile(dp5_path);
            ArrayList<Double> dp3 = parseDamageProfile(dp3_path);

            // Create a map to hold the parsed damage profiles for this read group
            Map<String, ArrayList<Double>> dpMap = new HashMap<>();
            dpMap.put("dp5", dp5);
            dpMap.put("dp3", dp3);

            // Add to parsedDP map with read group as key
            parsedDP.put(read_group, dpMap);
        }

        return parsedDP;
    }

    /**
     * Parses the tsv damage profile from the given file
     *
     * @param FilePath Path to damage profile
     * @return Damage profile as list of doubles
     */
    private static ArrayList<Double> parseDamageProfile(String FilePath) throws IOException {
        // Initialise output
        ArrayList<Double> parsedProfile = new ArrayList<>();

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

package datastructure;

import htsjdk.samtools.SAMRecord;

import java.util.ArrayList;
import java.util.TreeMap;

public class MappingPositionTree {

    TreeMap<String, ArrayList<MappingPosition>> container = new TreeMap<>((p1, p2) -> {
        String[] splitPosition1 = p1.split("\\.");
        String[] splitPosition2 = p2.split("\\.");
        int referencePosition1 = Integer.parseInt(splitPosition1[0]);
        int referencePosition2 = Integer.parseInt(splitPosition2[0]);
        int insertionPosition1 = Integer.parseInt(splitPosition1[1]);
        int insertionPosition2 = Integer.parseInt(splitPosition2[1]);
        if (referencePosition1 == referencePosition2)
            return Integer.compare(insertionPosition1, insertionPosition2);
        else
            return Integer.compare(referencePosition1, referencePosition2);
    });


    /**
     * Adds a read record to the tree.
     * @param read A SAMRecord object representing the read.
     */
    public void addReadRecord(SAMRecord read) {
        int alignedReadLength = read.getAlignmentEnd() - read.getAlignmentStart() + 1;
        // Iterate over read positions that map to the reference
        for (int i = 0; i < alignedReadLength; i++) {
            // Get the reference position and create key
            int refPos = read.getAlignmentStart() + i;
            String key = refPos + "." + 0;
            // Create a MappingPosition object and add it to the tree
            MappingPosition position = MappingPosition.createMappingPosition(read, refPos);
            if (position != null) {
                container.computeIfAbsent(key, k -> new ArrayList<>()).add(position);
            }
        }
    }


    /** Returns the list of MappingPosition objects for a given reference position.
     * @param key String representing the reference position.
     * @return List of MappingPosition objects.
     */
    public ArrayList<MappingPosition> getMappingPositionList(String key) {
        return container.getOrDefault(key, new ArrayList<>());
    }


    /** Removes a reference position from the tree.
     * @param key String representing the reference position.
     */
    public void removeKey(String key) {
        container.remove(key);
    }


    /** Checks if the tree is empty.
     * @return True if the tree is empty, false otherwise.
     */
    public boolean isEmpty() {
        return container.isEmpty();
    }
}

package datastructure;

import java.util.ArrayList;
import java.util.TreeMap;

public class MappingPositionTree {

    TreeMap< String, ArrayList< MappingPosition >> container = new TreeMap<>((p1, p2) -> {
        String[] splitPosition1 = p1.split(".");
        String[] splitPosition2 = p2.split(".");
        int referencePosition1 = Integer.parseInt(splitPosition1[0]);
        int referencePosition2 = Integer.parseInt(splitPosition2[0]);
        int insertionPosition1 = Integer.parseInt(splitPosition1[1]);
        int insertionPosition2 = Integer.parseInt(splitPosition2[1]);
        if (referencePosition1 == referencePosition2)
            return Integer.compare(insertionPosition1, insertionPosition2);
        else
            return Integer.compare(referencePosition1, referencePosition2);
    });

}

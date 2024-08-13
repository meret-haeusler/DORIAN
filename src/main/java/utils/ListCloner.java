package utils;

import datastructure.MappingPosition;

import java.util.ArrayList;

public class ListCloner {

    public static ArrayList<MappingPosition> cloneList(ArrayList<MappingPosition> list) {
        ArrayList<MappingPosition> clonedList = new ArrayList<>();

        for (MappingPosition mp : list) {
            MappingPosition newMP = new MappingPosition(mp.base, mp.read_idx, mp.read_length, mp.is_reverse, mp.weight);
            clonedList.add(newMP);
        }

        return clonedList;
    }

}

package edu.uob;

import java.util.List;

public class Util {

    private Util() {
    }

    public static <E> boolean ListEquals(List<E> list1, List<E> list2) {
        if (list1.size() != list2.size()) return false;

        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) return false;
        }

        return true;
    }
}

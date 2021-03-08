package pl.databucket.service;

import java.util.Set;

public class Misc {

    public boolean equalsSetOfIds(Set<Long> set1, Set<Long> set2) {

        if (set1 == null || set2 == null) {
            return set1 == null && set2 == null;
        }

        if (set1.size() != set2.size()) {
            return false;
        }

        return set1.containsAll(set2);
    }
}

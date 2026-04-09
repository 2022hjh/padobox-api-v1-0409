package com.pience.padobox.utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DuplicateUtil {
	
    public static List<String> findDuplicates(List<String> list1, List<String> list2) {
        List<String> duplicates = new ArrayList<>();
        Set<String> set1 = new HashSet<>(list1); // 중복 제거를 위해 Set 사용

        for (String element : list2) {
            if (set1.contains(element)) {
                duplicates.add(element);
            }
        }
        return duplicates;
    }

}

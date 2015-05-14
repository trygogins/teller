package ru.ovsyannikov.clustering;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Georgii Ovsiannikov
 * @since 5/15/15
 */
public class ComparisonUtils {

    /**
     * deep comparison of maps with given structure
     */
    public static <K, V> boolean isEqual(Map<K, List<V>> firstMap, Map<K, List<V>> secondMap) {
        if (firstMap.size() != secondMap.size()) {
            return false;
        }

        for (K key : firstMap.keySet()) {
            if (!compare(firstMap.get(key), secondMap.get(key))) {
                return false;
            }
        }

        return true;
    }

    /**
     * lists-by-items comparison
     */
    public static <T> boolean compare(List<T> list1, List<T> list2) {
        if (list1 == null || list2 == null) {
            return list1 == list2;
        }

        if (list1.size() != list2.size()) {
            return false;
        }

        for (int i = 0; i < list1.size(); i++) {
            if (!list1.get(i).equals(list2.get(i))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Method calculates cosine similarity between two lists
     *
     * @param array1 – first array
     * @param array2 – second array
     * @return similarity between given lists
     */
    public static <T> Double getListsSimilarity(List<T> array1, List<T> array2) {
        if (array1 == null || array2 == null) {
            return array1 == array2 ? 1.0 : 0.0;
        }


        HashSet<T> items = new HashSet<>();
        items.addAll(array1.stream().collect(Collectors.toList()));
        items.addAll(array2.stream().collect(Collectors.toList()));
        if (items.isEmpty()) {
            return 0.0;
        }

        return (array1.size() + array2.size() - items.size()) / Math.sqrt(array1.size() * array2.size());
    }
}

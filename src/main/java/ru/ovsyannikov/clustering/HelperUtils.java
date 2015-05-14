package ru.ovsyannikov.clustering;

import ru.ovsyannikov.clustering.model.DistanceInfo;
import ru.ovsyannikov.parsing.model.Movie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Georgii Ovsiannikov
 * @since 5/13/15
 */
public class HelperUtils {

    /**
     * Сравнение списков по элементам
     */
    public static  <T> boolean compare(List<T> list1, List<T> list2) {
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

    /**
     * method for retrieving precalculated distance between given collections from another collection
     * @param distances – list of pairwise distances, from which the target distance will be retrieved
     * @param targetCollection1 – first collection
     * @param targetCollection2 – second collection
     * @param <T> type of items
     * @return precalculated distance. @See {@link ru.ovsyannikov.clustering.CategoricalDistanceProcessor#calculateAttributesDistances}
     */
    public static <T> double getDistance(List<DistanceInfo<T>> distances, List<T> targetCollection1, List<T> targetCollection2) {
        if (compare(targetCollection1, targetCollection2)) {
            return 0;
        }

        for (DistanceInfo<T> distanceInfo : distances) {
            if (HelperUtils.compare(distanceInfo.getCollection1(), targetCollection1) &&
                    HelperUtils.compare(distanceInfo.getCollection2(), targetCollection2)) {
                return distanceInfo.getDistance();
            }
        }

        throw new IllegalArgumentException("no appropriate pair of collections provided!");
    }

    public static List<Movie> getTestMovies() {
        List<Movie> movies = new ArrayList<>();
        movies.add(new Movie("Godfather2", "Scorsece", Arrays.asList("crime"), Arrays.asList("De Niro")));
        movies.add(new Movie("Good Fellas", "Coppola", Arrays.asList("crime"), Arrays.asList("De Niro")));
        movies.add(new Movie("vertigo", "Hitchcock", Arrays.asList("thriller"), Arrays.asList("Stewart")));
        movies.add(new Movie("N by NW", "Hitchcock", Arrays.asList("thriller"), Arrays.asList("Grant")));
        movies.add(new Movie("Bishop's Life", "Koster", Arrays.asList("comedy"), Arrays.asList("Grant")));
        movies.add(new Movie("Harvey", "Koster", Arrays.asList("comedy"), Arrays.asList("Stewart")));

        return movies;
    }
}

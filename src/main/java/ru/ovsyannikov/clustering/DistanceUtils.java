package ru.ovsyannikov.clustering;

import ru.ovsyannikov.clustering.model.DistanceInfo;
import ru.ovsyannikov.parsing.model.Movie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Georgii Ovsiannikov
 * @since 5/13/15
 */
public class DistanceUtils {

    /**
     * method for retrieving precalculated distance between given collections from another collection
     * @param distances – list of pairwise distances, from which the target distance will be retrieved
     * @param targetCollection1 – first collection
     * @param targetCollection2 – second collection
     * @param <T> type of items
     * @return precalculated distance. @See {@link ru.ovsyannikov.clustering.CategoricalDistanceProcessor#calculateAttributesDistances}
     */
    public static <T> double getDistance(List<DistanceInfo<T>> distances, List<T> targetCollection1, List<T> targetCollection2) {
        if (ComparisonUtils.compare(targetCollection1, targetCollection2)) {
            return 0;
        }

        for (DistanceInfo<T> distanceInfo : distances) {
            if (ComparisonUtils.compare(distanceInfo.getCollection1(), targetCollection1) &&
                    ComparisonUtils.compare(distanceInfo.getCollection2(), targetCollection2)) {
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

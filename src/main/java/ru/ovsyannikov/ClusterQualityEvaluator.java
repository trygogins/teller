package ru.ovsyannikov;

import ru.ovsyannikov.clustering.KMeansProcessor;
import ru.ovsyannikov.clustering.model.ClusterCenter;
import ru.ovsyannikov.parsing.model.Movie;

import java.util.HashMap;
import java.util.List;

/**
 * @author Georgii Ovsiannikov
 * @since 5/16/15
 */
public class ClusterQualityEvaluator {

    private List<Movie> movies;

    public ClusterQualityEvaluator(List<Movie> movies) {
        this.movies = movies;
    }

    public void evaluateClusters() {
        KMeansProcessor processor = new KMeansProcessor(movies);
        HashMap<ClusterCenter, List<Movie>> clusters = processor.performClustering((int) Math.sqrt(movies.size() / 2));

        for (ClusterCenter clusterCenter : clusters.keySet()) {
            List<Movie> moviesInCluster = clusters.get(clusterCenter);
        }
    }

}

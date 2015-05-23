package ru.ovsyannikov.clustering;

import ru.ovsyannikov.parsing.model.Movie;

import java.util.List;

/**
 * @author Georgii Ovsiannikov
 * @since 5/20/15
 */
public class EntropyEstimator {

    private List<Movie> votedMovies;
    List<List<Movie>> clusters;

    public EntropyEstimator(List<Movie> votedMovies, List<List<Movie>> clusters) {
        this.votedMovies = votedMovies;
        this.clusters = clusters;
    }

    public boolean isEnough() {
        return true;
    }

}

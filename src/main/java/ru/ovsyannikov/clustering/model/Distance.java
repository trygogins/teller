package ru.ovsyannikov.clustering.model;

import ru.ovsyannikov.parsing.Movie;

/**
 * @author Georgii Ovsiannikov
 * @since 5/11/15
 */
public class Distance {

    private Movie movie;
    private Double distance;

    public Distance() {
        // nothing to do here
    }

    public Distance(Movie movie, Double distance) {
        this.movie = movie;
        this.distance = distance;
    }

    public Movie getMovie() {
        return movie;
    }

    public void setMovie(Movie movie) {
        this.movie = movie;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    @Override
    public String toString() {
        return "Distance{" +
                "title=" + movie.getTitle() +
                ", distance=" + distance +
                '}';
    }
}

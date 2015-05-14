package ru.ovsyannikov.clustering.model;

import ru.ovsyannikov.parsing.Movie;

import java.util.*;

/**
 * @author Georgii Ovsiannikov
 * @since 5/11/15
 */
public class ClusterCenter {

    Map<List<String>, Integer> actors = new HashMap<>();
    Map<List<String>, Integer> genres = new HashMap<>();
    Map<List<String>, Integer> directors = new HashMap<>();
    Map<List<String>, Integer> keywords = new HashMap<>();
    Integer Nc;

    public ClusterCenter(List<Movie> movies) {
        Nc = movies.size();
        for (Movie movie : movies) {
            Integer count = actors.getOrDefault(movie.getActors(), 0);
            actors.put(movie.getActors(), count + 1);
            count = genres.getOrDefault(movie.getGenres(), 0);
            genres.put(movie.getGenres(), count + 1);
            count = directors.getOrDefault(Arrays.asList(movie.getDirector()), 0);
            directors.put(Arrays.asList(movie.getDirector()), count + 1);
            count = keywords.getOrDefault(movie.getKeywords(), 0);
            keywords.put(movie.getKeywords(), count + 1);
        }
    }

    public Map<List<String>, Integer> getActors() {
        return actors;
    }

    public Map<List<String>, Integer> getGenres() {
        return genres;
    }

    public Map<List<String>, Integer> getDirectors() {
        return directors;
    }

    public Map<List<String>, Integer> getKeywords() {
        return keywords;
    }

    public Integer getNc() {
        return Nc;
    }
}

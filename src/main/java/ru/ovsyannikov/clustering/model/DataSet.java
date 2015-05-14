package ru.ovsyannikov.clustering.model;

import ru.ovsyannikov.parsing.model.Movie;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * @author Georgii Ovsiannikov
 * @since 5/11/15
 */
public class DataSet {

    List<List<String>> actors = new ArrayList<>();
    List<List<String>> genres = new ArrayList<>();
    List<List<String>> directors = new ArrayList<>();
    List<List<String>> keywords = new ArrayList<>();

    public DataSet() {
    }

    public DataSet(List<Movie> movies) {
        movies.stream().forEach(this::addItem);
    }

    public void addItem(Movie movie) {
        actors.add(movie.getActors());
        genres.add(movie.getGenres());
        directors.add(Arrays.asList(movie.getDirector()));
        keywords.add(movie.getKeywords());
    }

    public List<List<String>> getActors() {
        return actors;
    }

    public List<List<String>> getGenres() {
        return genres;
    }

    public List<List<String>> getDirectors() {
        return directors;
    }

    public List<List<String>> getKeywords() {
        return keywords;
    }

    public void removeDuplicates() {
        actors = new ArrayList<>(new HashSet<>(actors));
        genres = new ArrayList<>(new HashSet<>(genres));
        directors = new ArrayList<>(new HashSet<>(directors));
        keywords = new ArrayList<>(new HashSet<>(keywords));
    }
}

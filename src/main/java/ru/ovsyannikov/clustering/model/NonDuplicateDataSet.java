package ru.ovsyannikov.clustering.model;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Georgii Ovsiannikov
 * @since 5/11/15
 */
public class NonDuplicateDataSet {

    Set<List<String>> actors = new HashSet<>();
    Set<List<String>> genres = new HashSet<>();
    Set<List<String>> directors = new HashSet<>();
    Set<List<String>> keywords = new HashSet<>();

    public NonDuplicateDataSet() {
    }

    public NonDuplicateDataSet(DataSet dataSet) {
        actors = new HashSet<>(dataSet.getActors());
        genres = new HashSet<>(dataSet.getGenres());
        directors = new HashSet<>(dataSet.getDirectors());
        keywords = new HashSet<>(dataSet.getKeywords());
    }

    public Set<List<String>> getActors() {
        return actors;
    }

    public Set<List<String>> getGenres() {
        return genres;
    }

    public Set<List<String>> getDirectors() {
        return directors;
    }

    public Set<List<String>> getKeywords() {
        return keywords;
    }
}

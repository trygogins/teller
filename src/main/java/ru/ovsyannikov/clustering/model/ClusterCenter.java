package ru.ovsyannikov.clustering.model;

import ru.ovsyannikov.parsing.model.Movie;

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
    List<String> movieTitles = new ArrayList<>();
    Integer Nc;

    public ClusterCenter(List<Movie> movies) {
        Nc = movies.size();
        for (Movie movie : movies) {
            movieTitles.add(movie.getTitle());
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClusterCenter that = (ClusterCenter) o;

        if (Nc != null ? !Nc.equals(that.Nc) : that.Nc != null) return false;
        if (actors != null ? !actors.equals(that.actors) : that.actors != null) return false;
        if (directors != null ? !directors.equals(that.directors) : that.directors != null) return false;
        if (genres != null ? !genres.equals(that.genres) : that.genres != null) return false;
        if (keywords != null ? !keywords.equals(that.keywords) : that.keywords != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = actors != null ? actors.hashCode() : 0;
        result = 31 * result + (genres != null ? genres.hashCode() : 0);
        result = 31 * result + (directors != null ? directors.hashCode() : 0);
        result = 31 * result + (keywords != null ? keywords.hashCode() : 0);
        result = 31 * result + (Nc != null ? Nc.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ClusterCenter{" +
                "Nc=" + Nc +
                ", movieTitles=" + movieTitles +
                '}';
    }
}

package ru.ovsyannikov.clustering;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.stereotype.Service;
import ru.ovsyannikov.parsing.Movie;
import ru.ovsyannikov.parsing.MovieStorageHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Georgii Ovsiannikov
 * @since 5/6/15
 */
@Service
public class MovieSimilarityEstimator {

    @Autowired
    private MovieStorageHelper storageHelper;

    @Autowired
    private JdbcTemplate template;

    public List<Movie> getMovies(String tableName) {
        List<Movie> movies = storageHelper.getMoviesByKinopoiskId(template.query("select distinct kinopoisk_id from " + tableName, new SingleColumnRowMapper<>(Long.class)));
        return movies;
    }

    private List<FuzzyCMeansProcessor.Distance> calculateSimilarity(List<Movie> movies) {
        List<FuzzyCMeansProcessor.Distance> result = new ArrayList<>();
        for (Movie m1 : movies) {
            movies.stream().filter(m2 -> m1 != m2).forEach(m2 -> result.add(new FuzzyCMeansProcessor.Distance(m1, m2, similarity(m1, m2))));
        }

        return result;
    }

    /**
     * similarity between two movies is given as an average of attributes similarities
     * @return the similarity
     */
    public Double similarity(Movie m1, Movie m2) {
        double castSimilarity = getListsSimilarity(m1.getActors(), m2.getActors());
        double keywordsSimilarity = getListsSimilarity(m1.getKeywords(), m2.getKeywords());
        double genreSimilarity = getListsSimilarity(m1.getGenres(), m2.getGenres());
        double directorSimilarity = StringUtils.equals(m1.getDirector(), m2.getDirector()) ? 1.0 : 0.0;

        return (castSimilarity + keywordsSimilarity + genreSimilarity + directorSimilarity) / 4;
    }

    /**
     * Method calculates cosine similarity between two lists
     *
     * @param array1 – first array
     * @param array2 – second array
     * @return similarity between given lists
     */
    private <T> Double getListsSimilarity(List<T> array1, List<T> array2) {
        if (array1 == null || array2 == null) {
            return array1 == array2 ? 1.0 : 0.0;
        }

        HashSet<T> words = new HashSet<>();
        words.addAll(array1.stream().collect(Collectors.toList()));
        words.addAll(array2.stream().collect(Collectors.toList()));

        return (array1.size() + array2.size() - words.size()) / Math.sqrt(array1.size() * array2.size());
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        MovieSimilarityEstimator estimator = context.getBean(MovieSimilarityEstimator.class);
        List<Movie> movies = estimator.getMovies("votes2");
        estimator.calculateSimilarity(movies);
    }

}

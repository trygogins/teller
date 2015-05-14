package ru.ovsyannikov.clustering;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.stereotype.Service;
import ru.ovsyannikov.parsing.MovieStorageHelper;
import ru.ovsyannikov.parsing.model.Movie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private Map<Pair<Movie, Movie>, Double> calculateSimilarity(List<Movie> movies) {
        Map<Pair<Movie, Movie>, Double> result = new HashMap<>();
        for (Movie m1 : movies) {
            movies.stream().filter(m2 -> m1 != m2).forEach(m2 -> result.put(new ImmutablePair<>(m1, m2), similarity(m1, m2)));
        }

        return result;
    }

    /**
     * similarity between two movies is given as an average of attributes similarities
     * @return the similarity
     */
    public Double similarity(Movie m1, Movie m2) {
        double castSimilarity = ComparisonUtils.getListsSimilarity(m1.getActors(), m2.getActors());
        double keywordsSimilarity = ComparisonUtils.getListsSimilarity(m1.getKeywords(), m2.getKeywords());
        double genreSimilarity = ComparisonUtils.getListsSimilarity(m1.getGenres(), m2.getGenres());
        double directorSimilarity = StringUtils.equals(m1.getDirector(), m2.getDirector()) ? 1.0 : 0.0;

        return (castSimilarity + keywordsSimilarity + genreSimilarity + directorSimilarity) / 4;
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        MovieSimilarityEstimator estimator = context.getBean(MovieSimilarityEstimator.class);
        List<Movie> movies = estimator.getMovies("votes2");
        estimator.calculateSimilarity(movies);
    }

}

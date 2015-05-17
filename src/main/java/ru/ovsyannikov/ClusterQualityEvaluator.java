package ru.ovsyannikov;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
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

    public void evaluateClusters(JdbcTemplate template) {
        KMeansProcessor processor = new KMeansProcessor(movies, template);
        HashMap<ClusterCenter, List<Movie>> clusters = processor.performClustering((int) Math.sqrt(movies.size() / 2));

        for (ClusterCenter clusterCenter : clusters.keySet()) {
            List<Movie> moviesInCluster = clusters.get(clusterCenter);

        }
    }

    /**
     * @return Root Mean Squared Error for movies' votes
     */
    private double getRMSE(List<Movie> movies) {
        return movies.stream().mapToDouble(movie -> {
            double average = movie.getVotes().stream().mapToInt(Movie.UserVote::getVote).average().orElse(0.0);
            return Math.sqrt(movie.getVotes().stream()
                    .mapToDouble(uv -> (uv.getVote() - average) * (uv.getVote() - average))
                    .sum() / movie.getVotes().size());
        }).average().orElse(0.0);
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        MovieStorageHelper storageHelper = context.getBean(MovieStorageHelper.class);
        List<Movie> movies = storageHelper.getMovies("votes3");
        JdbcTemplate template = context.getBean(JdbcTemplate.class);
        ClusterQualityEvaluator evaluator = new ClusterQualityEvaluator(movies);
        KMeansProcessor processor = new KMeansProcessor(movies, template);

        HashMap<ClusterCenter, List<Movie>> clusters = processor.performClustering((int) Math.sqrt(movies.size() / 2));
        for (ClusterCenter center : clusters.keySet()) {
            System.out.println(center.toString() + ": RMSE=" + evaluator.getRMSE(clusters.get(center)));
        }
    }
}

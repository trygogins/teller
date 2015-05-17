package ru.ovsyannikov.clustering;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.ovsyannikov.MovieStorageHelper;
import ru.ovsyannikov.clustering.model.*;
import ru.ovsyannikov.parsing.model.Movie;

import java.util.*;

/**
 * The class contains an implementation of k-means clustering algorithm
 *
 * @author Georgii Ovsiannikov
 * @since 5/6/15
 */
public class KMeansProcessor {

    private static final Logger logger = LoggerFactory.getLogger(KMeansProcessor.class);

    private Random random = new Random();
    private NonDuplicateDataSet moviesSet;
    private List<Movie> movies;
    private Map<DistanceKey, Double> distances = new HashMap<>();

    public KMeansProcessor(List<Movie> movies) {
        this(movies, null);
    }

    public KMeansProcessor(List<Movie> movies, JdbcTemplate template) {
        distances = new HashMap<>();
        Multimap<String, DistanceInfo<String>> dist = getStringDistanceInfoMultimap(movies, template);
        for (String key : dist.keySet()) {
            for (DistanceInfo<String> info : dist.get(key)) {
                distances.put(new DistanceKey(key, info.getCollection1(), info.getCollection2()), info.getDistance());
                distances.put(new DistanceKey(key, info.getCollection2(), info.getCollection1()), info.getDistance());
            }
        }

        this.moviesSet = new NonDuplicateDataSet(new DataSet(movies));
        this.movies = movies;
    }

    private Multimap<String, DistanceInfo<String>> getStringDistanceInfoMultimap(List<Movie> movies, JdbcTemplate template) {
        Multimap<String, DistanceInfo<String>> dist;CategoricalDistanceProcessor distanceProcessor = new CategoricalDistanceProcessor();
        if (template != null) {
            dist = HashMultimap.create();
            List<DistanceInfo<String>> infoList = template.query("select * from distances", new BeanPropertyRowMapper(DistanceInfo.class));
            for (DistanceInfo<String> info : infoList) {
                dist.put(info.getType(), info);
            }
        } else {
            dist = distanceProcessor.calculateAttributesDistances(new DataSet(movies));
        }
        return dist;
    }

    public HashMap<ClusterCenter, List<Movie>> performClustering(int numClusters) {
        HashMap<ClusterCenter, List<Movie>> clusters = new HashMap<>();
        HashMap<ClusterCenter, List<Movie>> previousClusters = new HashMap<>();
        int iterations = 0;

        // randomly given centers
        for (int i = 0; i < numClusters; i++) {
            clusters.put(new ClusterCenter(Arrays.asList(movies.get(random.nextInt(movies.size())))), new ArrayList<>());
        }

        while (true) {
            iterations ++;
            for (Movie movie : movies) {
                double minDistance = Double.MAX_VALUE;
                ClusterCenter minDistanceCenter = null;
                for (ClusterCenter clusterCenter : clusters.keySet()) {
                    double distance = distance(movie, clusterCenter);
                    if (distance < minDistance) {
                        minDistance = distance;
                        minDistanceCenter = clusterCenter;
                    }
                }

                clusters.getOrDefault(minDistanceCenter, new ArrayList<>()).add(movie);
            }

            if (ComparisonUtils.isEqual(clusters, previousClusters)) {
                break;
            }

            previousClusters.clear();
            for (ClusterCenter clusterCenter : clusters.keySet()) {
                previousClusters.put(clusterCenter, clusters.get(clusterCenter));
            }

            clusters.clear();
            for (ClusterCenter clusterCenter : previousClusters.keySet()) {
                clusters.put(new ClusterCenter(previousClusters.get(clusterCenter)), new ArrayList<>());
            }
        }

        logger.info("{} iterations for {} movies and {} clusters", iterations, movies.size(), numClusters);
        return clusters;
    }

    /**
     * Calculates distance between a movie and a cluster center using `Cluster centers for mixed data sets`
     */
    public double distance(Movie movie, ClusterCenter center) {
        double dist = 0.0;
        for (List<String> actors : moviesSet.getActors()) {
            double actorsDistance = (double) center.getActors().getOrDefault(actors, 0) / center.getNc() *
                    distances.getOrDefault(new DistanceKey("actors", movie.getActors(), actors), 0.0);
            dist += actorsDistance * actorsDistance;
        }
        for (List<String> genres : moviesSet.getGenres()) {
            double genresDistance = (double) center.getGenres().getOrDefault(genres, 0) / center.getNc() *
                    distances.getOrDefault(new DistanceKey("genres", movie.getGenres(), genres), 0.0);
            dist += genresDistance * genresDistance;
        }
        for (List<String> directors : moviesSet.getDirectors()) {
            double directorsDistance = (double) center.getDirectors().getOrDefault(directors, 0) / center.getNc() *
                    distances.getOrDefault(new DistanceKey("directors", Arrays.asList(movie.getDirector()), directors), 0.0);
            dist += directorsDistance * directorsDistance;
        }
        for (List<String> keywords : moviesSet.getKeywords()) {
            double keywordsDistance = (double) center.getKeywords().getOrDefault(keywords, 0) / center.getNc() *
                    distances.getOrDefault(new DistanceKey("keywords", movie.getKeywords(), keywords), 0.0);
            dist += keywordsDistance * keywordsDistance;
        }

        return dist;
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        MovieStorageHelper storageHelper = context.getBean(MovieStorageHelper.class);
        JdbcTemplate template = context.getBean(JdbcTemplate.class);
        KMeansProcessor processor = new KMeansProcessor(storageHelper.getMovies("votes2"), template);

        HashMap<ClusterCenter, List<Movie>> clusteredMovies = processor.performClustering((int) Math.sqrt(processor.movies.size() / 2));
        System.out.println(clusteredMovies);
    }
}

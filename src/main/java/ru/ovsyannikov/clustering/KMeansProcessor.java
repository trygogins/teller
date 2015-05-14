package ru.ovsyannikov.clustering;

import com.google.common.collect.Multimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;
import ru.ovsyannikov.clustering.model.ClusterCenter;
import ru.ovsyannikov.clustering.model.DataSet;
import ru.ovsyannikov.clustering.model.DistanceInfo;
import ru.ovsyannikov.clustering.model.NonDuplicateDataSet;
import ru.ovsyannikov.parsing.model.Movie;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * The class contains an implementation of c-means clustering algorithm
 *
 * @author Georgii Ovsiannikov
 * @since 5/6/15
 */
@Service
public class KMeansProcessor {

    private static final Logger logger = LoggerFactory.getLogger(KMeansProcessor.class);

    @Autowired
    private MovieSimilarityEstimator similarityEstimator;
    @Autowired
    private CategoricalDistanceProcessor distanceProcessor;

    private List<Movie> movies;
    private Multimap<String, DistanceInfo<String>> distances;

    private Random random = new Random();

    @PostConstruct
    public void init() {
        movies = Boolean.valueOf(System.getProperty("ru.ovsyannikov.teller.production")) ?
                similarityEstimator.getMovies("votes2") : DistanceUtils.getTestMovies();
        distances = distanceProcessor.calculateAttributesDistances();
    }

    /**
     * Performs clustering
     */
    public HashMap<ClusterCenter, List<Movie>> cluster(int numClusters) {
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
        NonDuplicateDataSet dataSet = new NonDuplicateDataSet(new DataSet(movies));
        double dist = 0.0;
        for (List<String> actors : dataSet.getActors()) {
            double actorsDistance = (double) center.getActors().getOrDefault(actors, 0) / center.getNc() *
                    DistanceUtils.getDistance(new ArrayList<>(distances.get("actors")), movie.getActors(), actors);
            dist += actorsDistance * actorsDistance;
        }
        for (List<String> genres : dataSet.getGenres()) {
            double genresDistance = (double) center.getGenres().getOrDefault(genres, 0) / center.getNc() *
                    DistanceUtils.getDistance(new ArrayList<>(distances.get("genres")), movie.getGenres(), genres);
            dist += genresDistance * genresDistance;
        }
        for (List<String> directors : dataSet.getDirectors()) {
            double directorsDistance = (double) center.getDirectors().getOrDefault(directors, 0) / center.getNc() *
                    DistanceUtils.getDistance(new ArrayList<>(distances.get("directors")), Arrays.asList(movie.getDirector()), directors);
            dist += directorsDistance * directorsDistance;
        }
        for (List<String> keywords : dataSet.getKeywords()) {
            double keywordsDistance = (double) center.getKeywords().getOrDefault(keywords, 0) / center.getNc() *
                    DistanceUtils.getDistance(new ArrayList<>(distances.get("keywords")), movie.getKeywords(), keywords);
            dist += keywordsDistance * keywordsDistance;
        }

        return dist;
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        KMeansProcessor processor = context.getBean(KMeansProcessor.class);

        List<Movie> movies = processor.movies;
        for (Movie movie1 : movies) {
            movies.stream().filter(movie2 -> !movie1.equals(movie2)).forEach(movie2 ->
                System.out.println(movie1.getTitle() + " vs. " + movie2.getTitle() + " = " +
                        processor.distance(movie1, new ClusterCenter(Arrays.asList(movie2)))));
        }

        HashMap<ClusterCenter, List<Movie>> clusteredMovies = processor.cluster(2);
        System.out.println(clusteredMovies);
    }
}

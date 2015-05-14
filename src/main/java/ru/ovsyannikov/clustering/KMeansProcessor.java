package ru.ovsyannikov.clustering;

import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;
import ru.ovsyannikov.clustering.model.*;
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

    @Autowired
    private MovieSimilarityEstimator similarityEstimator;
    @Autowired
    private CategoricalDistanceProcessor distanceProcessor;

    private List<Movie> movies;
    private Multimap<String, DistanceInfo<String>> distances;

    @PostConstruct
    public void init() {
        movies = Boolean.valueOf(System.getProperty("ru.ovsyannikov.teller.production")) ?
                similarityEstimator.getMovies("votes2") : HelperUtils.getTestMovies();
        distances = distanceProcessor.calculateAttributesDistances();
    }

    /**
     * Performs clustering
     */
    public void cluster(int numClusters) {
        Map<Movie, List<Distance>> centroids = new HashMap<>();
        // randomly given centroids
        for (int i = 0; i < numClusters; i++) {
            centroids.put(movies.get(i), new ArrayList<>());
        }

        boolean[][] accessory = new boolean[numClusters][movies.size()];

        for (Movie movie : movies) {
            for (Movie centroidMovie : centroids.keySet()) {
                centroids.get(centroidMovie).add(new Distance(movie, similarityEstimator.similarity(centroidMovie, movie)));
            }
        }

        for (int i = 0; i < movies.size(); i++) {
            double maximumSimilarity = 0.0;
            int maximumSimilarityIndex = 0;
            int currentIndex = 0;
            for (Movie centroid : centroids.keySet()) {
                double similarity = centroids.get(centroid).get(i).getDistance();
                if (similarity > maximumSimilarity) {
                    maximumSimilarity = similarity;
                    maximumSimilarityIndex = currentIndex;
                }

                currentIndex++;
            }

            accessory[maximumSimilarityIndex][i] = true;
        }

        System.out.println(centroids);
    }

    /**
     * Calculates distance between a movie and a cluster center using `Cluster centers for mixed data sets`
     */
    public double distance(Movie movie, ClusterCenter center) {
        NonDuplicateDataSet dataSet = new NonDuplicateDataSet(new DataSet(movies));
        double dist = 0.0;
        for (List<String> actors : dataSet.getActors()) {
            double actorsDistance = center.getActors().getOrDefault(actors, 0) / center.getNc() *
                    HelperUtils.getDistance(new ArrayList<>(distances.get("actors")), movie.getActors(), actors);
            dist += actorsDistance * actorsDistance;
        }
        for (List<String> genres : dataSet.getGenres()) {
            double genresDistance = center.getGenres().getOrDefault(genres, 0) / center.getNc() *
                    HelperUtils.getDistance(new ArrayList<>(distances.get("genres")), movie.getGenres(), genres);
            dist += genresDistance * genresDistance;
        }
        for (List<String> directors : dataSet.getDirectors()) {
            double directorsDistance = center.getDirectors().getOrDefault(directors, 0) / center.getNc() *
                    HelperUtils.getDistance(new ArrayList<>(distances.get("directors")), Arrays.asList(movie.getDirector()), directors);
            dist += directorsDistance * directorsDistance;
        }
        for (List<String> keywords : dataSet.getKeywords()) {
            double keywordsDistance = center.getKeywords().getOrDefault(keywords, 0) / center.getNc() *
                    HelperUtils.getDistance(new ArrayList<>(distances.get("keywords")), movie.getKeywords(), keywords);
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
    }
}

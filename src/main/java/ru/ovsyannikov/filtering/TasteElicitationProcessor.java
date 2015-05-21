package ru.ovsyannikov.filtering;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.jdbc.MySQLJDBCDataModel;
import org.apache.mahout.cf.taste.impl.model.jdbc.ReloadFromJDBCDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;
import ru.ovsyannikov.MovieStorageHelper;
import ru.ovsyannikov.clustering.EntropyEstimator;
import ru.ovsyannikov.clustering.KMeansProcessor;
import ru.ovsyannikov.clustering.model.ClusterCenter;
import ru.ovsyannikov.elicitation.SplitterDeterminant;
import ru.ovsyannikov.exceptions.NotEnoughVotesException;
import ru.ovsyannikov.parsing.model.Movie;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author Georgii Ovsiannikov
 * @since 4/22/15
 */
@Service
public class TasteElicitationProcessor {

    public static final Integer MINIMAL_VOTES = 20;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private MovieStorageHelper movieStorageHelper;

    private DataModel dataModel;
    private List<List<Movie>> movieClusters;
    private SplitterDeterminant splitterDeterminant = new SplitterDeterminant();

    @PostConstruct
    public void init() {
        try {
            dataModel = new ReloadFromJDBCDataModel(new MySQLJDBCDataModel(dataSource, "votes", "user_id", "kinopoisk_id", "vote", "dt"));
            KMeansProcessor clusteringProcessor = new KMeansProcessor();
            ConcurrentMap<ClusterCenter, List<Movie>> clusters = clusteringProcessor.doClustering(movieStorageHelper.getMovies("votes5"));
            movieClusters = clusters.keySet().stream()
                    .map(clusters::get)
                    .collect(Collectors.toList());

        } catch (TasteException e) {
            throw new IllegalArgumentException("Unable to create MySQL Data Model!", e);
        }
    }

    public List<RecommendedItem> getRecommendedMovies(Long userId) throws TasteException {
        EntropyEstimator estimator = new EntropyEstimator(movieStorageHelper.getVotedMovies(userId), movieClusters);
        if (!estimator.isEnough()) {
            throw new NotEnoughVotesException(-1);
        }

        UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
        UserNeighborhood neighborhood = new NearestNUserNeighborhood(5, similarity, dataModel);
        GenericUserBasedRecommender recommender = new GenericUserBasedRecommender(dataModel, neighborhood, similarity);

        return recommender.recommend(userId, 5);
    }

    public List<Movie> getMoviesToVote(Long userId) {
        // TODO: get most relevant movies (sort clusters by size&popularity, get splitters etc.)
        movieClusters.sort((l1, l2) -> l1.size() > l2.size() ? 1 : l1.size() < l2.size() ? -1 : 0);
        List<Movie> watchedMovies = movieStorageHelper.getVotedMovies(userId);

        List<List<Movie>> notWatchedClusters = movieClusters.stream()
                .filter(m -> intersection(m, watchedMovies).size() > 0)
                .collect(Collectors.toList());

        return notWatchedClusters.stream()
                .map(splitterDeterminant::getSplitter)
                .collect(Collectors.toList());
    }

    private List<Movie> intersection(List<Movie> movies1, List<Movie> movies2) {
        return movies1.stream()
                .filter(movies2::contains)
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws TasteException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        TasteElicitationProcessor provider = context.getBean(TasteElicitationProcessor.class);
        List<RecommendedItem> recommendedMovies = provider.getRecommendedMovies(4230l);
        System.out.println(recommendedMovies);
    }

}

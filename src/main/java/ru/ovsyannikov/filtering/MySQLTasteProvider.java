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
import ru.ovsyannikov.exceptions.NotEnoughVotesException;
import ru.ovsyannikov.parsing.Movie;
import ru.ovsyannikov.parsing.MovieStorageHelper;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.List;

/**
 * @author Georgii Ovsiannikov
 * @since 4/22/15
 */
@Service
public class MySQLTasteProvider {

    public static final Integer MINIMAL_VOTES = 20;

    @Autowired
    private DataSource dataSource;
    @Autowired
    private MovieStorageHelper movieStorageHelper;

    private DataModel dataModel;

    @PostConstruct
    public void init() {
        try {
            dataModel = new ReloadFromJDBCDataModel(new MySQLJDBCDataModel(dataSource, "votes", "user_id", "kinopoisk_id", "vote", "dt"));
        } catch (TasteException e) {
            throw new IllegalArgumentException("Unable to create MySQL Data Model!", e);
        }
    }

    public List<RecommendedItem> getRecommendedMovies(Long userId) throws TasteException {
        Integer votes = movieStorageHelper.getVotesCount(userId);
        if (votes < MINIMAL_VOTES) {
            throw new NotEnoughVotesException(votes);
        }

        UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
        UserNeighborhood neighborhood = new NearestNUserNeighborhood(5, similarity, dataModel);
        GenericUserBasedRecommender recommender = new GenericUserBasedRecommender(dataModel, neighborhood, similarity);

        return recommender.recommend(userId, 5);
    }

    public List<Movie> getMoviesToVote(Long userId) {
        return movieStorageHelper.getMoviesToVote(userId);
    }

    public static void main(String[] args) throws TasteException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        MySQLTasteProvider provider = context.getBean(MySQLTasteProvider.class);
        List<RecommendedItem> recommendedMovies = provider.getRecommendedMovies(4230l);
        System.out.println(recommendedMovies);
    }

}

package ru.ovsyannikov.filtering;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.jdbc.MySQLJDBCDataModel;
import org.apache.mahout.cf.taste.impl.model.jdbc.ReloadFromJDBCDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.NearestNUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Georgii Ovsiannikov
 * @since 3/18/15
 */
public class EvaluateRecommender {

    public static void main(String[] args) throws IOException, TasteException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        DataSource dataSource = context.getBean(DataSource.class);
        DataModel dataModel = new ReloadFromJDBCDataModel(new MySQLJDBCDataModel(dataSource, "votes", "user_id", "kinopoisk_id", "vote", "dt"));

        List<Double> resultQuality = new ArrayList<>();
        for (int i = 1; i < 21; i++) {
            RecommenderEvaluator evaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
            RecommenderBuilder builder = new MyRecommenderBuilder(i);
            double qualityResult = 0.0;
            for (int j = 0; j < 10; j++) {
                qualityResult += evaluator.evaluate(builder, null, dataModel, 0.9, 0.1);
            }

            resultQuality.add(qualityResult / 10);
        }

        System.out.println(resultQuality);
    }

    public static class MyRecommenderBuilder implements RecommenderBuilder {

        private int neighbours;

        public MyRecommenderBuilder(int neighbours) {
            this.neighbours = neighbours;
        }

        @Override
        public Recommender buildRecommender(DataModel dataModel) throws TasteException {
            UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
            UserNeighborhood neighborhood = new NearestNUserNeighborhood(neighbours, similarity, dataModel);
            return new MyUBRec(dataModel, neighborhood, similarity);
        }
    }

    public static class MyUBRec extends GenericUserBasedRecommender {

        public MyUBRec(DataModel dataModel, UserNeighborhood neighborhood, UserSimilarity similarity) {
            super(dataModel, neighborhood, similarity);
        }

        @Override
        protected float doEstimatePreference(long theUserID, long[] theNeighborhood, long itemID) throws TasteException {
            return 7.2179f;
        }
    }

}

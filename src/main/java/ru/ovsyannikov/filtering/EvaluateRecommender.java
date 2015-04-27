package ru.ovsyannikov.filtering;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.RecommenderBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.eval.AverageAbsoluteDifferenceRecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.model.jdbc.MySQLJDBCDataModel;
import org.apache.mahout.cf.taste.impl.model.jdbc.ReloadFromJDBCDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.Recommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * @author Georgii Ovsiannikov
 * @since 3/18/15
 */
public class EvaluateRecommender {

    public static void main(String[] args) throws IOException, TasteException {
        for (int i = 0; i < 10; i++) {
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
            DataSource dataSource = context.getBean(DataSource.class);
            DataModel dataModel = new ReloadFromJDBCDataModel(new MySQLJDBCDataModel(dataSource, "votes", "user_id", "kinopoisk_id", "vote", "dt"));

            RecommenderEvaluator evaluator = new AverageAbsoluteDifferenceRecommenderEvaluator();
            RecommenderBuilder builder = new MyRecommenderBuilder();
            double qualityResult = evaluator.evaluate(builder, null, dataModel, 0.9, 0.1);
            System.out.println("quality: " + qualityResult);
        }
    }

    public static class MyRecommenderBuilder implements RecommenderBuilder {

        @Override
        public Recommender buildRecommender(DataModel dataModel) throws TasteException {
            UserSimilarity similarity = new PearsonCorrelationSimilarity(dataModel);
            UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, dataModel);
            return new GenericUserBasedRecommender(dataModel, neighborhood, similarity);
        }
    }

}

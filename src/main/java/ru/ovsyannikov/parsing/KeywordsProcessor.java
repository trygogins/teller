package ru.ovsyannikov.parsing;

import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.model.keyword.Keyword;
import com.omertron.themoviedbapi.results.ResultList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;

/**
 * @author Georgii Ovsiannikov
 * @since 5/6/15
 */
@Service
public class KeywordsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(KeywordsProcessor.class);

    @Autowired
    private TheMovieDbApi client;

    public void process() {
        try {
            ResultList<Keyword> movieKeywords = client.getMovieKeywords(157336);
            ResultList<Keyword> movieKeywords2 = client.getMovieKeywords(62);
            System.out.println(movieKeywords);
            System.out.println(movieKeywords2);
        } catch (Exception e) {
            logger.error("error", e);
        }
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        KeywordsProcessor processor = context.getBean(KeywordsProcessor.class);
        processor.process();
    }

}

package ru.ovsyannikov.parsing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;
import ru.ovsyannikov.MovieStorageHelper;
import ru.ovsyannikov.exceptions.KinopoiskForbiddenException;
import ru.ovsyannikov.parsing.model.Movie;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Georgii Ovsiannikov
 * @since 4/12/15
 */
@Service
public class MovieParser {

    private static final Logger logger = LoggerFactory.getLogger(MovieParser.class);

    @Autowired
    public MovieStorageHelper movieStorageHelper;

    /**
     * @see #parseMovie(String, boolean)
     */
    public Movie parseMovie(String url) {
        return parseMovie(url, false);
    }

    /**
     * method for parsing kinopoisk movie url to an object of type Movie
     * @param url – url to get the document
     * @param local – parameter to specify, if the document is on WEB, or a local html document
     * @return object of type Movie
     */
    public Movie parseMovie(String url, boolean local) {
        Document document = local ? getDocument(url) : downloadDocument(url);
        if (document == null) {
            return null;
        }

        try {
            Movie movie = new Movie();
            movie.fillInFields(document);
            return movie;
        } catch (IllegalArgumentException e) {
            logger.error("error parsing movie at {}", url, e);
            return null;
        }
    }

    /**
     * parsing local file to document
     */
    public Document getDocument(String localUrl) {
        try {
            return Jsoup.parse(new File(localUrl), "utf-8", "http://kinopoisk.ru");
        } catch (IOException e) {
            logger.error("unable to find local file: {}", localUrl, e);
            return null;
        }
    }

    /**
     * Establishing Jsoup connection to the given url (using human-like cookies)
     * @param url – url to request for document
     */
    public Document downloadDocument(String url) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .timeout(10000)
                    .followRedirects(true)
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2272.118 Safari/537.36")
                    .header("Cookie", "tc=5381; awfs=1; user_country=ru; noflash=false; mobile=no; mobile=no; _ym_visorc_22663942=b; yandexuid=687401641428855418; refresh_yandexuid=687401641428855418")
                    .execute();

            if (response.url().getHost().contains("error.")) {
                throw new KinopoiskForbiddenException(url);
            }

            return response.parse();
        } catch (IOException e) {
            logger.error("io - ", e);
            return null;
        }
    }

    public void process() {
        // fetch movies that are not yet processed
        List<Long> kinopoiskIds = movieStorageHelper.getUnprocessedMovies();
        ArrayBlockingQueue<Long> movieIds = new ArrayBlockingQueue<>(kinopoiskIds.size(), true, kinopoiskIds);

        int poolSize = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

        for (int i = 0; i < poolSize; i++) {
            try {
                Thread.sleep(1000 / poolSize);
                executorService.submit(() -> processMovie(movieIds));
            } catch (InterruptedException e) {
                throw new CancellationException();
            }
        }

        executorService.shutdown();
    }

    public void processMovie(BlockingQueue<Long> queue) {
        try {
            Long kinopoiskId;
            while ((kinopoiskId = queue.poll(5, TimeUnit.MINUTES)) != null) {
                MovieParser movieParser = new MovieParser();
                logger.info("movie #{} – started", kinopoiskId);
                Movie movie = movieParser.parseMovie("http://kinopoisk.ru/film/" + kinopoiskId);
                if (movie != null) {
                    movie.setKinopoiskId(kinopoiskId);
                    movieStorageHelper.saveMovie(movie);
                    logger.info("movie #{} – processed", kinopoiskId);
                }
            }
        }catch(InterruptedException e){
            logger.error("ERROR with queue", e);
        }catch(KinopoiskForbiddenException e){
            logger.error("403 by kinopoisk", e);
        }catch(IllegalStateException ignore){
        }
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        MovieParser parser = context.getBean(MovieParser.class);
        parser.process();
    }
}

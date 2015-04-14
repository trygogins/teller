package ru.ovsyannikov.parsing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.stereotype.Service;
import ru.ovsyannikov.parsing.exceptions.KinopoiskForbiddenException;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author Georgii Ovsiannikov
 * @since 4/12/15
 */
@Service
public class MovieParser {

    private static final Logger logger = LoggerFactory.getLogger(MovieParser.class);

    @Autowired
    public JdbcTemplate template;

    @Autowired
    public StorageHelper storageHelper;

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

        Movie movie = new Movie();
        movie.fillInFields(document);
        return movie;
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
        List<Long> kinopoiskIds = template.query("select kinopoisk_id from (select v.kinopoisk_id, count(*) from votes v left join movies m  on v.kinopoisk_id = m.kinopoisk_id where m.kinopoisk_id is null group by 1 order by 2 desc) t", new SingleColumnRowMapper<>(Long.class));

        MovieParser movieParser = new MovieParser();
        for (Long kinopoiskId : kinopoiskIds) {
            try {
                Movie movie = movieParser.parseMovie("http://kinopoisk.ru/film/" + kinopoiskId);
                movie.setKinopoiskId(kinopoiskId);
                storageHelper.saveMovie(movie);
                logger.info("movie #{} processed successfully", kinopoiskId);
            } catch (IllegalStateException ignore) {
            }
        }
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        MovieParser parser = context.getBean(MovieParser.class);
        parser.process();
    }
}

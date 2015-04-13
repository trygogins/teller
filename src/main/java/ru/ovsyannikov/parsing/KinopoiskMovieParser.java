package ru.ovsyannikov.parsing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ovsyannikov.parsing.exceptions.KinopoiskForbiddenException;

import java.io.File;
import java.io.IOException;

/**
 * @author Georgii Ovsiannikov
 * @since 4/12/15
 */
public class KinopoiskMovieParser {

    private static final Logger logger = LoggerFactory.getLogger(KinopoiskMovieParser.class);

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

    public static void main(String[] args) {
        KinopoiskMovieParser movieParser = new KinopoiskMovieParser();
        Movie movie = movieParser.parseMovie("/Users/georgii/Dropbox/coursework/filtering/src/main/resources/testpages/into_the_wild.html", true);
        System.out.println(movie);
    }
}

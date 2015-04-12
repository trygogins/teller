package ru.ovsyannikov.parsing;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ovsyannikov.parsing.exceptions.KinopoiskForbiddenException;

import java.io.IOException;

/**
 * @author Georgii Ovsiannikov
 * @since 4/12/15
 */
public class KinopoiskMovieParser {

    private static final Logger logger = LoggerFactory.getLogger(KinopoiskMovieParser.class);

    public Movie parseMovie(String url) {
        Document movieHtmlDocument = getDocument(url);

        System.out.println(movieHtmlDocument);
        return new Movie();
    }

    public Document getDocument(String url) {
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
        Movie movie = movieParser.parseMovie("http://www.kinopoisk.ru/film/252626/");
        System.out.println(movie);
    }
}

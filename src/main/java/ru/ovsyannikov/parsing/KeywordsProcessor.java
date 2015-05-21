package ru.ovsyannikov.parsing;

import com.omertron.themoviedbapi.MovieDbException;
import com.omertron.themoviedbapi.TheMovieDbApi;
import com.omertron.themoviedbapi.enumeration.SearchType;
import com.omertron.themoviedbapi.model.keyword.Keyword;
import com.omertron.themoviedbapi.model.movie.MovieInfo;
import com.omertron.themoviedbapi.results.ResultList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * @author Georgii Ovsiannikov
 * @since 5/6/15
 */
@Service
public class KeywordsProcessor {

    public static class MovieTitle {
        private Long movieId;
        private String title;

        public Long getMovieId() {
            return movieId;
        }

        public void setMovieId(Long movieId) {
            this.movieId = movieId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(KeywordsProcessor.class);

    @Autowired
    private TheMovieDbApi client;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void process() {
        try {
            List<MovieTitle> titles = jdbcTemplate.query("select movie_id, title from movies m " +
                    "join votes v on m.kinopoisk_id = v.kinopoisk_id " +
                    "where movie_id not in (select distinct movie_id from movie_keywords) " +
                    "group by movie_id " +
                    "order by count(*) desc", new BeanPropertyRowMapper<>(MovieTitle.class));
            for (MovieTitle title : titles) {
                int movieId = searchMovie(title.getTitle());
                Thread.sleep(300);
                if (movieId > 0) {
                    ResultList<Keyword> movieKeywords = client.getMovieKeywords(movieId);
                    GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();

                    for (Keyword keyword : movieKeywords.getResults()) {
                        Long keywordId;
                        try {
                            jdbcTemplate.update(con -> {
                                PreparedStatement statement = con.prepareStatement("insert into keywords(name) values (?)", Statement.RETURN_GENERATED_KEYS);
                                statement.setString(1, keyword.getName());
                                return statement;
                            }, keyHolder);
                            keywordId = keyHolder.getKey().longValue();
                        } catch (Exception e) {
                            keywordId = jdbcTemplate.queryForObject("select keyword_id from keywords where name = ?", Long.class, keyword.getName());
                        }

                        jdbcTemplate.update("replace into movie_keywords values(?, ?)", title.getMovieId(), keywordId);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("error", e);
        }
    }

    private int searchMovie(String title) throws MovieDbException {
        try {
            ResultList<MovieInfo> foundMovies = client.searchMovie(title, 0, "ru", true, 0, null, SearchType.PHRASE);
            for (MovieInfo movieInfo : foundMovies.getResults()) {
                if (movieInfo.getTitle().equals(title)) {
                    return movieInfo.getId();
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        KeywordsProcessor processor = context.getBean(KeywordsProcessor.class);
        processor.process();
    }

}

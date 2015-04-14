package ru.ovsyannikov.parsing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.PreparedStatement;
import java.sql.Statement;

/**
 * @author Georgii Ovsiannikov
 * @since 4/13/15
 */
@Service
public class StorageHelper {

    private static final Logger logger = LoggerFactory.getLogger(StorageHelper.class);

    @Autowired
    public JdbcTemplate template;

    @Autowired
    public TransactionTemplate transactionTemplate;

    public Boolean saveMovie(Movie movie) {
        return transactionTemplate.execute(transactionStatus -> {
                // saving the movie
                KeyHolder keyHolder = new GeneratedKeyHolder();
                template.update(con -> {
                    PreparedStatement statement = con.prepareStatement("insert into movies(kinopoisk_id, title, year, director) " +
                            "values (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, movie.getKinopoiskId());
                    statement.setString(2, movie.getTitle());
                    statement.setInt(3, movie.getYear());
                    statement.setString(4, movie.getDirector());

                    return statement;
                }, keyHolder);

                Long movieId = keyHolder.getKey().longValue();

                // saving actors
                for (String actor : movie.getActors()) {
                    Long actorId;
                    try {
                        template.update(con -> {
                            PreparedStatement statement = con.prepareStatement("insert into actors(name) values (?)", Statement.RETURN_GENERATED_KEYS);
                            statement.setString(1, actor);
                            return statement;
                        }, keyHolder);
                        actorId = keyHolder.getKey().longValue();
                    } catch (Exception e) {
                        actorId = template.queryForObject("select actor_id from actors where name = ?", Long.class, actor);
                    }

                    template.update("replace into movie_actors values (?, ?)", movieId, actorId);
                }

                // saving genres
                for (String genre : movie.getGenres()) {
                    Long genreId;
                    try {
                        template.update(con -> {
                            PreparedStatement statement = con.prepareStatement("insert into genres(genre) values (?)", Statement.RETURN_GENERATED_KEYS);
                            statement.setString(1, genre);
                            return statement;
                        }, keyHolder);
                        genreId = keyHolder.getKey().longValue();
                    } catch (Exception e) {
                        genreId = template.queryForObject("select genre_id from genres where genre = ?", Long.class, genre);
                    }

                    template.update("replace into movie_genres values (?, ?)", movieId, genreId);
                }

                return true;
        });
    }

}

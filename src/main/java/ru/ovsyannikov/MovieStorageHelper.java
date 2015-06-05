package ru.ovsyannikov;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.ovsyannikov.parsing.model.Movie;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Georgii Ovsiannikov
 * @since 4/13/15
 */
@Service
public class MovieStorageHelper {

    public static final ResultSetExtractor<List<Movie>> MOVIE_EXTRACTOR = resultSet -> {
        List<Movie> result = new ArrayList<>();
        while (resultSet.next()) {
            Movie movie = new Movie();
            movie.setId(resultSet.getLong("movie_id"));
            movie.setKinopoiskId(resultSet.getLong("kinopoisk_id"));
            movie.setTitle(resultSet.getString("title"));
            movie.setYear(resultSet.getInt("year"));
            movie.setDirector(resultSet.getString("director"));
            movie.setActors(Arrays.asList(StringUtils.split(resultSet.getString("actors"), ",")));
            movie.setGenres(Arrays.asList(StringUtils.split(resultSet.getString("genres"), ",")));
//            movie.setKeywords(Arrays.asList(StringUtils.split(resultSet.getString("keywords"), ",")));

            List<String> votes = Arrays.asList(StringUtils.split(resultSet.getString("user_votes"), ","));
            movie.setVotes(votes.stream()
                    .map(v -> new Movie.UserVote(NumberUtils.createLong(v.split("-")[0]),
                            NumberUtils.createInteger(v.split("-")[1])))
                    .collect(Collectors.toList()));

            result.add(movie);
        }

        return result;
    };

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

    public List<Long> getUnprocessedMovies() {
        return template.query("select kinopoisk_id from " +
                "(select v.kinopoisk_id, count(*) " +
                "from votes v left " +
                "join movies m  on v.kinopoisk_id = m.kinopoisk_id " +
                "where m.kinopoisk_id is null " +
                "group by 1 order by 2 desc) t", new SingleColumnRowMapper<>(Long.class));
    }

    public Movie getMovie(Long movieId) {
        List<Movie> movies = getMovies(Arrays.asList(movieId));
        if (movies.isEmpty()) {
            throw new IllegalArgumentException("No movie with specified id=" + movieId);
        }

        return movies.get(0);
    }

    public List<Movie> getVotedMovies(Long userId) {
        return getMoviesByKinopoiskId(template.query("select distinct kinopoisk_id from votes2 where user_id = ?",
                new SingleColumnRowMapper<>(Long.class), userId));
    }

    public int setMovieVote(Long userId, Long kinopoiskId, Integer vote) {
        return template.update("replace into votes2 values (?,?,now(),?)", userId, kinopoiskId, vote);
    }

    public List<Movie> getMoviesToVote(Long userId) {
        return getMoviesByKinopoiskId(template.query("select dispute.kinopoisk_id from " +
                "(select kinopoisk_id from votes group by 1 having count(*) > 20 order by variance(vote) * sqrt(count(*)) desc) dispute " +
                "left join " +
                "(select distinct kinopoisk_id from votes where user_id = ?) watched " +
                "on dispute.kinopoisk_id = watched.kinopoisk_id " +
                "where watched.kinopoisk_id is null limit 20", new SingleColumnRowMapper<>(Long.class), userId));
    }

    public List<Movie> getMovies(String tableName) {
        return getMoviesByKinopoiskId(template.query("select distinct kinopoisk_id from " + tableName, new SingleColumnRowMapper<>(Long.class)));
    }

    public List<Movie> getMovies(List<Long> movieIds) {
        return getMovies(movieIds, "movie_id");
    }

    public List<Movie> getMoviesByKinopoiskId(List<Long> kinopoiskIds) {
        return getMovies(kinopoiskIds, "kinopoisk_id");
    }

    private List<Movie> getMovies(List<Long> ids, String columnName) {
        if (CollectionUtils.isEmpty(ids)) {
            return new ArrayList<>();
        }

        return template.query("select m.*, group_concat(distinct a.name) as actors, group_concat(distinct g.genre) as genres, " +
                "group_concat(distinct v.user_id, '-', v.vote) as user_votes " +
                "from movies m " +
                "join movie_actors ma on m.movie_id = ma.movie_id join actors a on ma.actor_id = a.actor_id " +
                "join movie_genres mg on m.movie_id = mg.movie_id join genres g on mg.genre_id = g.genre_id " +
                "join votes2 v on m.kinopoisk_id = v.kinopoisk_id " +
                "where m." + columnName + " in (" + StringUtils.join(ids, ",") + ") " +
                "group by m.movie_id", MOVIE_EXTRACTOR);
    }

    public void deleteMark(Long userId, Long kinopoiskId) {
        template.update("delete from votes2 where user_id = ? and kinopoisk_id = ?", userId, kinopoiskId);
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        MovieStorageHelper helper = context.getBean(MovieStorageHelper.class);
        List<Movie> movie = helper.getMovies(Arrays.asList(2l, 5l));
        System.out.println(movie);
    }
}

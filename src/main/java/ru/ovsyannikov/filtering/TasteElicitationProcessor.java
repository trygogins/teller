package ru.ovsyannikov.filtering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import ru.ovsyannikov.MovieStorageHelper;
import ru.ovsyannikov.clustering.KMeansProcessor;
import ru.ovsyannikov.clustering.model.ClusterCenter;
import ru.ovsyannikov.collaborative.MovieMarkForecaster;
import ru.ovsyannikov.collaborative.UserNeighboursProcessor;
import ru.ovsyannikov.parsing.model.Movie;

import javax.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author Georgii Ovsiannikov
 * @since 4/22/15
 */
@Service
public class TasteElicitationProcessor {

    public static final Integer MINIMAL_VOTES = 20;
    private static final Logger logger = LoggerFactory.getLogger(TasteElicitationProcessor.class);

    @Autowired
    private MovieStorageHelper movieStorageHelper;
    @Autowired
    private JdbcTemplate template;

    private List<List<Movie>> movieClusters;
    // user_id -> list of marks
    private Map<Long, List<UserNeighboursProcessor.UserVote>> votesByUser;

    @PostConstruct
    public void init() {
        KMeansProcessor clusteringProcessor = new KMeansProcessor();
        String tableName = "votes2";
        List<Movie> movies = movieStorageHelper.getMovies(tableName);
        ConcurrentMap<ClusterCenter, List<Movie>> clusters = new ConcurrentHashMap<>(); //clusteringProcessor.doClustering(movies);
        movieClusters = getClustersLazy(movies);

//                clusters.keySet().stream()
//                        .map(clusters::get)
//                        .collect(Collectors.toList());

        List<UserNeighboursProcessor.UserVote> votes = template.query("select * from " + tableName, new BeanPropertyRowMapper<>(UserNeighboursProcessor.UserVote.class));
        votesByUser = new HashMap<>();
        for (UserNeighboursProcessor.UserVote userVote : votes) {
            List<UserNeighboursProcessor.UserVote> userVotes = votesByUser.get(userVote.getUserId());
            if (userVotes == null) {
                userVotes = new ArrayList<>();
                votesByUser.put(userVote.getUserId(), userVotes);
            }
            userVotes.add(userVote);
        }
    }

    public List<List<Movie>> getClustersLazy(List<Movie> movies) {
        Map<Long, Integer> clusters = template.query("select * from movie_clusters", new ResultSetExtractor<Map<Long, Integer>>() {
            @Override
            public Map<Long, Integer> extractData(ResultSet resultSet) throws SQLException, DataAccessException {
                Map<Long, Integer> result = new HashMap<>();
                while (resultSet.next()) {
                    result.put(resultSet.getLong("movie_id"), resultSet.getInt("cluster"));
                }

                return result;
            }
        });

        Map<Integer, List<Movie>> clusteredMovies = new HashMap<>();
        for (Movie movie : movies) {
            Integer clusterId = clusters.get(movie.getId());
            List<Movie> moviesInCluster = clusteredMovies.get(clusterId);
            if (moviesInCluster == null) {
                moviesInCluster = new ArrayList<>();
                clusteredMovies.put(clusterId, moviesInCluster);
            }

            moviesInCluster.add(movie);
        }

        return clusteredMovies.keySet().stream()
                .map(clusteredMovies::get)
                .collect(Collectors.toList());
    }

    /**
     * Метод возвращает мапу не просмотренных, но рекомендуемых к просмотру пользователем
     * @param userId текущий пользователь
     * @return map: key – kinopoisk_id, value – прогнозируемая оценка
     */
    public Map<Long, Double> getRecommendedMovies(Long userId) {
        List<Movie> watchedMovies = movieClusters.stream()
                .flatMap(Collection::stream)
                .filter(movie -> movie.getVotes().stream()
                        .filter(uv -> Objects.equals(uv.getUserId(), userId))
                        .count() > 0)
                .collect(Collectors.toList());

        // кластеры, фильмы из которых пользователь видел
        List<List<Movie>> watchedClusters = movieClusters.stream()
                .filter(m -> intersection(m, watchedMovies).size() > 0)
                .collect(Collectors.toList());

        List<Long> movieIdsInWatchedClusters = watchedClusters.stream()
                .flatMap(Collection::stream)
                .map(Movie::getKinopoiskId)
                .collect(Collectors.toList());

        Map<Long, List<UserNeighboursProcessor.UserVote>> localVotesByUser = new HashMap<>();
        for (Long uId : votesByUser.keySet()) {
            List<UserNeighboursProcessor.UserVote> userVotes = votesByUser.get(uId);
            localVotesByUser.put(uId, userVotes.stream()
                    .filter(m -> movieIdsInWatchedClusters.contains(m.getKinopoiskId()))
                    .collect(Collectors.toList()));
        }

        UserNeighboursProcessor neighboursProcessor = new UserNeighboursProcessor(localVotesByUser);
        Map<Long, Double> userNeighbours = neighboursProcessor.getUserNeighbours(userId, 5);
        MovieMarkForecaster markForecaster = new MovieMarkForecaster(localVotesByUser);

        return markForecaster.forecastMarks(5, userId, userNeighbours);
    }

    /**
     * Метод возвращает список кластеров, фильмы из которых пользователь ещё не посмотрел
     * @param userId текущий пользователь
     * @return список списков фильмов, отсортированный по убыванию размера кластера
     */
    public List<List<Movie>> getMoviesToVote(Long userId) {
        movieClusters.sort((l1, l2) -> l1.size() > l2.size() ? -1 : l1.size() < l2.size() ? 1 : 0);
        List<Movie> watchedMovies = movieClusters.stream()
                .flatMap(Collection::stream)
                .filter(movie -> movie.getVotes().stream()
                        .filter(uv -> Objects.equals(uv.getUserId(), userId))
                        .count() > 0)
                .collect(Collectors.toList());

        return movieClusters.stream()
                .filter(m -> intersection(m, watchedMovies).size() == 0)
                .collect(Collectors.toList());
    }

    private List<Movie> intersection(List<Movie> movies1, List<Movie> movies2) {
        return movies1.stream()
                .filter(movies2::contains)
                .collect(Collectors.toList());
    }

    public List<List<Movie>> getMovieClusters() {
        return movieClusters;
    }

    public Map<Long, List<UserNeighboursProcessor.UserVote>> getVotesByUser() {
        return votesByUser;
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        TasteElicitationProcessor elicitationProcessor = context.getBean(TasteElicitationProcessor.class);
        Map<Long, Double> recommendedMovies = elicitationProcessor.getRecommendedMovies(1497l);
        System.out.println(recommendedMovies);
    }
}

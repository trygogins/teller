package ru.ovsyannikov.clustering;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.hadoop.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.ovsyannikov.MovieStorageHelper;
import ru.ovsyannikov.clustering.model.*;
import ru.ovsyannikov.elicitation.SplitterDeterminingProcessor;
import ru.ovsyannikov.parsing.model.Movie;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The class contains an implementation of k-means clustering algorithm
 *
 * @author Georgii Ovsiannikov
 * @since 5/6/15
 */
public class KMeansProcessor {

    private static final Logger logger = LoggerFactory.getLogger(KMeansProcessor.class);

    private Random random = new Random();
    private NonDuplicateDataSet moviesSet;
    private List<Movie> movies;
    private Map<DistanceKey, Double> distances = new HashMap<>();

    public KMeansProcessor(List<Movie> movies, JdbcTemplate template) {
        this(movies, template, false);
    }

    public KMeansProcessor(List<Movie> movies, JdbcTemplate template, boolean recalculateDistances) {
        distances = new HashMap<>();
        Multimap<String, DistanceInfo<String>> dist = getStringDistanceInfoMultimap(movies, template, recalculateDistances);
        for (String key : dist.keySet()) {
            for (DistanceInfo<String> info : dist.get(key)) {
                distances.put(new DistanceKey(key, info.getCollection1(), info.getCollection2()), info.getDistance());
                distances.put(new DistanceKey(key, info.getCollection2(), info.getCollection1()), info.getDistance());
            }
        }

        this.moviesSet = new NonDuplicateDataSet(new DataSet(movies));
        this.movies = movies;
    }

    private Multimap<String, DistanceInfo<String>> getStringDistanceInfoMultimap(List<Movie> movies, JdbcTemplate template, boolean recalculateDistances) {
        Multimap<String, DistanceInfo<String>> dist;
        CategoricalDistanceProcessor distanceProcessor = new CategoricalDistanceProcessor();
        if (!recalculateDistances) {
            dist = HashMultimap.create();
            List<DistanceInfo<String>> infoList = template.query("select * from distances", new BeanPropertyRowMapper(DistanceInfo.class));
            for (DistanceInfo<String> info : infoList) {
                dist.put(info.getType(), info);
            }
        } else {
            dist = distanceProcessor.calculateAttributesDistances(new DataSet(movies));
            saveDistances(template, dist);
        }
        return dist;
    }

    private void saveDistances(JdbcTemplate template, Multimap<String, DistanceInfo<String>> distances) {
        for (String type : distances.keySet()) {
            StringBuilder sb = new StringBuilder("replace into distances values ");
            for (DistanceInfo<String> distanceInfo : distances.get(type)) {
                sb.append("('").append(type).append("','").append(StringUtils.join(",", distanceInfo.getCollection1())).append("','")
                        .append(StringUtils.join(",", distanceInfo.getCollection2())).append("',")
                        .append(distanceInfo.getDistance()).append(", now()),");
            }

            sb.setLength(sb.length() - 1);
            if (!sb.toString().endsWith("values")) {
                template.update(sb.toString());
            }
        }
    }

    public ConcurrentMap<ClusterCenter, List<Movie>> performClustering(List<Movie> movies, int numClusters) {
        ConcurrentMap<ClusterCenter, List<Movie>> clusters = new ConcurrentHashMap<>();
        ConcurrentMap<ClusterCenter, List<Movie>> previousClusters = new ConcurrentHashMap<>();
        int iterations = 0;

        // randomly given clusters
//        for (int i = 0; i < numClusters; i++) {
//            clusters.put(new ClusterCenter(Arrays.asList(movies.get(random.nextInt(movies.size())))),
//                    Collections.synchronizedList(new ArrayList<>()));
//        }

        List<Movie> furthestMovies = getFurthestMovies(movies, numClusters);
        for (Movie movie : furthestMovies) {
            clusters.put(new ClusterCenter(Arrays.asList(movie)), Collections.synchronizedList(new ArrayList<>()));
        }

//        List<List<Movie>> randomLists = new ArrayList<>();
//        for (int i = 0; i < numClusters; i++) {
//            randomLists.add(Collections.synchronizedList(new ArrayList<>()));
//        }
//        for (Movie movie : movies) {
//            randomLists.get(random.nextInt(numClusters)).add(movie);
//        }
//        for (List<Movie> movieList : randomLists) {
//            clusters.put(new ClusterCenter(movieList), Collections.synchronizedList(new ArrayList<>()));
//        }

        while (true) {
            iterations ++;
            AtomicInteger processed = new AtomicInteger(0);
            ExecutorService executorService = Executors.newFixedThreadPool(32);
            for (Movie movie : movies) {
                executorService.submit(() -> {
                    double minDistance = Double.MAX_VALUE;
                    ClusterCenter minDistanceCenter = null;
                    for (ClusterCenter clusterCenter : clusters.keySet()) {
                        double distance = distance(movie, clusterCenter);
                        if (distance < minDistance) {
                            minDistance = distance;
                            minDistanceCenter = clusterCenter;
                        }
                    }

                    clusters.getOrDefault(minDistanceCenter, Collections.synchronizedList(new ArrayList<>())).add(movie);
                    logger.info("processed={}%", (double) processed.incrementAndGet() * 100 / movies.size());
                });
            }

            executorService.shutdown();
            try {
                while (!executorService.awaitTermination(24L, TimeUnit.HOURS)) {
                    System.out.println("Still waiting for the executor to finish");
                }
            } catch (InterruptedException e) {
                logger.error("ERROR", e);
            }

            if (ComparisonUtils.isEqual(clusters, previousClusters)) {
                break;
            }

            previousClusters.clear();
            for (ClusterCenter clusterCenter : clusters.keySet()) {
                previousClusters.put(clusterCenter, clusters.get(clusterCenter));
            }

            clusters.clear();
            for (ClusterCenter clusterCenter : previousClusters.keySet()) {
                clusters.put(new ClusterCenter(previousClusters.get(clusterCenter)), Collections.synchronizedList(new ArrayList<>()));
            }
        }

        logger.info("{} iterations for {} movies and {} clusters", iterations, movies.size(), numClusters);
        return clusters;
    }

    private List<Movie> getFurthestMovies(List<Movie> movies, int requiredMovies) {
        List<Movie> result = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie.getGenres().contains("новости")) {
                result.add(movie);
                break;
            }
        }

        if (result.isEmpty()) {
            result.add(movies.get(random.nextInt(movies.size())));
        }

        while (result.size() < requiredMovies) {
            Movie maxDistanceMovie = null;
            double maxDistance = 0;
            for (Movie movie : movies) {
                result.add(movie);
                double distance = getAverageDistance(result);
                if (distance > maxDistance) {
                    maxDistanceMovie = movie;
                    maxDistance = distance;
                }

                result.remove(movie);
            }

            result.add(maxDistanceMovie);
        }

        return result;
    }

    private double getAverageDistance(List<Movie> movies) {
        int count = 0;
        double distance = 0;
        for (int i = 0; i < movies.size(); i++) {
            for (int j = 0; j < i; j++) {
                count ++;
                distance += distance(movies.get(i), movies.get(j));
            }
        }

        return distance / count;
    }


    public double distance(Movie movie1, Movie movie2) {
        return distance(movie1, new ClusterCenter(Arrays.asList(movie2)));
    }
    /**
     * Calculates distance between a movie and a cluster center using `Cluster centers for mixed data sets`
     */
    public double distance(Movie movie, ClusterCenter center) {
        if (center.getNc() == 0) {
            return Double.MAX_VALUE;
        }

        double dist = 0.0;
        for (List<String> genresList : center.getGenres().keySet()) {
            double genresDistance = (double) center.getGenres().get(genresList) / center.getNc() *
                    (1 - ComparisonUtils.getListsSimilarity(genresList, movie.getGenres()));
            dist += genresDistance * genresDistance;
        }
        dist *= 10;

        for (List<String> actorsList : center.getActors().keySet()) {
            double actorsDistance = (double) center.getActors().get(actorsList) / center.getNc() *
                    (1 - ComparisonUtils.getListsSimilarity(actorsList, movie.getActors()));
            dist += actorsDistance * actorsDistance;
        }
        for (List<String> directorsList : center.getDirectors().keySet()) {
            double directorsDistance = (double) center.getDirectors().get(directorsList) / center.getNc() *
                    (1 - ComparisonUtils.getListsSimilarity(directorsList, Arrays.asList(movie.getDirector())));
            dist += directorsDistance * directorsDistance;
        }
//        for (List<String> keywordsList : center.getKeywords().keySet()) {
//            double keywordsDistance = (double) center.getKeywords().get(keywordsList) / center.getNc() *
//                    (1 - ComparisonUtils.getListsSimilarity(keywordsList, movie.getKeywords()));
//            dist += keywordsDistance * keywordsDistance;
//        }

        return dist * Math.sqrt(center.getNc());
//        for (List<String> genres : moviesSet.getGenres()) {
//            double genresDistance = (double) center.getGenres().getOrDefault(genres, 0) / center.getNc() *
//                    distances.getOrDefault(new DistanceKey("genres", movie.getGenres(), genres), 0.0);
//            dist += genresDistance * genresDistance;
//        }
//        for (List<String> actors : moviesSet.getActors()) {
//            double actorsDistance = (double) center.getActors().getOrDefault(actors, 0) / center.getNc() *
//                    distances.getOrDefault(new DistanceKey("actors", movie.getActors(), actors), 0.0);
//            dist += actorsDistance * actorsDistance;
//        }
//        for (List<String> directors : moviesSet.getDirectors()) {
//            double directorsDistance = (double) center.getDirectors().getOrDefault(directors, 0) / center.getNc() *
//                    distances.getOrDefault(new DistanceKey("directors", Arrays.asList(movie.getDirector()), directors), 0.0);
//            dist += directorsDistance * directorsDistance;
//        }
//        for (List<String> keywords : moviesSet.getKeywords()) {
//            double keywordsDistance = (double) center.getKeywords().getOrDefault(keywords, 0) / center.getNc() *
//                    distances.getOrDefault(new DistanceKey("keywords", movie.getKeywords(), keywords), 0.0);
//            dist += keywordsDistance * keywordsDistance;
//        }
//
//        return dist;
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        MovieStorageHelper storageHelper = context.getBean(MovieStorageHelper.class);
        JdbcTemplate template = context.getBean(JdbcTemplate.class);
        List<Movie> movies = storageHelper.getMovies("votes5");
        SplitterDeterminingProcessor splitterProcessor = new SplitterDeterminingProcessor();

        KMeansProcessor processor = new KMeansProcessor(movies, template);

        // требуемое количество кластеров
        int targetClustersCount = (int) Math.sqrt(movies.size() / 2);
        // примерный размер кластера (чтобы не было кластеров размером 1 и т. п.)
        int minimalClusterSize = (int) (0.3 * movies.size() / targetClustersCount);
        ConcurrentMap<ClusterCenter, List<Movie>> clusteredMovies = processor.performClustering(movies, targetClustersCount);

        // пока не разделим на достаточное количество кластеров
        while (targetClustersCount > clusteredMovies.size()) {
            List<Movie> largestList = Collections.synchronizedList(new ArrayList<>());
            ClusterCenter largestCenter = null;
            List<ClusterCenter> centersToRemove = Collections.synchronizedList(new ArrayList<>());

            for (ClusterCenter clusterCenter : clusteredMovies.keySet()) {
                List<Movie> current = clusteredMovies.get(clusterCenter);
                if (largestList.size() < current.size()) {
                    largestList = current;
                    largestCenter = clusterCenter;
                }

                if (current.size() < minimalClusterSize) {
                    centersToRemove.add(clusterCenter);
                }
            }
            // выпиливаем самый жирный кластер – он, как правило, огромный и состоит из драм
            clusteredMovies.remove(largestCenter);
            // выпиливаем маленькие кластеры и добавляем их элементы для кластеризации
            final List<Movie> finalLargestList = largestList;
//            centersToRemove.forEach(c -> finalLargestList.addAll(clusteredMovies.remove(c)));
            clusteredMovies.putAll(processor.performClustering(finalLargestList, targetClustersCount - clusteredMovies.keySet().size()));
        }

        for (ClusterCenter center : clusteredMovies.keySet()) {
            Movie splitter = splitterProcessor.getSplitter(clusteredMovies.get(center));
            System.out.println(splitter);
        }
    }
}

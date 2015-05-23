package ru.ovsyannikov.clustering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.ovsyannikov.MovieStorageHelper;
import ru.ovsyannikov.clustering.model.ClusterCenter;
import ru.ovsyannikov.parsing.model.Movie;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * The class contains an implementation of k-means clustering algorithm
 *
 * @author Georgii Ovsiannikov
 * @since 5/6/15
 */
public class KMeansProcessor {

    private static final Logger logger = LoggerFactory.getLogger(KMeansProcessor.class);

    private Random random = new Random();

    /**
     * Метод для кластеризации фильмов по содержимому.
     * Количество кластеров определяется как sqrt(n/2), где n – количество фильмов.
     * Как правило, на первых этапах кластеризации выбирается несколько настоящих кластеров,
     * а фильмы с популярным жанром `драма` остаются в самом большом кластере.
     * Следующими итерациями самый большой кластер разделяется на более маленькие (итерация – метод {@link #performClustering}).
     * Когда разделение перестаёт давать результаты (5 раз было получено одинаковое количество кластеров) – кластеризация заканчивается.
     *
     * @param movies – список фильмов для разделения
     * @return таблица, в которой ключ – центр кластера (взвешенный), значение – список фильмов
     */
    public ConcurrentMap<ClusterCenter, List<Movie>> doClustering(List<Movie> movies) {
        int targetClustersCount = (int) Math.sqrt(movies.size() / 2); // сколько кластеров в идеале должно получиться
        int minimumClusterSize = 5; // если кластер слишком маленький – возможно, неудачно кластеризовался (5 - эмпирическое значение)
        int maximumClusterSize = movies.size() / targetClustersCount * 4; // если кластер слишком большой – нужно делить
        int previousClusters = 0;
        int stableClusters = 0;
        ConcurrentMap<ClusterCenter, List<Movie>> clusteredMovies = performClustering(movies, targetClustersCount); // initial итерация
        while (true) {
            List<ClusterCenter> badCenters = Collections.synchronizedList(new ArrayList<>());
            for (ClusterCenter clusterCenter : clusteredMovies.keySet()) {
                List<Movie> current = clusteredMovies.get(clusterCenter);
                // если кластер не удовлетворяет заданным размерам - добавляем в `плохие` (они потом перекластеризуются)
                if (current.size() > maximumClusterSize || current.size() < minimumClusterSize) {
                    badCenters.add(clusterCenter);
                }
            }

            // проверяется условие останова: если полученодостаточно кластеров
            // либо при повторной кластеризации 5 раз не было изменений
            if (clusteredMovies.size() - badCenters.size() >= targetClustersCount * 0.9 // 90% – т.к. никто не идеален :)
                    || stableClusters >= 5) {
                logger.info(stableClusters >= 5 ? "ending clustering due to stable clusters" :
                        "ending clustering due to enough clusters acquired");
                break;
            }

            // проверка наличия изменений в количестве кластеров
            if (previousClusters == clusteredMovies.size() - badCenters.size()) {
                stableClusters ++;
            } else {
                previousClusters = clusteredMovies.size() - badCenters.size();
            }

            // вытаскиваются фильмы для перекластеризации
            List<Movie> moviesToRecompute = clusteredMovies.keySet().stream()
                    .map(clusteredMovies::get)
                    .filter(c -> minimumClusterSize > c.size() || maximumClusterSize < c.size())
                    .flatMap(List::stream)
                    .collect(Collectors.toList());
            // удаляются плохие кластеры
            badCenters.forEach(clusteredMovies::remove);
            // перекластеризуем
            clusteredMovies.putAll(performClustering(moviesToRecompute, targetClustersCount - clusteredMovies.size()));
        }

        clusteredMovies.remove(new ClusterCenter(Collections.emptyList()));
        return clusteredMovies;
    }

    /**
     * Метод выполняет стандартную k-means кластеризацию фильмов.
     * Начальные центры – наиболее удалённые друг от друга фильмы.
     * Критерий останова - на новой итерации не произошло изменений.
     *
     * @param movies – список фильмов для кластеризации
     * @param numClusters – требуемое количество кластеров
     * @return таблица, в которой ключ – центр кластера (взвешенный), значение – список фильмов
     */
    public ConcurrentMap<ClusterCenter, List<Movie>> performClustering(List<Movie> movies, int numClusters) {
        ConcurrentMap<ClusterCenter, List<Movie>> clusters = new ConcurrentHashMap<>();
        ConcurrentMap<ClusterCenter, List<Movie>> previousClusters = new ConcurrentHashMap<>();
        int iterations = 0;

        // наиболее удалённые фильмы выбираются как начальные центры кластеров
        List<Movie> furthestMovies = getFurthestMovies(movies, numClusters);
        for (Movie movie : furthestMovies) {
            clusters.put(new ClusterCenter(Arrays.asList(movie)), Collections.synchronizedList(new ArrayList<>()));
        }

        while (true) {
            iterations ++;
            // ближайший кластер считается в 32 потока, т.к. иначе по списку movies (длиной порядка нескольких тысяч) долго идти
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

            // проверка условия останова
            if (ComparisonUtils.isEqual(clusters, previousClusters)) {
                break;
            }

            // дальше сохранение результатов итерации в previousClusters
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

    /**
     * из коллекции выбирается заданное количество наиболее удалённых друг от друга (по содержимому) фильмов.
     *
     * @param movies – коллекция фильмов для выбора наиболее удалённых
     * @param requiredMovies – требуемое количество фильмов
     * @return набор (Set) наиболее удалённых друг от друга фильмов
     */
    private List<Movie> getFurthestMovies(List<Movie> movies, int requiredMovies) {
        Set<Movie> result = new HashSet<>();
        for (Movie movie : movies) {
            if (movie.getGenres().contains("мультфильм")) {
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
                if (!result.contains(movie)) {
                    result.add(movie);
                    // выбирается фильм, при добавлении которого в набор достигается максимальное среднее расстояние между парами
                    double distance = getAverageDistance(new ArrayList<>(result));
                    if (distance > maxDistance) {
                        maxDistanceMovie = movie;
                        maxDistance = distance;
                    }

                    result.remove(movie);
                }
            }

            result.add(maxDistanceMovie);
        }

        return new ArrayList<>(result);
    }

    /**
     * Подсчёт среднего расстояния между парами фильмов в списке
     * @param movies – список фильмов
     * @return – сумма расстояний между всеми парами, деленая на количество пар фильмов
     */
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
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        MovieStorageHelper storageHelper = context.getBean(MovieStorageHelper.class);
        List<Movie> movies = storageHelper.getMovies("votes5");

        KMeansProcessor processor = new KMeansProcessor();
        ConcurrentMap<ClusterCenter, List<Movie>> result = processor.doClustering(movies);
        System.out.println(result);
    }
}

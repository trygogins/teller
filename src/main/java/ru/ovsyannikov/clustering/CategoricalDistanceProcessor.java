package ru.ovsyannikov.clustering;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ovsyannikov.clustering.model.DataSet;
import ru.ovsyannikov.clustering.model.DistanceInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class for calculating distances between categorical attributes of movies
 * @see 'http://edu.cs.uni-magdeburg.de/EC/lehre/sommersemester-2013/wissenschaftliches-schreiben-in-der-informatik/publikationen-fuer-studentische-vortraege/kMeansMixedCatNum.pdf'
 *
 * @author Georgii Ovsiannikov
 * @since 5/11/15
 */
public class CategoricalDistanceProcessor {

    private static final Logger logger = LoggerFactory.getLogger(CategoricalDistanceProcessor.class);

    /**
     * Считает расстояние между всеми парами значений всех атрибутов
     */
    // TODO: refactor – provide movies attributes as a map
    public Multimap<String, DistanceInfo<String>> calculateAttributesDistances(DataSet dataSet) {
        Multimap<String, DistanceInfo<String>> result = Multimaps.synchronizedSetMultimap(HashMultimap.create());
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        List<Future> futures = new ArrayList<>();
        // actors
        AtomicInteger actorsCount = new AtomicInteger(0);
        for (int i = 0; i < dataSet.getActors().size(); i++) {
            final int fi = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < fi; j++) {
                    List<String> actors0 = dataSet.getActors().get(fi);
                    List<String> actors1 = dataSet.getActors().get(j);
                    double sum = 0;
                    if (ComparisonUtils.compare(actors0, actors1)) {
                        DistanceInfo<String> info = new DistanceInfo<>(actors0, actors1, "actors", 0);
                        result.put("actors", info);
                        continue;
                    }

                    // actors-genres
                    sum += findMax(dataSet.getActors(), dataSet.getGenres(), actors0, actors1);
                    // actors-directors
                    sum += findMax(dataSet.getActors(), dataSet.getDirectors(), actors0, actors1);
                    // actors-keywords
                    sum += findMax(dataSet.getActors(), dataSet.getKeywords(), actors0, actors1);
                    DistanceInfo<String> info = new DistanceInfo<>(actors0, actors1, "actors", sum / 3);
                    result.put("actors", info);
                    System.out.println("actors: " + actorsCount.incrementAndGet());
                }
            }));
        }
        // genres
        AtomicInteger genresCount = new AtomicInteger(0);
        for (int i = 0; i < dataSet.getGenres().size(); i++) {
            final int fi = i;
            futures.add(executorService.submit(() -> {
                try {
                    for (int j = 0; j < fi; j++) {
                        List<String> genres0 = dataSet.getGenres().get(fi);
                        List<String> genres1 = dataSet.getGenres().get(j);
                        double sum = 0;
                        if (ComparisonUtils.compare(genres0, genres1)) {
                            DistanceInfo<String> info = new DistanceInfo<>(genres0, genres1, "genres", 0);
                            result.put("genres", info);
                            continue;
                        }

                        // genres-directors
                        sum += findMax(dataSet.getGenres(), dataSet.getDirectors(), genres0, genres1);
                        // genres-keywords
                        sum += findMax(dataSet.getGenres(), dataSet.getKeywords(), genres0, genres1);
                        // genres-actors
                        sum += findMax(dataSet.getGenres(), dataSet.getActors(), genres0, genres1);
                        DistanceInfo<String> info = new DistanceInfo<>(genres0, genres1, "genres", sum / 3);
                        result.put("genres", info);
                        System.out.println("genres: " + genresCount.incrementAndGet());
                    }
                } catch (Throwable e) {
                    logger.error("ERROR", e);
                }
            }));
        }
        // directors
        AtomicInteger directorsCount = new AtomicInteger(0);
        for (int i = 0; i < dataSet.getDirectors().size(); i++) {
            final int fi = i;
            futures.add(executorService.submit(() -> {
                try {
                    for (int j = 0; j < fi; j++) {
                        List<String> directors0 = dataSet.getDirectors().get(fi);
                        List<String> directors1 = dataSet.getDirectors().get(j);
                        double sum = 0;
                        if (ComparisonUtils.compare(directors0, directors1)) {
                            DistanceInfo<String> info = new DistanceInfo<>(directors0, directors1, "directors", 0);
                            result.put("directors", info);
                            continue;
                        }

                        // directors-keywords
                        sum += findMax(dataSet.getDirectors(), dataSet.getKeywords(), directors0, directors1);
                        // directors-actors
                        sum += findMax(dataSet.getDirectors(), dataSet.getActors(), directors0, directors1);
                        // directors-genres
                        sum += findMax(dataSet.getDirectors(), dataSet.getGenres(), directors0, directors1);
                        DistanceInfo<String> info = new DistanceInfo<>(directors0, directors1, "directors", sum / 3);
                        result.put("directors", info);
                        System.out.println("directors: " + directorsCount.incrementAndGet());
                    }
                } catch (Throwable e) {
                    logger.error("ERROR", e);
                }
            }));
        }
        // keywords
        AtomicInteger keywordsCount = new AtomicInteger(0);
        for (int i = 0; i < dataSet.getKeywords().size(); i++) {
            final int fi = i;
            futures.add(executorService.submit(() -> {
                for (int j = 0; j < fi; j++) {
                    List<String> keywords0 = dataSet.getKeywords().get(fi);
                    List<String> keywords1 = dataSet.getKeywords().get(j);
                    double sum = 0;
                    if (ComparisonUtils.compare(keywords0, keywords1)) {
                        DistanceInfo<String> info = new DistanceInfo<>(keywords0, keywords1, "keywords", 0);
                        result.put("keywords", info);
                        continue;
                    }

                    // keywords-actors
                    sum += findMax(dataSet.getKeywords(), dataSet.getActors(), keywords0, keywords1);
                    // keywords-genres
                    sum += findMax(dataSet.getKeywords(), dataSet.getGenres(), keywords0, keywords1);
                    // keywords-directors
                    sum += findMax(dataSet.getKeywords(), dataSet.getDirectors(), keywords0, keywords1);
                    DistanceInfo<String> info = new DistanceInfo<>(keywords0, keywords1, "keywords", sum / 3);
                    result.put("keywords", info);
                    System.out.println("keywords: " + keywordsCount.incrementAndGet());
                }
            }));
        }

        for (Future future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                logger.error("ERROR", e);
            }
        }

        executorService.shutdownNow();
        logger.info("distances calculation finished");
        return result;
    }

    /**
     * Например:
     *
     * @param attributes1 – существующие списки жанров (просто выгружены из БД)
     * @param attributes2 – существующие списки актеров (просто выгружены из БД)
     * @param xValue – жанр1
     * @param yValue – жанр2
     * @return расстояние между жанрами 1 и 2 относительно аттрибута `актеры`
     */
    public double findMax(List<List<String>> attributes1, List<List<String>> attributes2, List<String> xValue, List<String> yValue) {
        if (attributes1.size() != attributes2.size()) {
            throw new IllegalArgumentException("unequal lengths of attributes lists!");
        }

        double distance = 0;
        Set<List<String>> mainValueSet = new HashSet<>();
        Set<List<String>> supportValueSet = new HashSet<>();
        for (List<String> attribute2Value : attributes2) {
            double p1 = p(attributes1, attributes2, xValue, attribute2Value);
            double p2 = p(attributes1, attributes2, yValue, attribute2Value);
            if (p1 >= p2) {
                if (!mainValueSet.contains(attribute2Value)) {
                    mainValueSet.add(attribute2Value);
                    distance += p1;
                }
            } else {
                if (!supportValueSet.contains(attribute2Value)) {
                    supportValueSet.add(attribute2Value);
                    distance += p2;
                }
            }

        }

        return distance - 1;
    }

    /**
     * Метод считает вероятность, что значение value1 аттрибута attribute1 встретится вместе
     * со значением value2 аттрибута attribute2
     */
    public double p(List<List<String>> attributes1, List<List<String>> attributes2, List<String> value1, List<String> value2) {
        double coOccurrence = 0;
        double value2Occurrence = 0;
        for (int i = 0; i < attributes1.size(); i++) {
            Double similarity = ComparisonUtils.getListsSimilarity(attributes1.get(i), value1);
            value2Occurrence += similarity;
            if (similarity > 0) {
                coOccurrence += ComparisonUtils.getListsSimilarity(attributes2.get(i), value2);
            }
//            if (ComparisonUtils.compare(attributes1.get(i), value1)) {
//                value2Occurrence += 1;
//                if (ComparisonUtils.compare(attributes2.get(i), value2)) {
//                    coOccurrence += 1;
//                }
//            }
        }

        if (value2Occurrence == 0.0) {
            return 0;
        }

        return coOccurrence / value2Occurrence;
    }
}

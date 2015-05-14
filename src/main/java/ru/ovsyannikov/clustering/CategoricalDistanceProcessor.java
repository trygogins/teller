package ru.ovsyannikov.clustering;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;
import ru.ovsyannikov.clustering.model.DataSet;
import ru.ovsyannikov.clustering.model.DistanceInfo;
import ru.ovsyannikov.parsing.Movie;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Class for calculating distances between categorical attributes of movies
 * @see 'http://edu.cs.uni-magdeburg.de/EC/lehre/sommersemester-2013/wissenschaftliches-schreiben-in-der-informatik/publikationen-fuer-studentische-vortraege/kMeansMixedCatNum.pdf'
 *
 * @author Georgii Ovsiannikov
 * @since 5/11/15
 */
@Service
public class CategoricalDistanceProcessor {

    @Autowired
    private MovieSimilarityEstimator similarityEstimator;

    private DataSet dataSet = new DataSet();

    @PostConstruct
    public void init() {
        List<Movie> movies = Boolean.valueOf(System.getProperty("ru.ovsyannikov.teller.production")) ?
                similarityEstimator.getMovies("votes2") : HelperUtils.getTestMovies();
        movies.forEach(dataSet::addItem);
    }

    /**
     * Считает расстояние между всеми парами значений всех атрибутов
     */
    // TODO: refactor – provide movies attributes as a map
    public Multimap<String, DistanceInfo<String>> calculateAttributesDistances() {
        Multimap<String, DistanceInfo<String>> result = HashMultimap.create();
        // actors
        for (List<String> actors0 : dataSet.getActors()) {
            for (List<String> actors1 : dataSet.getActors()) {
                if (!actors0.equals(actors1)) {
                    double sum = 0;
                    // actors-genres
                    sum += findMax(dataSet.getActors(), dataSet.getGenres(), actors0, actors1);
                    // actors-directors
                    sum += findMax(dataSet.getActors(), dataSet.getDirectors(), actors0, actors1);
                    // actors-keywords
                    sum += findMax(dataSet.getActors(), dataSet.getKeywords(), actors0, actors1);
                    DistanceInfo<String> info = new DistanceInfo<>(actors0, actors1, "actors", sum / 3);
                    if (!result.containsValue(info)) {
                        result.put("actors", info);
                    }
                }
            }
        }

        // genres
        for (List<String> genres0 : dataSet.getGenres()) {
            for (List<String> genres1 : dataSet.getGenres()) {
                if (!genres0.equals(genres1)) {
                    double sum = 0;
                    // genres-directors
                    sum += findMax(dataSet.getGenres(), dataSet.getDirectors(), genres0, genres1);
                    // genres-keywords
                    sum += findMax(dataSet.getGenres(), dataSet.getKeywords(), genres0, genres1);
                    // genres-actors
                    sum += findMax(dataSet.getGenres(), dataSet.getActors(), genres0, genres1);
                    DistanceInfo<String> info = new DistanceInfo<>(genres0, genres1, "genres", sum / 3);
                    if (!result.containsValue(info)) {
                        result.put("genres", info);
                    }
                }
            }
        }

        // directors
        for (List<String> directors0 : dataSet.getDirectors()) {
            for (List<String> directors1 : dataSet.getDirectors()) {
                if (!directors0.equals(directors1)) {
                    double sum = 0;
                    // directors-keywords
                    sum += findMax(dataSet.getDirectors(), dataSet.getKeywords(), directors0, directors1);
                    // directors-actors
                    sum += findMax(dataSet.getDirectors(), dataSet.getActors(), directors0, directors1);
                    // directors-genres
                    sum += findMax(dataSet.getDirectors(), dataSet.getGenres(), directors0, directors1);
                    DistanceInfo<String> info = new DistanceInfo<>(directors0, directors1, "directors", sum / 3);
                    if (!result.containsValue(info)) {
                        result.put("directors", info);
                    }
                }
            }
        }

        // keywords
        for (List<String> keywords0 : dataSet.getKeywords()) {
            for (List<String> keywords1 : dataSet.getKeywords()) {
                if (!keywords0.equals(keywords1)) {
                    double sum = 0;
                    // keywords-actors
                    sum += findMax(dataSet.getKeywords(), dataSet.getActors(), keywords0, keywords1);
                    // keywords-genres
                    sum += findMax(dataSet.getKeywords(), dataSet.getGenres(), keywords0, keywords1);
                    // keywords-directors
                    sum += findMax(dataSet.getKeywords(), dataSet.getDirectors(), keywords0, keywords1);
                    DistanceInfo<String> info = new DistanceInfo<>(keywords0, keywords1, "keywords", sum / 3);
                    if (!result.containsValue(info)) {
                        result.put("keywords", info);
                    }
                }
            }
        }

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
            Double similarity1 = HelperUtils.getListsSimilarity(attributes1.get(i), value1);
            value2Occurrence += similarity1;
            if (similarity1 > 0) {
                coOccurrence += HelperUtils.getListsSimilarity(attributes2.get(i), value2);
            }
        }

        if (value2Occurrence == 0.0) {
            return 0;
        }

        return coOccurrence / value2Occurrence;
    }

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        CategoricalDistanceProcessor processor = context.getBean(CategoricalDistanceProcessor.class);
        Multimap<String, DistanceInfo<String>> distances = processor.calculateAttributesDistances();
        System.out.println(distances);
    }
}

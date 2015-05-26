package ru.ovsyannikov.collaborative;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import ru.ovsyannikov.Services;
import ru.ovsyannikov.filtering.TasteElicitationProcessor;
import ru.ovsyannikov.parsing.model.Movie;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Georgii Ovsiannikov
 * @since 5/23/15
 */
public class MovieMarkForecaster {

    Map<Long, List<UserNeighboursProcessor.UserVote>> userVotes;

    public MovieMarkForecaster(Map<Long, List<UserNeighboursProcessor.UserVote>> userVotes) {
        this.userVotes = userVotes;
    }

    public Map<Long, Double> forecastMarks(int howMany, Long userId, Map<Long, Double> neighbours) {
        Set<Long> possibleMoviesToRecommend = new HashSet<>();
        for (Long uId : userVotes.keySet()) {
            possibleMoviesToRecommend.addAll(CollectKinopoiskIds(uId));
        }

        possibleMoviesToRecommend.removeAll(CollectKinopoiskIds(userId));

        Double lowestMark = Double.NEGATIVE_INFINITY;
        boolean full = false;
        Queue<RecommendedItem> itemsQueue = new PriorityQueue<>(howMany + 1, Collections.reverseOrder(new RecommendedItem.ItemsComparator()));

        for (Long kinopoiskId : possibleMoviesToRecommend) {
            Double estimatedMark = estimateMark(userId, kinopoiskId, neighbours);
            if (!Double.isNaN(estimatedMark) && (!full || estimatedMark > lowestMark)) {
                itemsQueue.add(new RecommendedItem(kinopoiskId, estimatedMark));
                if (full) {
                    itemsQueue.poll();
                } else if (itemsQueue.size() > howMany) {
                    full = true;
                    itemsQueue.poll();
                }

                lowestMark = itemsQueue.peek().getEstimatedMark();
            }
        }

        if (itemsQueue.isEmpty()) {
            return Collections.emptyMap();
        }

        return itemsQueue.stream()
                .collect(Collectors.toMap(RecommendedItem::getKinopoiskId, RecommendedItem::getEstimatedMark));
    }

    /**
     * Making forecast for the user's mark on movie with given kinopoiskId
     */
    private Double estimateMark(Long userId, Long kinopoiskId, Map<Long, Double> neighbours) {
        Double preference = 0.0;
        Double totalSimilarity = 0.0;
        int count = 0;

        for (Long uId : neighbours.keySet()) {
            if (!Objects.equals(userId, uId)) {
                Integer userVote = getUserVote(uId, kinopoiskId);
                if (userVote != null) {
                    double similarity = neighbours.get(uId);
                    if (!Double.isNaN(similarity)) {
                        preference += similarity * userVote;
                        totalSimilarity += similarity;
                        count++;
                    }
                }
            }
        }

        if (count <= 1) {
            return Double.NaN;
        }

        return preference / totalSimilarity;
    }

    /**
     * Get the vote of the given user to the movie with given kinopoiskId
     */
    private Integer getUserVote(Long userId, Long kinopoiskId) {
        List<UserNeighboursProcessor.UserVote> targetMovie = userVotes.get(userId).stream()
                .filter(u -> Objects.equals(u.getKinopoiskId(), kinopoiskId))
                .collect(Collectors.toList());
        if (targetMovie.isEmpty()) {
            return null;
        }

        return targetMovie.get(0).getVote();
    }

    /**
     * Take all distinct kinopoiskIds of movies
     */
    private Set<Long> CollectKinopoiskIds(Long userId) {
        return userVotes.get(userId).stream()
                .map(UserNeighboursProcessor.UserVote::getKinopoiskId)
                .collect(Collectors.toSet());
    }

    public static void main(String[] args) {

        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("application-context.xml");
        TasteElicitationProcessor tasteProcessor = context.getBean(TasteElicitationProcessor.class);

        List<Long> userIds = Arrays.asList(1497l, 4230l, 3404l, 6324l, 1585l, 5613l, 5470l, 5892l, 6523l, 3204l);
        double[] errors0 = new double[10];
        double[] errors1 = new double[10];
        for (int i = 0; i < 10; i++) {
            errors0[i] = 0;
            errors1[i] = 0;
        }

        for (int k = 0; k < 10; k++) {
            long userId = userIds.get(k);

            UserNeighboursProcessor firstProcessor = new UserNeighboursProcessor("votes", Services.getTemplate());
            Map<Long, List<UserNeighboursProcessor.UserVote>> userVotes = firstProcessor.getVotesByUser();
            List<UserNeighboursProcessor.UserVote> allUserMarks = userVotes.get(userId);

            UserNeighboursProcessor processor = new UserNeighboursProcessor("votes2", Services.getTemplate());
            Map<Long, List<UserNeighboursProcessor.UserVote>> userVotesChronological = processor.getVotesByUser();
            userVotesChronological.put(userId, allUserMarks);

            UserNeighboursProcessor tProcessor = new UserNeighboursProcessor("votes2", Services.getTemplate());
            Map<Long, List<UserNeighboursProcessor.UserVote>> userVotesActive = tProcessor.getVotesByUser();

            List<Movie> moviesToVote = tasteProcessor.getMoviesToVote(userId);
            List<Long> toVoteIds = moviesToVote.stream().map(Movie::getKinopoiskId).collect(Collectors.toList());

            MovieMarkForecaster markForecaster;
            for (int i = 20; i > 0; i--) {
                // готовим наборы для прогнозирования:
                // 1) набор с оценками в хронологическом порядке
                Map<Long, List<UserNeighboursProcessor.UserVote>> chronologicalVotes = limitUserVotesByDate(i, userId, userVotesChronological);
                // 2) набор с оценками по порядку запрашивания системой
                userVotesActive.put(userId, allUserMarks.stream()
                        .filter(uv -> toVoteIds.contains(uv.getKinopoiskId()))
                        .limit(i)
                        .collect(Collectors.toList()));

                // хронологическая часть
                processor = new UserNeighboursProcessor(chronologicalVotes);
                Map<Long, Double> neighbours = processor.getUserNeighbours(userId, 5);
                System.out.println("Chronological neighbours: " + neighbours);
                markForecaster = new MovieMarkForecaster(chronologicalVotes);
                Map<Long, Double> chronologicalForecast = markForecaster.forecastMarks(5, userId, neighbours);
                System.out.println("Chronological forecast: " + chronologicalForecast);
                double error0 = evaluateForecast(allUserMarks, chronologicalForecast);

                // активная часть (наша)
                processor = new UserNeighboursProcessor(userVotesActive);
                neighbours = processor.getUserNeighbours(userId, 5);
                System.out.println("Active neighbours: " + neighbours);
                markForecaster = new MovieMarkForecaster(userVotesActive);
                Map<Long, Double> activeQueryingForecast = markForecaster.forecastMarks(5, userId, neighbours);
                System.out.println("Active forecast: " + activeQueryingForecast);
                double error1 = evaluateForecast(allUserMarks, activeQueryingForecast);

                System.err.println("Chronological " + error0 + " vs. " + error1 + " Active");
                errors0[k] += error0;
                errors1[k] += error1;
            }

            errors0[k] /= 10;
            errors1[k] /= 10;
        }

        System.out.println(Arrays.toString(errors0));
        System.out.println(Arrays.toString(errors1));
    }

    public static Map<Long, List<UserNeighboursProcessor.UserVote>> limitUserVotesByDate(int howMany, long userId,
                                                                                  Map<Long, List<UserNeighboursProcessor.UserVote>> userVotes) {
        List<UserNeighboursProcessor.UserVote> restVotes = userVotes.get(userId).stream()
                .sorted((o1, o2) -> o1.getDt().compareTo(o2.getDt()))
                .limit(howMany)
                .collect(Collectors.toList());
        userVotes.put(userId, restVotes);

        return userVotes;
    }

    public static Double evaluateForecast(List<UserNeighboursProcessor.UserVote> allUserMarks, Map<Long, Double> forecastedMarks) {
        List<UserNeighboursProcessor.UserVote> actualUserMarks = allUserMarks.stream()
                .filter(uv -> forecastedMarks.containsKey(uv.getKinopoiskId()))
                .collect(Collectors.toList());

        double error = 0.0;
        for (UserNeighboursProcessor.UserVote userMark : actualUserMarks) {
            error += Math.abs(userMark.getVote() - forecastedMarks.get(userMark.getKinopoiskId()));
        }

        return error / actualUserMarks.size();
    }
}

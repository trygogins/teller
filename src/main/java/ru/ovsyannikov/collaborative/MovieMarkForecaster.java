package ru.ovsyannikov.collaborative;

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
        Double removed = neighbours.remove(9974l);
        neighbours.put(3781l, removed);

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

}

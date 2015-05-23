package ru.ovsyannikov.collaborative;

import org.joda.time.DateTime;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.ovsyannikov.Services;

import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Georgii Ovsiannikov
 * @since 5/23/15
 */
public class UserNeighboursProcessor {

    private final Map<Long, List<UserVote>> votesByUser;

    public UserNeighboursProcessor(String tableName, JdbcTemplate template) {
        List<UserVote> votes = template.query("select * from " + tableName, new BeanPropertyRowMapper<>(UserVote.class));
        votesByUser = new HashMap<>();
        for (UserVote userVote : votes) {
            List<UserVote> userVotes = votesByUser.get(userVote.getUserId());
            if (userVotes == null) {
                userVotes = new ArrayList<>();
                votesByUser.put(userVote.getUserId(), userVotes);
            }
            userVotes.add(userVote);
        }
    }

    public UserNeighboursProcessor(Map<Long, List<UserVote>> votesByUser) {
        this.votesByUser = votesByUser;
    }

    public Map<Long, Double> getUserNeighbours(long userId, int howMany) {
        return getNNeighbours(userId, howMany, votesByUser);
    }

    /**
     * Вычисляет N ближайших соседей данного пользователя
     */
    private Map<Long, Double> getNNeighbours(long userId, int howMany, Map<Long, List<UserVote>> userVotes) {
        Map<Long, Double> result = new HashMap<>();
        TreeMap<Long, Double> sorted = new TreeMap<>(new ValueComparator(result));
        List<UserVote> targetUserVotes = userVotes.get(userId);
        userVotes.keySet().stream()
                .filter(uId -> !uId.equals(userId))
                .forEach(uId -> result.put(uId, getUsersSimilarity(targetUserVotes, userVotes.get(uId))));

        sorted.putAll(result);
        return sorted.keySet().stream().limit(howMany).collect(Collectors.toMap(k -> k, result::get));
    }

    private Double getUsersSimilarity(List<UserVote> user1Votes, List<UserVote> user2Votes) {
        double sumXY = 0;
        double sumX = 0;
        double sumX2 = 0;
        double sumY = 0;
        double sumY2 = 0;
        int count = 0;

        for (UserVote user1Vote : user1Votes) {
            for (UserVote user2Vote : user2Votes) {
                if (user1Vote.getKinopoiskId().equals(user2Vote.getKinopoiskId())) {
                    int x = user1Vote.getVote();
                    int y = user2Vote.getVote();
                    sumXY += x * y;
                    sumX += x;
                    sumX2 += x * x;
                    sumY += y;
                    sumY2 += y * y;
                    count++;
                }
            }
        }
        // center the data
        double meanX = sumX / count;
        double meanY = sumY / count;
        double centeredSumXY = sumXY - meanY * sumX;
        double centeredSumX2 = sumX2 - meanX * sumX;
        double centeredSumY2 = sumY2 - meanY * sumY;

        return getPearsonResult(count, centeredSumXY, centeredSumX2, centeredSumY2);
    }

    private Double getPearsonResult(int count, double sumXY, double sumX2, double sumY2) {
        if (count == 0) {
            return Double.NaN;
        }

        double denominator = Math.sqrt(sumX2) * Math.sqrt(sumY2);
        if (denominator == 0.0) {
            return Double.NaN;
        }

        return sumXY / denominator;
    }

    public Map<Long, List<UserVote>> getVotesByUser() {
        return votesByUser;
    }

    public static class UserVote {

        Long userId;
        Long kinopoiskId;
        DateTime dt;
        Integer vote;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getKinopoiskId() {
            return kinopoiskId;
        }

        public void setKinopoiskId(Long kinopoiskId) {
            this.kinopoiskId = kinopoiskId;
        }

        public DateTime getDt() {
            return dt;
        }

        public void setDt(Timestamp dt) {
            this.dt = new DateTime(dt.getTime());
        }

        public Integer getVote() {
            return vote;
        }

        public void setVote(Integer vote) {
            this.vote = vote;
        }
    }

    // Comparator for descending sort
    public static class ValueComparator implements Comparator<Long> {

        Map<Long, Double> base;

        public ValueComparator(Map<Long, Double> base) {
            this.base = base;
        }

        public int compare(Long a, Long b) {
            Double value1 = base.get(a);
            Double value2 = base.get(b);
            if (Double.isNaN(value1)) {
                return 1;
            }
            if (Double.isNaN(value2)) {
                return -1;
            }

            if (value1 >= value2) {
                return -1;
            } else {
                return 1;
            }
        }
    }

    public static void main(String[] args) {
        UserNeighboursProcessor processor = new UserNeighboursProcessor("votes2", Services.getTemplate());

        Map<Long, Double> userNeighbours = processor.getUserNeighbours(1497l, 5);
        MovieMarkForecaster markForecaster = new MovieMarkForecaster(processor.getVotesByUser());
        Map<Long, Double> forecastedMarks = markForecaster.forecastMarks(5, 1497l, userNeighbours);
        System.out.println(forecastedMarks);
    }
}

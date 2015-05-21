package ru.ovsyannikov.elicitation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ovsyannikov.parsing.model.Movie;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Georgii Ovsiannikov
 * @since 5/17/15
 */
public class SplitterDeterminant {

    private static final Logger logger = LoggerFactory.getLogger(SplitterDeterminant.class);

    public Movie getSplitter(List<Movie> movies) {
        // total
        double r = movies.stream().map(Movie::getVotes).flatMap(Collection::stream).mapToDouble(Movie.UserVote::getVote).sum();
        double r2 = movies.stream().map(Movie::getVotes).flatMap(Collection::stream).mapToDouble(uv -> uv.getVote() * uv.getVote()).sum();
        int n = (int) movies.stream().map(Movie::getVotes).flatMap(Collection::stream).count();

        double minError = Double.MAX_VALUE;

        Movie splitter = movies.isEmpty() ? null : movies.get(0);
        for (Movie movie : movies) {
            // likers
            List<Movie.UserVote> likers = getVoters(movie, uv -> uv.getVote() > 7);
            double lr = likers.stream().mapToDouble(Movie.UserVote::getVote).sum();
            double lr2 = likers.stream().mapToDouble(uv -> uv.getVote() * uv.getVote()).sum();
            int ln = likers.size();
            // haters
            List<Movie.UserVote> haters = getVoters(movie, uv -> uv.getVote() < 7);
            double hr = haters.stream().mapToDouble(Movie.UserVote::getVote).sum();
            double hr2 = haters.stream().mapToDouble(uv -> uv.getVote() * uv.getVote()).sum();
            int hn = haters.size();
            // unknown
            double ur = r - lr - hr;
            double ur2 = r2 - lr2 - hr2;
            int un = n - ln - hn;

            double error = (un == 0 ? 0 :(ur2 - ur * ur / un)) +
                    (ln == 0 ? 0 : (lr2 - lr * lr / ln)) +
                    (hn == 0 ? 0 : (hr2 - hr * hr / hn));
            if (error < minError) {
                minError = error;
                splitter = movie;
            }
        }

        return splitter;
    }

    private List<Movie.UserVote> getVoters(Movie movie, Predicate<Movie.UserVote> categoryFilter) {
        return movie.getVotes().stream()
                .filter(categoryFilter)
                .collect(Collectors.toList());
    }

}

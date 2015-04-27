package ru.ovsyannikov.exceptions;

/**
 * @author Georgii Ovsiannikov
 * @since 4/23/15
 */
public class NotEnoughVotesException extends RuntimeException {

    private int votes;

    public NotEnoughVotesException(int votes) {
        super("Not enough movies rated by the user!");
        this.votes = votes;
    }

    public int getVotes() {
        return votes;
    }
}

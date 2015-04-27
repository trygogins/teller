package ru.ovsyannikov.exceptions;

/**
 * @author Georgii Ovsiannikov
 * @since 4/12/15
 */
public class KinopoiskForbiddenException extends RuntimeException {

    private String url;

    public KinopoiskForbiddenException(String url) {
        super("403-Forbidden");
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}

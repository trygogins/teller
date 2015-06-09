package ru.ovsyannikov.parsing.model;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Georgii Ovsiannikov
 * @since 4/2/15
 */
public class Movie {

    public static class UserVote {

        private Long userId;
        private Integer vote;

        public UserVote() {
        }

        public UserVote(Long userId, Integer vote) {
            this.userId = userId;
            this.vote = vote;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Integer getVote() {
            return vote;
        }

        public void setVote(Integer vote) {
            this.vote = vote;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            UserVote userVote = (UserVote) o;

            if (userId != null ? !userId.equals(userVote.userId) : userVote.userId != null) return false;
            if (vote != null ? !vote.equals(userVote.vote) : userVote.vote != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = userId != null ? userId.hashCode() : 0;
            result = 31 * result + (vote != null ? vote.hashCode() : 0);
            return result;
        }
    }

    private Long id;
    private Long kinopoiskId;
    private String title;
    private String director;
    private List<String> genres;
    private List<String> actors;
    private List<String> keywords = new ArrayList<>();
    private Integer year;

    private List<UserVote> votes;

    public Movie() {
        // nothing to do here
    }

    public Movie(String title, String director, List<String> genres, List<String> actors) {
        this.title = title;
        this.director = director;
        this.genres = genres;
        this.actors = actors;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKinopoiskId() {
        return kinopoiskId;
    }

    public void setKinopoiskId(Long kinopoiskId) {
        this.kinopoiskId = kinopoiskId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public List<String> getActors() {
        return actors;
    }

    public void setActors(List<String> actors) {
        this.actors = actors;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public List<UserVote> getVotes() {
        return votes;
    }

    public void setVotes(List<UserVote> votes) {
        this.votes = votes;
    }

    public void fillInFields(Document document) {
        setTitle(document);
        setYear(document);
        setDirector(document);
        setGenres(document);
        setActors(document);
    }

    public void setTitle(Document document) {
        Elements elements = document.select("span[itemprop=alternativeHeadline]");
        if (elements.isEmpty() || elements.size() > 1) {
            throw new IllegalArgumentException("Incorrect number of items for title!");
        }

        this.title = elements.get(0).text();
    }

    public void setDirector(Document document) {
        Elements elements = document.select("td[itemprop=director]");
        if (elements.isEmpty() || elements.size() > 1) {
            throw new IllegalArgumentException("Incorrect number of items for director!");
        }

        this.director = elements.get(0).children().isEmpty() ? "-" : elements.get(0).child(0).text();
    }

    public void setGenres(Document document) {
        genres = new ArrayList<>();
        Elements elements = document.select("span[itemprop=genre]");
        if (elements.size() > 1) {
            throw new IllegalArgumentException("Incorrect number of items for genres list!");
        }
        // there may be no genres (kinopoisk, chyo...)
        if (elements.isEmpty()) {
            return;
        }

        genres.addAll(elements.get(0).children().stream()
                .map(Element::text)
                .collect(Collectors.toList()));
    }

    public void setActors(Document document) {
        actors = new ArrayList<>();
        Elements elements = document.select("li[itemprop=actors]");
        // first five characters
        for (int i = 0; i < (elements.size() > 10 ? 10 : elements.size()); i++) {
            actors.add(elements.get(i).child(0).text());
        }
    }

    public void setYear(Document document) {
        Elements elements = document.select("a[href^=/lists/m_act%5Byear%5D/]");
        if (elements.isEmpty() || elements.size() > 1) {
            throw new IllegalArgumentException("Incorrect number of items for year!");
        }

        this.year = Integer.parseInt(elements.get(0).text());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Movie movie = (Movie) o;

        if (actors != null ? !actors.equals(movie.actors) : movie.actors != null) return false;
        if (director != null ? !director.equals(movie.director) : movie.director != null) return false;
        if (genres != null ? !genres.equals(movie.genres) : movie.genres != null) return false;
        if (!id.equals(movie.id)) return false;
        if (keywords != null ? !keywords.equals(movie.keywords) : movie.keywords != null) return false;
        if (kinopoiskId != null ? !kinopoiskId.equals(movie.kinopoiskId) : movie.kinopoiskId != null) return false;
        if (title != null ? !title.equals(movie.title) : movie.title != null) return false;
        if (year != null ? !year.equals(movie.year) : movie.year != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (kinopoiskId != null ? kinopoiskId.hashCode() : 0);
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (director != null ? director.hashCode() : 0);
        result = 31 * result + (genres != null ? genres.hashCode() : 0);
        result = 31 * result + (actors != null ? actors.hashCode() : 0);
        result = 31 * result + (keywords != null ? keywords.hashCode() : 0);
        result = 31 * result + (year != null ? year.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Movie{" +
                "id=" + id +
                ", title='" + title + '\'' +
                '}';
    }

    public Movie getCopy() {
        Movie movie = new Movie(this.getTitle(), this.getDirector(), this.getGenres(), this.getActors());
        movie.setId(this.getId());
        movie.setKinopoiskId(this.getKinopoiskId());
        movie.setKeywords(this.getKeywords());
        movie.setYear(this.getYear());
        List<UserVote> userVotes = this.getVotes().stream()
                .map(userVote -> new UserVote(userVote.getUserId(), userVote.getVote()))
                .collect(Collectors.toList());
        movie.setVotes(userVotes);

        return movie;
    }
}

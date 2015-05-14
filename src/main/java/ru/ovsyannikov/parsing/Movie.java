package ru.ovsyannikov.parsing;

import com.google.code.morphia.annotations.Entity;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
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
@Entity(noClassnameStored = true, value = "movie_info")
public class Movie {

    private Long id;
    private Long kinopoiskId;
    private String title;
    private String director;
    private List<String> genres;
    private List<String> actors;
    private List<String> keywords = new ArrayList<>();
    private Integer year;

    public Movie() {
        // nothing to do here
    }

    public Movie(String title, String director, List<String> genres, List<String> actors) {
        this.title = title;
        this.director = director;
        this.genres = genres;
        this.actors = actors;
    }

    @JsonIgnore
    public Long getId() {
        return id;
    }

    @JsonIgnore
    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("kinopoisk_id")
    public Long getKinopoiskId() {
        return kinopoiskId;
    }

    @JsonProperty("kinopoisk_id")
    public void setKinopoiskId(Long kinopoiskId) {
        this.kinopoiskId = kinopoiskId;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    public void setTitle(Document document) {
        Elements elements = document.select("h1.moviename-big[itemprop=name]");
        if (elements.isEmpty() || elements.size() > 1) {
            throw new IllegalArgumentException("Incorrect number of items for title!");
        }

        this.title = elements.get(0).text();
    }

    @JsonProperty("director")
    public String getDirector() {
        return director;
    }

    @JsonProperty("director")
    public void setDirector(String director) {
        this.director = director;
    }

    public void setDirector(Document document) {
        Elements elements = document.select("td[itemprop=director]");
        if (elements.isEmpty() || elements.size() > 1) {
            throw new IllegalArgumentException("Incorrect number of items for director!");
        }

        this.director = elements.get(0).children().isEmpty() ? "-" : elements.get(0).child(0).text();
    }

    @JsonProperty("genres")
    public List<String> getGenres() {
        return genres;
    }

    @JsonProperty("genres")
    public void setGenres(List<String> genres) {
        this.genres = genres;
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

    @JsonProperty("actors")
    public List<String> getActors() {
        return actors;
    }

    @JsonProperty("actors")
    public void setActors(List<String> actors) {
        this.actors = actors;
    }

    public void setActors(Document document) {
        actors = new ArrayList<>();
        Elements elements = document.select("li[itemprop=actors]");
        // first five characters
        for (int i = 0; i < (elements.size() > 10 ? 10 : elements.size()); i++) {
            actors.add(elements.get(i).child(0).text());
        }
    }

    @JsonProperty("year")
    public Integer getYear() {
        return year;
    }

    @JsonProperty("year")
    public void setYear(Integer year) {
        this.year = year;
    }

    public void setYear(Document document) {
        Elements elements = document.select("a[href^=/lists/m_act%5Byear%5D/]");
        if (elements.isEmpty() || elements.size() > 1) {
            throw new IllegalArgumentException("Incorrect number of items for year!");
        }

        this.year = Integer.parseInt(elements.get(0).text());
    }

    @JsonProperty("keywords")
    public List<String> getKeywords() {
        return keywords;
    }

    @JsonProperty("keywords")
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public void fillInFields(Document document) {
        setTitle(document);
        setYear(document);
        setDirector(document);
        setGenres(document);
        setActors(document);
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
}

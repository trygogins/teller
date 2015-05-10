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
    private List<String> keywords;
    private Integer year;

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
}

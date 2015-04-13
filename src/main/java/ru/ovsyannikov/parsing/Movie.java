package ru.ovsyannikov.parsing;

import com.google.code.morphia.annotations.Entity;
import org.codehaus.jackson.annotate.JsonProperty;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Georgii Ovsiannikov
 * @since 4/2/15
 */
@Entity(noClassnameStored = true, value = "movie_info")
public class Movie {

    private static final Logger logger = LoggerFactory.getLogger(Movie.class);

    private String title;
    private String director;
    private List<String> genres;
    private List<String> actors;

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(String title) {
        this.title = title;
    }

    public void setTitle(Document document) {
        Elements elements = document.select("span[itemprop=alternativeHeadline]");
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

        this.director = elements.get(0).child(0).text();
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
        if (elements.isEmpty() || elements.size() > 1) {
            throw new IllegalArgumentException("Incorrect number of items for genres list!");
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
        for (int i = 0; i < (elements.size() > 5 ? 5 : elements.size()); i++) {
            actors.add(elements.get(i).child(0).text());
        }
    }

    public void fillInFields(Document document) {
        Method[] methods = Movie.class.getDeclaredMethods();
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length > 0 && parameterTypes[0].equals(Document.class) && !method.getName().equals("fillInFields")) {
                try {
                    method.invoke(this, document);
                } catch (ReflectiveOperationException e) {
                    logger.error("Error in parsing a movie", e);
                }
            }
        }
    }
}

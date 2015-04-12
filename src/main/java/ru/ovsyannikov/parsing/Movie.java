package ru.ovsyannikov.parsing;

import com.google.code.morphia.annotations.Entity;

/**
 * @author Georgii Ovsiannikov
 * @since 4/2/15
 */
@Entity(noClassnameStored = true, value = "movie_info")
public class Movie {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

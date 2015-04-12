package ru.ovsyannikov.parsing;

import com.google.code.morphia.Datastore;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Georgii Ovsiannikov
 * @since 4/2/15
 */
public class MovieProcessor {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        Datastore datastore = context.getBean(Datastore.class);

        Movie movie = new Movie();
        movie.setName("Interstellar");

        datastore.save(movie);
    }

}

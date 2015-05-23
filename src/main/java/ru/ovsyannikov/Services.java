package ru.ovsyannikov;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @author Georgii Ovsiannikov
 * @since 5/23/15
 */
public class Services {

    public static JdbcTemplate getTemplate() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        try {
            dataSource.setDriverClassName("com.mysql.jdbc.Driver");
            dataSource.setUrl("jdbc:mysql://localhost:3306/teller?characterEncoding=UTF-8&amp;useUnicode=true");
            dataSource.setUsername("root");
            dataSource.setPassword("");
            return new JdbcTemplate(dataSource);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}

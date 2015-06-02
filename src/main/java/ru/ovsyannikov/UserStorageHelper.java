package ru.ovsyannikov;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.ovsyannikov.parsing.model.User;

import java.util.Arrays;
import java.util.List;

/**
 * @author Georgii Ovsiannikov
 * @since 4/26/15
 */
@Service
public class UserStorageHelper {

    @Autowired
    public JdbcTemplate template;

    public List<User> getUsers(List<Long> userIds) {
        return template.query("select * from users where user_id in (" + StringUtils.join(userIds, ", ") + ")",
                new BeanPropertyRowMapper<>(User.class));
    }

    public Long getUserId(String username) {
        return template.queryForObject("select user_id from users where username = ?", Long.class, username);
    }

    public User getUser(Long userId) {
        List<User> users = getUsers(Arrays.asList(userId));
        if (users.isEmpty()) {
            throw new IllegalArgumentException("no user with id=" + userId);
        }

        return users.get(0);
    }
}

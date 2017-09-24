package com.firefly.example.reactive.coffee.store.dao.impl;

import com.firefly.$;
import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.dao.UserDAO;
import com.firefly.example.reactive.coffee.store.model.User;
import com.firefly.reactive.adapter.db.ReactiveTransactionalManager;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
@Component
public class UserDAOImpl implements UserDAO {

    @Inject
    private ReactiveTransactionalManager db;

    @Override
    public Mono<User> getByName(String name) {
        if (!$.string.hasText(name)) {
            return Mono.error(new IllegalArgumentException("The username is required"));
        }

        String sql = "select * from `coffee_store`.`user` where `name` = ?";
        return db.execSQL(c -> c.queryForObject(sql, User.class, name));
    }
}

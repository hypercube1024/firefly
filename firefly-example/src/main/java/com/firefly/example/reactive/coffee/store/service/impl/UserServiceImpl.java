package com.firefly.example.reactive.coffee.store.service.impl;

import com.firefly.$;
import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.example.reactive.coffee.store.dao.UserDAO;
import com.firefly.example.reactive.coffee.store.model.User;
import com.firefly.example.reactive.coffee.store.service.UserService;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
@Component
public class UserServiceImpl implements UserService {

    @Inject
    private UserDAO userDAO;

    @Override
    public Mono<User> getByName(String name) {
        return userDAO.getByName(name);
    }
}

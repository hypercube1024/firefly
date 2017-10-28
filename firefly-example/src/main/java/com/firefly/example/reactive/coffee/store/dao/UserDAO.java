package com.firefly.example.reactive.coffee.store.dao;

import com.firefly.example.reactive.coffee.store.model.User;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
public interface UserDAO {

    Mono<User> getByName(String name);

}

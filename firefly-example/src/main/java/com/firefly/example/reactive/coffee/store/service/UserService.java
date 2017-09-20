package com.firefly.example.reactive.coffee.store.service;

import com.firefly.example.reactive.coffee.store.model.User;
import reactor.core.publisher.Mono;

/**
 * @author Pengtao Qiu
 */
public interface UserService {
    Mono<User> getByName(String name);
}

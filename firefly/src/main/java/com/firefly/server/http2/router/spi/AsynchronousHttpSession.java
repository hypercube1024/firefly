package com.firefly.server.http2.router.spi;

import com.firefly.utils.lang.GenericTypeReference;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public interface AsynchronousHttpSession {

    CompletableFuture<String> getId();

    CompletableFuture<Long> getCreationTime();

    CompletableFuture<Long> getLastAccessedTime();

    CompletableFuture<Boolean> setMaxInactiveInterval(int interval);

    CompletableFuture<Integer> getMaxInactiveInterval();

    <T> CompletableFuture<T> getAttribute(String name, Class<T> clazz);

    <T> CompletableFuture<T> getAttribute(String name, GenericTypeReference<T> reference);

    CompletableFuture<Boolean> setAttribute(String name, Object value);

    CompletableFuture<Boolean> removeAttribute(String name);

    CompletableFuture<Boolean> invalidate();

    CompletableFuture<Boolean> isNew();
}

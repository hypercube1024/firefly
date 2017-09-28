package com.firefly.example.test.service;

import com.firefly.example.test.TestBase;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Pengtao Qiu
 */
public class TestOrderService extends TestBase {

    @Before
    public void init() {
        System.out.println("test");
    }

    @Test
    public void test() {
        Mono.just(Arrays.asList("1", "2", "3", "4"))
            .map(list -> list.stream().map(Integer::parseInt).collect(Collectors.toList()))
            .doOnSuccess(list -> {throw new IllegalArgumentException("xxxx");})
            .subscribe(System.out::println, ex -> {
                System.out.println("error test, " + ex.getMessage());
            });
    }
}

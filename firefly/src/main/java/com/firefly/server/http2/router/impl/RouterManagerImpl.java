package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;
import com.firefly.server.http2.router.RouterManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pengtao Qiu
 */
public class RouterManagerImpl implements RouterManager {

    private AtomicInteger idGenerator = new AtomicInteger();
    private final Matcher precisePathMather = new PrecisePathMatcher();
    private final Matcher patternPathMatcher = new PatternPathMatcher();
    private final Matcher regexPathMatcher = new RegexPathMatcher();
    private final Matcher parameterPathMatcher = new ParameterPathMatcher();


    public Matcher getPrecisePathMather() {
        return precisePathMather;
    }

    public Matcher getPatternPathMatcher() {
        return patternPathMatcher;
    }

    public Matcher getRegexPathMatcher() {
        return regexPathMatcher;
    }

    public Matcher getParameterPathMatcher() {
        return parameterPathMatcher;
    }


    @Override
    public Router register() {
        return new RouterImpl(idGenerator.getAndIncrement(), this);
    }

    @Override
    public void accept(SimpleRequest request) {

    }
}

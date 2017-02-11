package com.firefly.server.http2.router.impl;

import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.router.Router;
import com.firefly.server.http2.router.RouterManager;
import com.firefly.utils.pattern.Pattern;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pengtao Qiu
 */
public class RouterManagerImpl implements RouterManager {

    private AtomicInteger idGenerator = new AtomicInteger();
    private Map<String, Set<RouterImpl>> precisePath;
    private Map<PatternPath, Set<RouterImpl>> patternPath;
    private Map<Integer, Map<String, Set<RouterImpl>>> parameterPath;
    private Map<RegexPath, Set<RouterImpl>> regexPath;


    private Map<String, Set<RouterImpl>> precisePath() {
        if (precisePath == null) {
            precisePath = new HashMap<>();
        }
        return precisePath;
    }

    private Map<PatternPath, Set<RouterImpl>> patternPath() {
        if (patternPath == null) {
            patternPath = new HashMap<>();
        }
        return patternPath;
    }

    private Map<Integer, Map<String, Set<RouterImpl>>> parameterPath() {
        if (parameterPath == null) {
            parameterPath = new HashMap<>();
        }
        return parameterPath;
    }

    private Map<RegexPath, Set<RouterImpl>> regexPath() {
        if (regexPath == null) {
            regexPath = new HashMap<>();
        }
        return regexPath;
    }

    void precisePath(String path, RouterImpl router) {
        precisePath().computeIfAbsent(path, k -> new HashSet<>()).add(router);
    }

    void patternPath(String path, RouterImpl router) {
        patternPath().computeIfAbsent(new PatternPath(path), k -> new HashSet<>()).add(router);
    }

    void parameterPath(String path, List<String> paths, RouterImpl router) {
        parameterPath().computeIfAbsent(paths.size(), k -> new HashMap<>())
                       .computeIfAbsent(path, k -> new HashSet<>())
                       .add(router);
    }

    void regexPath(String path, RouterImpl router) {
        regexPath().computeIfAbsent(new RegexPath(path), k -> new HashSet<>()).add(router);
    }

    private static class PatternPath {
        String path;
        Pattern pattern;

        public PatternPath(String path) {
            this.path = path;
            pattern = Pattern.compile(path, "*");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PatternPath that = (PatternPath) o;
            return Objects.equals(path, that.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }

    private static class RegexPath {
        String path;
        java.util.regex.Pattern pattern;

        public RegexPath(String path) {
            this.path = path;
            pattern = java.util.regex.Pattern.compile(path);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RegexPath regexPath = (RegexPath) o;
            return Objects.equals(path, regexPath.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }

    @Override
    public Router register() {
        return new RouterImpl(idGenerator.getAndIncrement(), this);
    }

    @Override
    public void accept(SimpleRequest request) {

    }
}

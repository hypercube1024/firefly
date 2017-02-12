package com.firefly.server.http2.router.impl;

import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.server.http2.router.Matcher.MatchType;
import com.firefly.server.http2.router.Router;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.server.http2.router.utils.PathUtils;
import com.firefly.utils.function.Action1;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author Pengtao Qiu
 */
public class RouterImpl implements Router, Comparable<RouterImpl> {

    private final int id;
    private final RouterManagerImpl routerManager;
    private final Set<MatchType> patchTypes;

    private Action1<RoutingContext> context;
    private volatile boolean enable = true;
    private String url;

    public RouterImpl(int id, RouterManagerImpl routerManager) {
        this.id = id;
        this.routerManager = routerManager;
        patchTypes = new HashSet<>();
    }

    @Override
    public Router path(String url) {
        checkPath(url);
        url = url.trim();

        if (url.length() == 1) {
            switch (url.charAt(0)) {
                case '/':
                    routerManager.getPrecisePathMather().add(url, this);
                    break;
                case '*':
                    routerManager.getPatternPathMatcher().add(url, this);
                    break;
                default:
                    throw new IllegalArgumentException("the url: [" + url + "] format error");
            }
        } else {
            if (url.charAt(0) != '/') {
                throw new IllegalArgumentException("the path must start with '/'");
            }

            if (url.contains("*")) {
                routerManager.getPatternPathMatcher().add(url, this);
            } else {
                if (url.charAt(url.length() - 1) != '/') {
                    url = url + "/";
                }

                List<String> paths = PathUtils.split(url);
                if (isParameterPath(paths)) {
                    routerManager.getParameterPathMatcher().add(url, this);
                } else {
                    routerManager.getPrecisePathMather().add(url, this);
                }
            }
        }
        this.url = url;
        patchTypes.add(MatchType.PATH);
        return this;
    }

    private void checkPath(String url) {
        if (url == null) {
            throw new IllegalArgumentException("the url is null");
        }

        if (this.url != null) {
            throw new IllegalArgumentException("the path of this router has been set");
        }
    }

    private boolean isParameterPath(List<String> paths) {
        for (String p : paths) {
            if (p.charAt(0) == ':') {
                return true;
            }
        }
        return false;
    }

    @Override
    public Router pathRegex(String regex) {
        checkPath(regex);
        regex = regex.trim();
        routerManager.getRegexPathMatcher().add(regex, this);
        this.url = regex;
        patchTypes.add(MatchType.PATH);
        return this;
    }

    @Override
    public Router method(HttpMethod httpMethod) {
        return method(httpMethod.asString());
    }

    @Override
    public Router method(String method) {
        routerManager.getHttpMethodMatcher().add(method, this);
        patchTypes.add(MatchType.METHOD);
        return this;
    }

    @Override
    public Router get(String url) {
        return method(HttpMethod.GET).path(url);
    }

    @Override
    public Router post(String url) {
        return method(HttpMethod.POST).path(url);
    }

    @Override
    public Router put(String url) {
        return method(HttpMethod.PUT).path(url);
    }

    @Override
    public Router delete(String url) {
        return method(HttpMethod.DELETE).path(url);
    }

    @Override
    public Router consumes(String contentType) {
        return null;
    }

    @Override
    public Router produces(String contentType) {
        return null;
    }

    @Override
    public Router handler(Action1<RoutingContext> context) {
        this.context = context;
        return this;
    }

    @Override
    public Router enable() {
        enable = true;
        return this;
    }

    @Override
    public Router disable() {
        enable = false;
        return this;
    }

    public int getId() {
        return id;
    }

    public boolean isEnable() {
        return enable;
    }

    public Set<MatchType> getPatchTypes() {
        return patchTypes;
    }

    public Action1<RoutingContext> getContext() {
        return context;
    }

    @Override
    public int compareTo(RouterImpl o) {
        return Integer.compare(id, o.id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RouterImpl router = (RouterImpl) o;
        return id == router.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

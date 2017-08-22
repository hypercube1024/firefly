package com.firefly.server.http2.router;

import com.firefly.codec.http2.model.HttpMethod;

import java.util.List;
import java.util.Set;

/**
 * @author Pengtao Qiu
 */
public interface Router extends Comparable<Router> {

    int getId();

    boolean isEnable();

    Set<Matcher.MatchType> getMatchTypes();

    Router path(String url);

    Router paths(List<String> urlList);

    Router pathRegex(String regex);

    Router method(String method);

    Router method(HttpMethod httpMethod);

    Router get(String url);

    Router post(String url);

    Router put(String url);

    Router delete(String url);

    Router consumes(String contentType);

    Router produces(String accept);

    Router handler(Handler handler);

    Router enable();

    Router disable();

}

package com.firefly.server.http2.router.impl;

import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.server.http2.SimpleRequest;
import com.firefly.server.http2.router.Matcher;
import com.firefly.server.http2.router.Router;
import com.firefly.server.http2.router.RouterManager;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.CollectionUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pengtao Qiu
 */
public class RouterManagerImpl implements RouterManager {

    private AtomicInteger idGenerator = new AtomicInteger();
    private final Map<Matcher.MatchType, List<Matcher>> matcherMap;
    private final Matcher precisePathMather;
    private final Matcher patternPathMatcher;
    private final Matcher regexPathMatcher;
    private final Matcher parameterPathMatcher;
    private final Matcher httpMethodMatcher;
    private final Matcher contentTypePreciseMatcher;
    private final Matcher contentTypePatternMatcher;
    private final Matcher acceptHeaderMatcher;

    public RouterManagerImpl() {
        matcherMap = new HashMap<>();
        precisePathMather = new PrecisePathMatcher();
        patternPathMatcher = new PatternPathMatcher();
        parameterPathMatcher = new ParameterPathMatcher();
        regexPathMatcher = new RegexPathMatcher();
        matcherMap.put(Matcher.MatchType.PATH, Arrays.asList(precisePathMather, patternPathMatcher, parameterPathMatcher, regexPathMatcher));

        httpMethodMatcher = new HTTPMethodMatcher();
        matcherMap.put(Matcher.MatchType.METHOD, Collections.singletonList(httpMethodMatcher));

        contentTypePreciseMatcher = new ContentTypePreciseMatcher();
        contentTypePatternMatcher = new ContentTypePatternMatcher();
        matcherMap.put(Matcher.MatchType.CONTENT_TYPE, Arrays.asList(contentTypePreciseMatcher, contentTypePatternMatcher));

        acceptHeaderMatcher = new AcceptHeaderMatcher();
        matcherMap.put(Matcher.MatchType.ACCEPT, Collections.singletonList(acceptHeaderMatcher));
    }

    public Matcher getHttpMethodMatcher() {
        return httpMethodMatcher;
    }

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

    public Matcher getContentTypePreciseMatcher() {
        return contentTypePreciseMatcher;
    }

    public Matcher getAcceptHeaderMatcher() {
        return acceptHeaderMatcher;
    }

    public Matcher getContentTypePatternMatcher() {
        return contentTypePatternMatcher;
    }

    @Override
    public NavigableSet<RouterMatchResult> findRouter(String method, String path, String contentType, String accept) {
        Map<Router, Set<Matcher.MatchType>> routerMatchTypes = new HashMap<>();
        Map<Router, Map<String, String>> routerParameters = new HashMap<>();
        findRouter(method, Matcher.MatchType.METHOD, routerMatchTypes, routerParameters);
        findRouter(path, Matcher.MatchType.PATH, routerMatchTypes, routerParameters);
        findRouter(contentType, Matcher.MatchType.CONTENT_TYPE, routerMatchTypes, routerParameters);
        findRouter(accept, Matcher.MatchType.ACCEPT, routerMatchTypes, routerParameters);

        NavigableSet<RouterMatchResult> ret = new TreeSet<>();
        routerMatchTypes.entrySet()
                        .stream()
                        .filter(e -> e.getKey().isEnable())
                        .filter(e -> e.getKey().getMatchTypes().equals(e.getValue()))
                        .map(e -> new RouterMatchResult(e.getKey(), routerParameters.get(e.getKey()), e.getValue()))
                        .forEach(ret::add);
        return ret;
    }

    private void findRouter(String value, Matcher.MatchType matchType,
                            Map<Router, Set<Matcher.MatchType>> routerMatchTypes,
                            Map<Router, Map<String, String>> routerParameters) {
        matcherMap.get(matchType)
                  .stream()
                  .map(m -> m.match(value))
                  .filter(Objects::nonNull)
                  .forEach(result -> result.getRouters().forEach(router -> {
                      routerMatchTypes.computeIfAbsent(router, k -> new HashSet<>())
                                      .add(result.getMatchType());
                      if (!CollectionUtils.isEmpty(result.getParameters())) {
                          routerParameters.computeIfAbsent(router, k -> new HashMap<>())
                                          .putAll(result.getParameters().get(router));
                      }
                  }));
    }

    @Override
    public Router register() {
        return new RouterImpl(idGenerator.getAndIncrement(), this);
    }

    public Router registerLast() {
        return new RouterImpl(Integer.MAX_VALUE, this);
    }

    @Override
    public void accept(SimpleRequest request) {
        NavigableSet<RouterMatchResult> routers = findRouter(
                request.getMethod(),
                request.getURI().getPath(),
                request.getFields().get(HttpHeader.CONTENT_TYPE),
                request.getFields().get(HttpHeader.ACCEPT));
        RoutingContext routingContext = new RoutingContextImpl(request, routers);
        routingContext.next();
    }
}

package com.firefly.server.http2.router.handler.cors;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Pengtao Qiu
 */
public class CORSHandler implements Handler {

    private CORSConfiguration configuration = new CORSConfiguration();

    public CORSConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(CORSConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void handle(RoutingContext ctx) {
        Set<String> allowOrigins = configuration.getAllowOrigins();
        if (!CollectionUtils.isEmpty(allowOrigins)) {
            String origin = ctx.getFields().get(HttpHeader.ORIGIN);
            if ($.string.hasText(origin)) {
                if (ctx.getMethod().equalsIgnoreCase(HttpMethod.OPTIONS.asString())) {
                    // preflight
                    if (allowOrigins.contains(origin)) {
                        Set<String> acReqMethods = Optional.ofNullable(ctx.getFields().get(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD))
                                                           .filter($.string::hasText)
                                                           .map(s -> $.string.split(s, ','))
                                                           .map(arr -> Arrays.stream(arr)
                                                                             .map(String::trim)
                                                                             .filter(configuration.getAllowMethods()::contains)
                                                                             .collect(Collectors.toSet()))
                                                           .orElse(Collections.emptySet());
                        if (!CollectionUtils.isEmpty(acReqMethods)) {
                            ctx.getResponse().getFields().put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                            ctx.getResponse().getFields().put(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, configuration.getAllowCredentials().toString());
                            ctx.getResponse().getFields().put(HttpHeader.ACCESS_CONTROL_MAX_AGE, configuration.getPreflightMaxAge().toString());
                            ctx.getResponse().getFields().put(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS, String.join(", ", configuration.getAllowMethods()));
                            if (!CollectionUtils.isEmpty(configuration.getAllowHeaders())) {
                                Set<String> acReqHeaders = Optional.ofNullable(ctx.getFields().get(HttpHeader.ACCESS_CONTROL_REQUEST_HEADERS))
                                                                   .filter($.string::hasText)
                                                                   .map(s -> $.string.split(s, ','))
                                                                   .map(arr -> Arrays.stream(arr)
                                                                                     .map(String::trim)
                                                                                     .filter(configuration.getAllowHeaders()::contains)
                                                                                     .collect(Collectors.toSet()))
                                                                   .orElse(Collections.emptySet());
                                if (!CollectionUtils.isEmpty(acReqHeaders)) {
                                    ctx.getResponse().getFields().put(HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS, String.join(", ", configuration.getAllowHeaders()));
                                }
                            }
                        }
                    }
                } else {
                    // simple CORS request
                    if (allowOrigins.contains(origin)) {
                        ctx.getResponse().getFields().put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                        ctx.getResponse().getFields().put(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS, configuration.getAllowCredentials().toString());
                        if (!CollectionUtils.isEmpty(configuration.getAllowHeaders())) {
                            ctx.getResponse().getFields().put(HttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS, String.join(", ", configuration.getAllowHeaders()));
                        }
                    }
                }
            }
        }

        ctx.next();
    }
}

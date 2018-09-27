package com.firefly.server.http2.router.handler.cors;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.firefly.codec.http2.model.HttpHeader.*;
import static com.firefly.codec.http2.model.HttpMethod.OPTIONS;

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
        final CORSConfiguration c = configuration;
        final String origin = ctx.getFields().get(ORIGIN);

        if ($.string.hasText(origin) && !CollectionUtils.isEmpty(c.getAllowOrigins()) && c.getAllowOrigins().contains(origin)) {
            if (ctx.getMethod().equalsIgnoreCase(OPTIONS.asString())) { // preflight
                Set<String> acReqMethods = getAcRequestMethods(ctx);
                if (!CollectionUtils.isEmpty(acReqMethods)) {
                    HttpFields fields = ctx.getResponse().getFields();
                    fields.put(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                    fields.put(ACCESS_CONTROL_ALLOW_CREDENTIALS, c.getAllowCredentials().toString());
                    fields.put(ACCESS_CONTROL_MAX_AGE, c.getPreflightMaxAge().toString());
                    fields.put(ACCESS_CONTROL_ALLOW_METHODS, String.join(", ", c.getAllowMethods()));
                    if (!CollectionUtils.isEmpty(c.getAllowHeaders())) {
                        Set<String> acReqHeaders = getAcRequestHeaders(ctx);
                        if (!CollectionUtils.isEmpty(acReqHeaders)) {
                            fields.put(ACCESS_CONTROL_ALLOW_HEADERS, String.join(", ", c.getAllowHeaders()));
                        }
                    }
                }
            } else { // simple CORS request
                HttpFields fields = ctx.getResponse().getFields();
                fields.put(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                fields.put(ACCESS_CONTROL_ALLOW_CREDENTIALS, c.getAllowCredentials().toString());
                if (!CollectionUtils.isEmpty(c.getAllowHeaders())) {
                    fields.put(ACCESS_CONTROL_EXPOSE_HEADERS, String.join(", ", c.getExposeHeaders()));
                }
            }
        }

        ctx.next();
    }

    public Set<String> getAcRequestHeaders(RoutingContext ctx) {
        return Optional.ofNullable(ctx.getFields().get(ACCESS_CONTROL_REQUEST_HEADERS))
                       .filter($.string::hasText)
                       .map(s -> $.string.split(s, ','))
                       .map(arr -> Arrays.stream(arr)
                                         .map(String::trim)
                                         .filter(configuration.getAllowHeaders()::contains)
                                         .collect(Collectors.toSet()))
                       .orElse(Collections.emptySet());
    }

    public Set<String> getAcRequestMethods(RoutingContext ctx) {
        return Optional.ofNullable(ctx.getFields().get(ACCESS_CONTROL_REQUEST_METHOD))
                       .filter($.string::hasText)
                       .map(s -> $.string.split(s, ','))
                       .map(arr -> Arrays.stream(arr)
                                         .map(String::trim)
                                         .filter(configuration.getAllowMethods()::contains)
                                         .collect(Collectors.toSet()))
                       .orElse(Collections.emptySet());
    }
}

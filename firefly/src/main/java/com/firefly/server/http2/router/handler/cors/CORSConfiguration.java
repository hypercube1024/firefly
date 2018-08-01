package com.firefly.server.http2.router.handler.cors;

import com.firefly.codec.http2.model.HttpMethod;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Pengtao Qiu
 */
public class CORSConfiguration {

    private Set<String> allowOrigins;
    private List<String> exposeHeaders;
    private Integer preflightMaxAge = 86400;
    private Boolean allowCredentials = true;
    private Set<String> allowMethods = new HashSet<>(Arrays.asList(
            HttpMethod.GET.asString(),
            HttpMethod.POST.asString(),
            HttpMethod.PUT.asString(),
            HttpMethod.DELETE.asString(),
            HttpMethod.OPTIONS.asString()));
    private Set<String> allowHeaders;

    public Set<String> getAllowOrigins() {
        return allowOrigins;
    }

    public void setAllowOrigins(Set<String> allowOrigins) {
        this.allowOrigins = allowOrigins;
    }

    public List<String> getExposeHeaders() {
        return exposeHeaders;
    }

    public void setExposeHeaders(List<String> exposeHeaders) {
        this.exposeHeaders = exposeHeaders;
    }

    public Integer getPreflightMaxAge() {
        return preflightMaxAge;
    }

    public void setPreflightMaxAge(Integer preflightMaxAge) {
        this.preflightMaxAge = preflightMaxAge;
    }

    public Boolean getAllowCredentials() {
        return allowCredentials;
    }

    public void setAllowCredentials(Boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
    }

    public Set<String> getAllowMethods() {
        return allowMethods;
    }

    public void setAllowMethods(Set<String> allowMethods) {
        this.allowMethods = allowMethods;
    }

    public Set<String> getAllowHeaders() {
        return allowHeaders;
    }

    public void setAllowHeaders(Set<String> allowHeaders) {
        this.allowHeaders = allowHeaders;
    }
}

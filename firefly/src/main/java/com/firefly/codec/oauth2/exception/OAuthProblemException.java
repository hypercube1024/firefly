package com.firefly.codec.oauth2.exception;

import com.firefly.utils.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class OAuthProblemException extends RuntimeException {

    private String error;
    private String description;
    private String uri;
    private String state;
    private String scope;
    private String redirectUri;

    private int responseStatus;

    private Map<String, String> parameters = new HashMap<>();

    protected OAuthProblemException(String error) {
        this(error, "");
    }

    protected OAuthProblemException(String error, String description) {
        super(error + " " + description);
        this.description = description;
        this.error = error;
    }


    public static OAuthProblemException error(String error) {
        return new OAuthProblemException(error);
    }

    public static OAuthProblemException error(String error, String description) {
        return new OAuthProblemException(error, description);
    }

    public OAuthProblemException description(String description) {
        this.description = description;
        return this;
    }

    public OAuthProblemException uri(String uri) {
        this.uri = uri;
        return this;
    }

    public OAuthProblemException state(String state) {
        this.state = state;
        return this;
    }

    public OAuthProblemException scope(String scope) {
        this.scope = scope;
        return this;
    }

    public OAuthProblemException responseStatus(int responseStatus) {
        this.responseStatus = responseStatus;
        return this;
    }

    public OAuthProblemException setParameter(String name, String value) {
        parameters.put(name, value);
        return this;
    }

    public String getError() {
        return error;
    }

    public String getDescription() {
        return description;
    }

    public String getUri() {
        return uri;
    }

    public String getState() {
        return state;
    }

    public String getScope() {
        return scope;
    }

    public int getResponseStatus() {
        return responseStatus == 0 ? 400 : responseStatus;
    }

    public String get(String name) {
        return parameters.get(name);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    @Override
    public String getMessage() {
        StringBuilder b = new StringBuilder();
        if (StringUtils.hasText(error)) {
            b.append(error);
        }

        if (StringUtils.hasText(description)) {
            b.append(", ").append(description);
        }


        if (StringUtils.hasText(uri)) {
            b.append(", ").append(uri);
        }


        if (StringUtils.hasText(state)) {
            b.append(", ").append(state);
        }

        if (StringUtils.hasText(scope)) {
            b.append(", ").append(scope);
        }

        return b.toString();
    }

    @Override
    public String toString() {
        return "OAuthProblemException{" +
                "error='" + error + '\'' +
                ", description='" + description + '\'' +
                ", uri='" + uri + '\'' +
                ", state='" + state + '\'' +
                ", scope='" + scope + '\'' +
                ", redirectUri='" + redirectUri + '\'' +
                ", responseStatus=" + responseStatus +
                ", parameters=" + parameters +
                '}';
    }
}

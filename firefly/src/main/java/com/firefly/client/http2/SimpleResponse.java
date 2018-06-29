package com.firefly.client.http2;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.oauth2.model.AccessTokenResponse;
import com.firefly.codec.oauth2.model.AuthorizationCodeResponse;
import com.firefly.utils.StringUtils;
import com.firefly.utils.collection.MultiMap;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.IO;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import com.firefly.utils.lang.GenericTypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static com.firefly.codec.oauth2.model.OAuth.*;

public class SimpleResponse {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    Response response;
    List<ByteBuffer> responseBody = new ArrayList<>();
    List<Cookie> cookies;
    String stringBody;

    public SimpleResponse(Response response) {
        this.response = response;
    }

    public HttpVersion getHttpVersion() {
        return response.getHttpVersion();
    }

    public HttpFields getFields() {
        return response.getFields();
    }

    public long getContentLength() {
        return response.getContentLength();
    }

    public Iterator<HttpField> iterator() {
        return response.iterator();
    }

    public int getStatus() {
        return response.getStatus();
    }

    public String getReason() {
        return response.getReason();
    }

    public Supplier<HttpFields> getTrailerSupplier() {
        return response.getTrailerSupplier();
    }

    public void forEach(Consumer<? super HttpField> action) {
        response.forEach(action);
    }

    public Spliterator<HttpField> spliterator() {
        return response.spliterator();
    }

    public Response getResponse() {
        return response;
    }

    public List<ByteBuffer> getResponseBody() {
        return responseBody;
    }

    public String getStringBody() {
        return getStringBody("UTF-8");
    }

    public String getStringBody(String charset) {
        if (stringBody == null) {
            String contentEncoding = getFields().get(HttpHeader.CONTENT_ENCODING);
            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                byte[] bytes = BufferUtils.toArray(responseBody);
                if (bytes != null) {
                    try (GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
                        return IO.toString(gzipInputStream, charset);
                    } catch (IOException e) {
                        log.error("unzip exception", e);
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                stringBody = BufferUtils.toString(responseBody, charset);
                return stringBody;
            }
        } else {
            return stringBody;
        }
    }

    public <T> T getJsonBody(GenericTypeReference<T> typeReference) {
        return Json.toObject(getStringBody(), typeReference);
    }

    public <T> T getJsonBody(Class<T> clazz) {
        return Json.toObject(getStringBody(), clazz);
    }

    public JsonObject getJsonObjectBody() {
        return Json.toJsonObject(getStringBody());
    }

    public JsonArray getJsonArrayBody() {
        return Json.toJsonArray(getStringBody());
    }

    public List<Cookie> getCookies() {
        if (cookies == null) {
            cookies = response.getFields().getValuesList(HttpHeader.SET_COOKIE.asString()).stream()
                              .map(CookieParser::parseSetCookie).collect(Collectors.toList());
            return cookies;
        } else {
            return cookies;
        }
    }

    public AuthorizationCodeResponse getAuthorizationCodeResponse() {
        return Optional.ofNullable(getFields().get(HttpHeader.LOCATION))
                       .map(HttpURI::new)
                       .map(uri -> {
                           MultiMap<String> parameters = new MultiMap<>();
                           uri.decodeQueryTo(parameters);
                           AuthorizationCodeResponse r = new AuthorizationCodeResponse();
                           r.setCode(parameters.getString(OAUTH_CODE));
                           r.setState(parameters.getString(OAUTH_STATE));
                           return r;
                       }).orElse(null);
    }

    public AccessTokenResponse getAccessTokenResponseFromFragment() {
        return Optional.ofNullable(getFields().get(HttpHeader.LOCATION))
                       .map(HttpURI::new)
                       .map(uri -> {
                           String fragment = uri.getFragment();
                           MultiMap<String> parameters = new MultiMap<>();
                           UrlEncoded.decodeUtf8To(fragment, parameters);
                           AccessTokenResponse r = new AccessTokenResponse();
                           r.setState(parameters.getString(OAUTH_STATE));
                           r.setScope(parameters.getString(OAUTH_SCOPE));
                           r.setRefreshToken(parameters.getString(OAUTH_REFRESH_TOKEN));
                           r.setAccessToken(parameters.getString(OAUTH_ACCESS_TOKEN));
                           r.setTokenType(parameters.getString(OAUTH_TOKEN_TYPE));
                           r.setExpiresIn(Optional.ofNullable(parameters.getString(OAUTH_EXPIRES_IN))
                                                  .filter(StringUtils::hasText)
                                                  .map(Long::parseLong).orElse(null));
                           return r;
                       }).orElse(null);
    }
}

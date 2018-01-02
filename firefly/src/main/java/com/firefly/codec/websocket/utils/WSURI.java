package com.firefly.codec.websocket.utils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

/**
 * Utility methods for converting a {@link URI} between a HTTP(S) and WS(S) URI.
 */
public final class WSURI {
    /**
     * Convert to HTTP <code>http</code> or <code>https</code> scheme URIs.
     * <p>
     * Converting <code>ws</code> and <code>wss</code> URIs to their HTTP equivalent
     *
     * @param inputUri the input URI
     * @return the HTTP scheme URI for the input URI.
     * @throws URISyntaxException if unable to convert the input URI
     */
    public static URI toHttp(final URI inputUri) throws URISyntaxException {
        Objects.requireNonNull(inputUri, "Input URI must not be null");
        String wsScheme = inputUri.getScheme();
        if ("http".equalsIgnoreCase(wsScheme) || "https".equalsIgnoreCase(wsScheme)) {
            // leave alone
            return inputUri;
        }

        if ("ws".equalsIgnoreCase(wsScheme)) {
            // convert to http
            return new URI("http" + inputUri.toString().substring(wsScheme.length()));
        }

        if ("wss".equalsIgnoreCase(wsScheme)) {
            // convert to https
            return new URI("https" + inputUri.toString().substring(wsScheme.length()));
        }

        throw new URISyntaxException(inputUri.toString(), "Unrecognized WebSocket scheme");
    }

    /**
     * Convert to WebSocket <code>ws</code> or <code>wss</code> scheme URIs
     * <p>
     * Converting <code>http</code> and <code>https</code> URIs to their WebSocket equivalent
     *
     * @param inputUrl the input URI
     * @return the WebSocket scheme URI for the input URI.
     * @throws URISyntaxException if unable to convert the input URI
     */
    public static URI toWebsocket(CharSequence inputUrl) throws URISyntaxException {
        return toWebsocket(new URI(inputUrl.toString()));
    }

    /**
     * Convert to WebSocket <code>ws</code> or <code>wss</code> scheme URIs
     * <p>
     * Converting <code>http</code> and <code>https</code> URIs to their WebSocket equivalent
     *
     * @param inputUrl the input URI
     * @param query    the optional query string
     * @return the WebSocket scheme URI for the input URI.
     * @throws URISyntaxException if unable to convert the input URI
     */
    public static URI toWebsocket(CharSequence inputUrl, String query) throws URISyntaxException {
        if (query == null) {
            return toWebsocket(new URI(inputUrl.toString()));
        }
        return toWebsocket(new URI(inputUrl.toString() + '?' + query));
    }

    /**
     * Convert to WebSocket <code>ws</code> or <code>wss</code> scheme URIs
     * <p>
     * <p>
     * Converting <code>http</code> and <code>https</code> URIs to their WebSocket equivalent
     *
     * @param inputUri the input URI
     * @return the WebSocket scheme URI for the input URI.
     * @throws URISyntaxException if unable to convert the input URI
     */
    public static URI toWebsocket(final URI inputUri) throws URISyntaxException {
        Objects.requireNonNull(inputUri, "Input URI must not be null");
        String httpScheme = inputUri.getScheme();
        if ("ws".equalsIgnoreCase(httpScheme) || "wss".equalsIgnoreCase(httpScheme)) {
            // keep as-is
            return inputUri;
        }

        if ("http".equalsIgnoreCase(httpScheme)) {
            // convert to ws
            return new URI("ws" + inputUri.toString().substring(httpScheme.length()));
        }

        if ("https".equalsIgnoreCase(httpScheme)) {
            // convert to wss
            return new URI("wss" + inputUri.toString().substring(httpScheme.length()));
        }

        throw new URISyntaxException(inputUri.toString(), "Unrecognized HTTP scheme");
    }
}

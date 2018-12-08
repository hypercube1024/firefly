package com.fireflysource.net.http.model;

import com.fireflysource.common.collection.map.MultiMap;
import com.fireflysource.common.object.TypeUtils;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Http URI. Parse a HTTP URI from a string or byte array. Given a URI
 * <code>http://user@host:port/path/info;param?query#fragment</code> this class
 * will split it into the following undecoded optional elements:
 * <ul>
 * <li>{@link #getScheme()} - http:</li>
 * <li>{@link #getAuthority()} - //name@host:port</li>
 * <li>{@link #getHost()} - host</li>
 * <li>{@link #getPort()} - port</li>
 * <li>{@link #getPath()} - /path/info</li>
 * <li>{@link #getParam()} - param</li>
 * <li>{@link #getQuery()} - query</li>
 * <li>{@link #getFragment()} - fragment</li>
 * </ul>
 *
 * <p>
 * Any parameters will be returned from {@link #getPath()}, but are excluded
 * from the return value of {@link #getDecodedPath()}. If there are multiple
 * parameters, the {@link #getParam()} method returns only the last one.
 */
public class HttpURI {
    private enum State {
        START,
        HOST_OR_PATH,
        SCHEME_OR_PATH,
        HOST,
        IPV6,
        PORT,
        PATH,
        PARAM,
        QUERY,
        FRAGMENT,
        ASTERISK
    }

    private String scheme;
    private String user;
    private String host;
    private int port;
    private String path;
    private String param;
    private String query;
    private String fragment;

    String uri;
    String decodedPath;

    /**
     * Construct a normalized URI.
     * Port is not set if it is the default port.
     *
     * @param scheme   the URI scheme
     * @param host     the URI hose
     * @param port     the URI port
     * @param path     the URI path
     * @param param    the URI param
     * @param query    the URI query
     * @param fragment the URI fragment
     * @return the normalized URI
     */
    public static HttpURI createHttpURI(String scheme, String host, int port, String path, String param, String query, String fragment) {
        if (port == 80 && HttpScheme.HTTP.is(scheme))
            port = 0;
        if (port == 443 && HttpScheme.HTTPS.is(scheme))
            port = 0;
        return new HttpURI(scheme, host, port, path, param, query, fragment);
    }

    public HttpURI() {
    }

    public HttpURI(String scheme, String host, int port, String path, String param, String query, String fragment) {
        this.scheme = scheme;
        this.host = host;
        this.port = port;
        this.path = path;
        this.param = param;
        this.query = query;
        this.fragment = fragment;
    }

    public HttpURI(HttpURI uri) {
        this(uri.scheme, uri.host, uri.port, uri.path, uri.param, uri.query, uri.fragment);
        this.uri = uri.uri;
    }

    public HttpURI(String uri) {
        port = -1;
        parse(State.START, uri, 0, uri.length());
    }

    public HttpURI(URI uri) {
        this.uri = null;

        scheme = uri.getScheme();
        host = uri.getHost();
        if (host == null && uri.getRawSchemeSpecificPart().startsWith("//"))
            host = "";
        port = uri.getPort();
        user = uri.getUserInfo();
        path = uri.getRawPath();

        decodedPath = uri.getPath();
        if (decodedPath != null) {
            int p = decodedPath.lastIndexOf(';');
            if (p >= 0)
                param = decodedPath.substring(p + 1);
        }
        query = uri.getRawQuery();
        fragment = uri.getFragment();

        decodedPath = null;
    }

    public HttpURI(String scheme, String host, int port, String pathQuery) {
        uri = null;

        this.scheme = scheme;
        this.host = host;
        this.port = port;

        if (pathQuery != null)
            parse(State.PATH, pathQuery, 0, pathQuery.length());

    }

    public void parse(String uri) {
        clear();
        this.uri = uri;
        parse(State.START, uri, 0, uri.length());
    }

    /**
     * Parse according to https://tools.ietf.org/html/rfc7230#section-5.3
     *
     * @param method the request method
     * @param uri    the request uri
     */
    public void parseRequestTarget(String method, String uri) {
        clear();
        this.uri = uri;

        if (HttpMethod.CONNECT.is(method))
            path = uri;
        else
            parse(uri.startsWith("/") ? State.PATH : State.START, uri, 0, uri.length());
    }

    @Deprecated
    public void parseConnect(String uri) {
        clear();
        this.uri = uri;
        path = uri;
    }

    public void parse(String uri, int offset, int length) {
        clear();
        int end = offset + length;
        this.uri = uri.substring(offset, end);
        parse(State.START, uri, offset, end);
    }

    private void parse(State state, final String uri, final int offset, final int end) {
        boolean encoded = false;
        int mark = offset;
        int path_mark = 0;

        for (int i = offset; i < end; i++) {
            char c = uri.charAt(i);

            switch (state) {
                case START: {
                    switch (c) {
                        case '/':
                            mark = i;
                            state = State.HOST_OR_PATH;
                            break;
                        case ';':
                            mark = i + 1;
                            state = State.PARAM;
                            break;
                        case '?':
                            // assume empty path (if seen at start)
                            path = "";
                            mark = i + 1;
                            state = State.QUERY;
                            break;
                        case '#':
                            mark = i + 1;
                            state = State.FRAGMENT;
                            break;
                        case '*':
                            path = "*";
                            state = State.ASTERISK;
                            break;

                        default:
                            mark = i;
                            if (scheme == null)
                                state = State.SCHEME_OR_PATH;
                            else {
                                path_mark = i;
                                state = State.PATH;
                            }
                    }

                    continue;
                }

                case SCHEME_OR_PATH: {
                    switch (c) {
                        case ':':
                            // must have been a scheme
                            scheme = uri.substring(mark, i);
                            // Start again with scheme set
                            state = State.START;
                            break;

                        case '/':
                            // must have been in a path and still are
                            state = State.PATH;
                            break;

                        case ';':
                            // must have been in a path
                            mark = i + 1;
                            state = State.PARAM;
                            break;

                        case '?':
                            // must have been in a path
                            path = uri.substring(mark, i);
                            mark = i + 1;
                            state = State.QUERY;
                            break;

                        case '%':
                            // must have be in an encoded path
                            encoded = true;
                            state = State.PATH;
                            break;

                        case '#':
                            // must have been in a path
                            path = uri.substring(mark, i);
                            state = State.FRAGMENT;
                            break;
                    }
                    continue;
                }

                case HOST_OR_PATH: {
                    switch (c) {
                        case '/':
                            host = "";
                            mark = i + 1;
                            state = State.HOST;
                            break;

                        case '@':
                        case ';':
                        case '?':
                        case '#':
                            // was a path, look again
                            i--;
                            path_mark = mark;
                            state = State.PATH;
                            break;
                        default:
                            // it is a path
                            path_mark = mark;
                            state = State.PATH;
                    }
                    continue;
                }

                case HOST: {
                    switch (c) {
                        case '/':
                            host = uri.substring(mark, i);
                            path_mark = mark = i;
                            state = State.PATH;
                            break;
                        case ':':
                            if (i > mark)
                                host = uri.substring(mark, i);
                            mark = i + 1;
                            state = State.PORT;
                            break;
                        case '@':
                            if (user != null)
                                throw new IllegalArgumentException("Bad authority");
                            user = uri.substring(mark, i);
                            mark = i + 1;
                            break;

                        case '[':
                            state = State.IPV6;
                            break;
                    }
                    continue;
                }

                case IPV6: {
                    switch (c) {
                        case '/':
                            throw new IllegalArgumentException("No closing ']' for ipv6 in " + uri);
                        case ']':
                            c = uri.charAt(++i);
                            host = uri.substring(mark, i);
                            if (c == ':') {
                                mark = i + 1;
                                state = State.PORT;
                            } else {
                                path_mark = mark = i;
                                state = State.PATH;
                            }
                            break;
                    }

                    continue;
                }

                case PORT: {
                    if (c == '@') {
                        if (user != null)
                            throw new IllegalArgumentException("Bad authority");
                        // It wasn't a port, but a password!
                        user = host + ":" + uri.substring(mark, i);
                        mark = i + 1;
                        state = State.HOST;
                    } else if (c == '/') {
                        port = TypeUtils.parseInt(uri, mark, i - mark, 10);
                        path_mark = mark = i;
                        state = State.PATH;
                    }
                    continue;
                }

                case PATH: {
                    switch (c) {
                        case ';':
                            mark = i + 1;
                            state = State.PARAM;
                            break;
                        case '?':
                            path = uri.substring(path_mark, i);
                            mark = i + 1;
                            state = State.QUERY;
                            break;
                        case '#':
                            path = uri.substring(path_mark, i);
                            mark = i + 1;
                            state = State.FRAGMENT;
                            break;
                        case '%':
                            encoded = true;
                            break;
                    }
                    continue;
                }

                case PARAM: {
                    switch (c) {
                        case '?':
                            path = uri.substring(path_mark, i);
                            param = uri.substring(mark, i);
                            mark = i + 1;
                            state = State.QUERY;
                            break;
                        case '#':
                            path = uri.substring(path_mark, i);
                            param = uri.substring(mark, i);
                            mark = i + 1;
                            state = State.FRAGMENT;
                            break;
                        case '/':
                            encoded = true;
                            // ignore internal params
                            state = State.PATH;
                            break;
                        case ';':
                            // multiple parameters
                            mark = i + 1;
                            break;
                    }
                    continue;
                }

                case QUERY: {
                    if (c == '#') {
                        query = uri.substring(mark, i);
                        mark = i + 1;
                        state = State.FRAGMENT;
                    }
                    continue;
                }

                case ASTERISK: {
                    throw new IllegalArgumentException("Bad character '*'");
                }

                case FRAGMENT: {
                    fragment = uri.substring(mark, end);
                    i = end;
                }
            }
        }


        switch (state) {
            case START:
                break;
            case SCHEME_OR_PATH:
                path = uri.substring(mark, end);
                break;

            case HOST_OR_PATH:
                path = uri.substring(mark, end);
                break;

            case HOST:
                if (end > mark)
                    host = uri.substring(mark, end);
                break;

            case IPV6:
                throw new IllegalArgumentException("No closing ']' for ipv6 in " + uri);

            case PORT:
                port = TypeUtils.parseInt(uri, mark, end - mark, 10);
                break;

            case ASTERISK:
                break;

            case FRAGMENT:
                fragment = uri.substring(mark, end);
                break;

            case PARAM:
                path = uri.substring(path_mark, end);
                param = uri.substring(mark, end);
                break;

            case PATH:
                path = uri.substring(path_mark, end);
                break;

            case QUERY:
                query = uri.substring(mark, end);
                break;
        }

        if (!encoded) {
            if (param == null)
                decodedPath = path;
            else
                decodedPath = path.substring(0, path.length() - param.length() - 1);
        }
    }

    public String getScheme() {
        return scheme;
    }

    public String getHost() {
        // Return null for empty host to retain compatibility with java.net.URI
        if (host != null && host.length() == 0)
            return null;
        return host;
    }

    public int getPort() {
        return port;
    }

    /**
     * The parsed Path.
     *
     * @return the path as parsed on valid URI.  null for invalid URI.
     */
    public String getPath() {
        return path;
    }

    public String getDecodedPath() {
        if (decodedPath == null && path != null)
            decodedPath = URIUtils.decodePath(path);
        return decodedPath;
    }


    public String getParam() {
        return param;
    }


    public String getQuery() {
        return query;
    }


    public boolean hasQuery() {
        return query != null && query.length() > 0;
    }


    public String getFragment() {
        return fragment;
    }


    public void decodeQueryTo(MultiMap<String> parameters) {
        if (query == null)
            return;
        UrlEncoded.decodeUtf8To(query, parameters);
    }


    public void decodeQueryTo(MultiMap<String> parameters, String encoding) throws UnsupportedEncodingException {
        decodeQueryTo(parameters, Charset.forName(encoding));
    }


    public void decodeQueryTo(MultiMap<String> parameters, Charset encoding) throws UnsupportedEncodingException {
        if (query == null)
            return;

        if (encoding == null || StandardCharsets.UTF_8.equals(encoding))
            UrlEncoded.decodeUtf8To(query, parameters);
        else
            UrlEncoded.decodeTo(query, parameters, encoding);
    }


    public void clear() {
        uri = null;

        scheme = null;
        host = null;
        port = -1;
        path = null;
        param = null;
        query = null;
        fragment = null;

        decodedPath = null;
    }


    public boolean isAbsolute() {
        return scheme != null && scheme.length() > 0;
    }


    @Override
    public String toString() {
        if (uri == null) {
            StringBuilder out = new StringBuilder();

            if (scheme != null)
                out.append(scheme).append(':');

            if (host != null) {
                out.append("//");
                if (user != null)
                    out.append(user).append('@');
                out.append(host);
            }

            if (port > 0)
                out.append(':').append(port);

            if (path != null)
                out.append(path);

            if (query != null)
                out.append('?').append(query);

            if (fragment != null)
                out.append('#').append(fragment);

            if (out.length() > 0)
                uri = out.toString();
            else
                uri = "";
        }
        return uri;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HttpURI))
            return false;
        return toString().equals(o.toString());
    }

    public void setScheme(String scheme) {
        this.scheme = scheme;
        uri = null;
    }

    /**
     * @param host the host
     * @param port the port
     */
    public void setAuthority(String host, int port) {
        this.host = host;
        this.port = port;
        uri = null;
    }


    /**
     * @param path the path
     */
    public void setPath(String path) {
        uri = null;
        this.path = path;
        decodedPath = null;
    }


    public void setPathQuery(String path) {
        uri = null;
        this.path = null;
        decodedPath = null;
        param = null;
        fragment = null;
        if (path != null)
            parse(State.PATH, path, 0, path.length());
    }


    public void setQuery(String query) {
        this.query = query;
        uri = null;
    }


    public URI toURI() throws URISyntaxException {
        return new URI(scheme, null, host, port, path, query == null ? null : UrlEncoded.decodeString(query), fragment);
    }


    public String getPathQuery() {
        if (query == null)
            return path;
        return path + "?" + query;
    }


    public String getAuthority() {
        if (port > 0)
            return host + ":" + port;
        return host;
    }


    public String getUser() {
        return user;
    }

}

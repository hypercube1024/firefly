package com.firefly.codec.http2.model.servlet;

import com.firefly.codec.http2.model.*;
import com.firefly.net.tcp.codec.flex.stream.ContextAttribute;
import com.firefly.net.tcp.codec.flex.stream.impl.LazyContextAttribute;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.StringUtils;
import com.firefly.utils.lang.URIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import static com.firefly.utils.StringUtils.EMPTY_STRING_ARRAY;

/**
 * @author Pengtao Qiu
 */
public class HttpServletRequestAdapter implements HttpServletRequest {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    public static final Collection<Locale> DEFAULT_LOCALE = Collections.singleton(Locale.getDefault());
    public static final String ALL_INTERFACES = "0.0.0.0";

    protected final RoutingContext context;
    protected final MetaData.Request request;
    protected Cookie[] cookies;
    protected String method;
    protected String pathInfo;
    protected String originalURI;
    protected String characterEncoding;
    protected ServletInputStream servletInputStream;
    protected ContextAttribute attribute = new LazyContextAttribute();
    protected DispatcherType dispatcherType;

    public HttpServletRequestAdapter(RoutingContext context) {
        this.context = context;
        this.request = context.getRequest().getRequest();

        setMethod(request.getMethod());
        HttpURI uri = request.getURI();
        originalURI = uri.isAbsolute() && request.getHttpVersion() != HttpVersion.HTTP_2 ? uri.toString() : uri.getPathQuery();

        String encoded = uri.getPath();
        String path;
        if (encoded == null) {
            path = uri.isAbsolute() ? "/" : null;
            uri.setPath(path);
        } else if (encoded.startsWith("/")) {
            path = (encoded.length() == 1) ? "/" : URIUtils.canonicalPath(URIUtils.decodePath(encoded));
        } else if ("*".equals(encoded) || HttpMethod.CONNECT.is(getMethod())) {
            path = encoded;
        } else {
            path = null;
        }

        if (path == null || path.isEmpty()) {
            setPathInfo(encoded == null ? "" : encoded);
            throw new BadMessageException(400, "Bad URI");
        }
        setPathInfo(path);
    }

    @Override
    public Cookie[] getCookies() {
        if (cookies == null) {
            cookies = request.getFields().getValuesList(HttpHeader.COOKIE).stream()
                             .filter(StringUtils::hasText)
                             .flatMap(v -> CookieParser.parserServletCookie(v).stream())
                             .toArray(Cookie[]::new);
        }
        return cookies;
    }

    @Override
    public long getDateHeader(String name) {
        return request.getFields().getDateField(name);
    }

    @Override
    public String getHeader(String name) {
        return request.getFields().get(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        return request.getFields().getValues(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return request.getFields().getFieldNames();
    }

    @Override
    public int getIntHeader(String name) {
        return (int) request.getFields().getLongField(name);
    }

    @Override
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Override
    public String getPathInfo() {
        return pathInfo;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return null;
    }

    @Override
    public String getQueryString() {
        return request.getURI().getQuery();
    }

    @Override
    public String getRemoteUser() {
        return Optional.ofNullable(getUserPrincipal()).map(Principal::getName).orElse(null);
    }

    @Override
    public String getRequestedSessionId() {
        return context.getRequestedSessionId();
    }

    @Override
    public String getRequestURI() {
        return request.getURI().getPath();
    }

    @Override
    public StringBuffer getRequestURL() {
        final StringBuffer url = new StringBuffer(128);
        URIUtils.appendSchemeHostPort(url, getScheme(), getServerName(), getServerPort());
        url.append(getRequestURI());
        return url;
    }

    @Override
    public String getServletPath() {
        return null;
    }

    @Override
    public HttpSession getSession(boolean create) {
        try {
            return new HttpSessionAdapter(context.getSession(create).get());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public HttpSession getSession() {
        try {
            return new HttpSessionAdapter(context.getSession().get());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String changeSessionId() {
        try {
            return context.getSession(true).get().getId();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        try {
            return context.getSession(false).get().isInvalid();
        } catch (Exception e) {
            log.error("get session exception", e);
            return false;
        }
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return context.isRequestedSessionIdFromCookie();
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        return context.isRequestedSessionIdFromURL();
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return context.isRequestedSessionIdFromURL();
    }

    @Override
    public Collection<Part> getParts() {
        return context.getParts();
    }

    @Override
    public Part getPart(String name) {
        return context.getPart(name);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        return attribute.getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attribute.getAttributes().keySet());
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    @Override
    public int getContentLength() {
        if (request.getContentLength() != Long.MIN_VALUE) {
            return (int) request.getContentLength();
        } else {
            return (int) request.getFields().getLongField(HttpHeader.CONTENT_LENGTH.toString());
        }
    }

    @Override
    public long getContentLengthLong() {
        if (request.getContentLength() != Long.MIN_VALUE) {
            return request.getContentLength();
        } else {
            return request.getFields().getLongField(HttpHeader.CONTENT_LENGTH.toString());
        }
    }

    @Override
    public String getContentType() {
        String contentType = request == null ? null : request.getFields().get(HttpHeader.CONTENT_TYPE);
        if (characterEncoding == null && contentType != null) {
            MimeTypes.Type mime = MimeTypes.CACHE.get(contentType);
            String charset = (mime == null || mime.getCharset() == null) ? MimeTypes.getCharsetFromContentType(contentType) : mime.getCharset().toString();
            if (charset != null) {
                characterEncoding = charset;
            }
        }
        return contentType;
    }

    @Override
    public ServletInputStream getInputStream() {
        if (servletInputStream == null) {
            InputStream inputStream = context.getInputStream();
            servletInputStream = new ServletInputStream() {

                @Override
                public int read() throws IOException {
                    return inputStream.read();
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    return inputStream.read(b, off, len);
                }

                @Override
                public boolean isFinished() {
                    return true;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {

                }
            };
        }
        return servletInputStream;
    }

    @Override
    public String getParameter(String name) {
        return context.getParameter(name);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(context.getParameterMap().keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return context.getParameterValues(name).toArray(EMPTY_STRING_ARRAY);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return context.getParameterMap().entrySet().stream()
                      .collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().toArray(EMPTY_STRING_ARRAY)));
    }

    @Override
    public String getProtocol() {
        return request.getHttpVersion().toString();
    }

    @Override
    public String getScheme() {
        String scheme = request.getURI().getScheme();
        return scheme == null ? HttpScheme.HTTP.asString() : scheme;
    }

    @Override
    public String getServerName() {
        return Optional.ofNullable(request.getURI().getHost()).orElseGet(this::findServerName);
    }

    protected String findServerName() {
        HttpField host = request.getFields().getField(HttpHeader.HOST);
        if (host != null) {
            if (!(host instanceof HostPortHttpField) && host.getValue() != null && !host.getValue().isEmpty()) {
                host = new HostPortHttpField(host.getValue());
            }
            if (host instanceof HostPortHttpField) {
                HostPortHttpField authority = (HostPortHttpField) host;
                request.getURI().setAuthority(authority.getHost(), authority.getPort());
                return authority.getHost();
            }
        }

        String name = getLocalName();
        if (name != null) {
            return name;
        }

        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            log.error("");
        }

        return null;
    }

    @Override
    public int getServerPort() {
        HttpURI uri = request.getURI();
        int port = (uri == null || uri.getHost() == null) ? findServerPort() : uri.getPort();

        // If no port specified, return the default port for the scheme
        if (port <= 0) {
            if (getScheme().equalsIgnoreCase(URIUtils.HTTPS)) {
                return 443;
            } else {
                return 80;
            }
        }

        // return a specific port
        return port;
    }

    protected int findServerPort() {
        // Return host from header field
        HttpField host = request.getFields().getField(HttpHeader.HOST);
        if (host != null) {
            HostPortHttpField authority = (host instanceof HostPortHttpField)
                    ? ((HostPortHttpField) host)
                    : new HostPortHttpField(host.getValue());
            request.getURI().setAuthority(authority.getHost(), authority.getPort());
            return authority.getPort();
        }

        return getLocalPort();
    }

    @Override
    public BufferedReader getReader() {
        return context.getBufferedReader();
    }

    @Override
    public String getRemoteAddr() {
        InetSocketAddress remote = context.getRequest().getConnection().getRemoteAddress();
        if (remote == null) {
            return "";
        }
        InetAddress address = remote.getAddress();
        if (address == null) {
            return remote.getHostString();
        }
        return address.getHostAddress();
    }

    @Override
    public String getRemoteHost() {
        InetSocketAddress remote = context.getRequest().getConnection().getRemoteAddress();
        return remote == null ? "" : remote.getHostString();
    }

    @Override
    public void setAttribute(String name, Object o) {
        attribute.setAttribute(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        attribute.getAttributes().remove(name);
    }

    @Override
    public Locale getLocale() {
        List<String> acceptable = request.getFields().getQualityCSV(HttpHeader.ACCEPT_LANGUAGE);
        // handle no locale
        if (acceptable.isEmpty()) {
            return Locale.getDefault();
        }

        String language = acceptable.get(0);
        language = HttpFields.stripParameters(language);
        String country = "";
        int dash = language.indexOf('-');
        if (dash > -1) {
            country = language.substring(dash + 1).trim();
            language = language.substring(0, dash).trim();
        }
        return new Locale(language, country);
    }

    @Override
    public Enumeration<Locale> getLocales() {
        List<String> acceptable = request.getFields().getQualityCSV(HttpHeader.ACCEPT_LANGUAGE);

        // handle no locale
        if (acceptable.isEmpty()) {
            return Collections.enumeration(DEFAULT_LOCALE);
        }
        List<Locale> locales = acceptable.stream().map(language -> {
            language = HttpFields.stripParameters(language);
            String country = "";
            int dash = language.indexOf('-');
            if (dash > -1) {
                country = language.substring(dash + 1).trim();
                language = language.substring(0, dash).trim();
            }
            return new Locale(language, country);
        }).collect(Collectors.toList());
        return Collections.enumeration(locales);
    }

    @Override
    public boolean isSecure() {
        return context.getRequest().getConnection().isEncrypted();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        return new RequestDispatcher() {

            @Override
            public void forward(ServletRequest request, ServletResponse response) {
                dispatcherType = DispatcherType.FORWARD;
                context.renderTemplate(path, attribute.getAttributes());
            }

            @Override
            public void include(ServletRequest request, ServletResponse response) {
                dispatcherType = DispatcherType.INCLUDE;
                context.renderTemplate(path, attribute.getAttributes());
            }
        };
    }

    @Override
    public String getRealPath(String path) {
        return null;
    }

    @Override
    public int getRemotePort() {
        InetSocketAddress remote = context.getRequest().getConnection().getRemoteAddress();
        return remote == null ? 0 : remote.getPort();
    }

    @Override
    public String getLocalName() {
        InetSocketAddress local = context.getRequest().getConnection().getLocalAddress();
        if (local != null) {
            return local.getHostString();
        }
        try {
            String name = InetAddress.getLocalHost().getHostName();
            if (ALL_INTERFACES.equals(name)) {
                return null;
            }
            return name;
        } catch (java.net.UnknownHostException ignored) {
        }
        return null;
    }

    @Override
    public String getLocalAddr() {
        InetSocketAddress local = context.getRequest().getConnection().getLocalAddress();
        if (local == null) {
            return "";
        }
        InetAddress address = local.getAddress();
        if (address == null) {
            return local.getHostString();
        }
        return address.getHostAddress();
    }

    @Override
    public int getLocalPort() {
        InetSocketAddress local = context.getRequest().getConnection().getLocalAddress();
        return local == null ? 0 : local.getPort();
    }

    @Override
    public DispatcherType getDispatcherType() {
        return dispatcherType;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public boolean isAsyncStarted() {
        return false;
    }

    @Override
    public boolean isAsyncSupported() {
        return false;
    }

    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }

    @Override
    public String getAuthType() {
        // TODO
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        // TODO
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        // TODO
        return null;
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        // TODO
        return false;
    }

    @Override
    public void login(String username, String password) throws ServletException {
        // TODO
    }

    @Override
    public void logout() throws ServletException {
        // TODO
    }
}

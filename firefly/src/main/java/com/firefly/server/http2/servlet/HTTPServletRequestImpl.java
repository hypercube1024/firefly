package com.firefly.server.http2.servlet;

import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.CookieParser;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MetaData.Request;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.model.MultiPartInputStreamParser;
import com.firefly.codec.http2.stream.HTTPConnection;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.server.exception.HttpServerException;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.collection.MultiMap;
import com.firefly.utils.io.ByteArrayPipedStream;
import com.firefly.utils.io.FilePipedStream;
import com.firefly.utils.io.IO;
import com.firefly.utils.io.PipedStream;
import com.firefly.utils.json.Json;
import com.firefly.utils.json.JsonArray;
import com.firefly.utils.json.JsonObject;
import com.firefly.utils.lang.StringParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.*;

public class HTTPServletRequestImpl implements HttpServletRequest, HttpStringBodyRequest, Closeable {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    private final HTTPConnection connection;
    private final Request request;
    private final HTTPServletResponseImpl response;

    private static final Cookie[] EMPTY_COOKIE_ARR = new Cookie[0];
    private Cookie[] cookies;

    private MultiMap<String> parameterMap;
    private Map<String, String[]> _parameterMap;
    private List<Locale> localeList;
    private Collection<Part> parts;
    private MultipartConfigElement multipartConfigElement;

    final ServerHTTP2Configuration http2Configuration;
    private Charset encoding;
    private String characterEncoding;
    private Map<String, Object> attributeMap = new HashMap<>();
    private boolean requestedSessionIdFromCookie;
    private boolean requestedSessionIdFromURL;
    private String requestedSessionId;
    private HttpSession httpSession;

    private PipedStream bodyPipedStream;
    private ServletInputStream servletInputStream;
    private BufferedReader bufferedReader;

    private AsyncContextImpl asyncContext;
    private RequestDispatcherImpl requestDispatcher;

    private String stringBody;

    HTTPServletRequestImpl(ServerHTTP2Configuration http2Configuration, Request request, Response response,
                           HTTPOutputStream output, HTTPConnection connection) {
        this.request = request;
        this.connection = connection;
        this.http2Configuration = http2Configuration;
        try {
            this.setCharacterEncoding(http2Configuration.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            log.error("set character encoding error", e);
        }
        this.response = new HTTPServletResponseImpl(response, output, this);
    }

    private static class IteratorWrap<T> implements Enumeration<T> {

        private final Iterator<T> iterator;

        IteratorWrap(Iterator<T> iterator) {
            this.iterator = iterator;
        }

        @Override
        public boolean hasMoreElements() {
            return iterator.hasNext();
        }

        @Override
        public T nextElement() {
            return iterator.next();
        }

    }

    HTTPServletResponseImpl getResponse() {
        return response;
    }

    // set and get request attribute

    @Override
    public void setAttribute(String name, Object o) {
        attributeMap.put(name, o);
    }

    @Override
    public void removeAttribute(String name) {
        attributeMap.remove(name);
    }

    @Override
    public Object getAttribute(String name) {
        return attributeMap.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return new IteratorWrap<>(attributeMap.keySet().iterator());
    }

    // get the connection attributes

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        this.encoding = Charset.forName(env);
        this.characterEncoding = env;
    }

    @Override
    public boolean isSecure() {
        return http2Configuration.isSecureConnectionEnabled();
    }

    @Override
    public String getServerName() {
        return connection.getLocalAddress().getHostName();
    }

    @Override
    public int getServerPort() {
        return connection.getLocalAddress().getPort();
    }

    @Override
    public String getRemoteAddr() {
        return connection.getRemoteAddress().getAddress().getHostAddress();
    }

    @Override
    public String getRemoteHost() {
        return connection.getRemoteAddress().getHostName();
    }

    @Override
    public int getRemotePort() {
        return connection.getRemoteAddress().getPort();
    }

    @Override
    public String getLocalName() {
        return connection.getLocalAddress().getHostName();
    }

    @Override
    public String getLocalAddr() {
        return connection.getLocalAddress().getAddress().getHostAddress();
    }

    @Override
    public int getLocalPort() {
        return connection.getLocalAddress().getPort();
    }

    // get HTTP heads

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
    public int getContentLength() {
        return (int) request.getContentLength();
    }

    @Override
    public long getContentLengthLong() {
        return request.getContentLength();
    }

    @Override
    public String getContentType() {
        return request.getFields().get(HttpHeader.CONTENT_TYPE);
    }

    // get HTTP request line
    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getRequestURI() {
        return request.getURI().getPath();
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0)
            port = 80; // Work around java.net.URL bug

        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());
        return url;
    }

    @Override
    public String getProtocol() {
        return request.getHttpVersion().asString();
    }

    @Override
    public String getScheme() {
        return request.getURI().getScheme();
    }

    @Override
    public String getQueryString() {
        return request.getURI().getQuery();
    }

    // get locale
    @Override
    public Locale getLocale() {
        parseLocales();
        return localeList.get(0);
    }

    @Override
    public Enumeration<Locale> getLocales() {
        parseLocales();
        return new IteratorWrap<>(localeList.iterator());
    }

    private void parseLocales() {
        if (localeList == null) {
            localeList = new ArrayList<>();
            Enumeration<String> values = getHeaders("accept-language");
            while (values.hasMoreElements()) {
                parseLocalesHeader(values.nextElement());
            }
            if (localeList.size() == 0) {
                localeList.add(Locale.getDefault());
            }
        }
    }

    /**
     * Parse accept-language header value.
     *
     * @param value The head string
     */
    private void parseLocalesHeader(String value) {
        StringParser parser = new StringParser();
        // Store the accumulated languages that have been requested in
        // a local collection, sorted by the quality value (so we can
        // add Locales in descending order). The values will be ArrayLists
        // containing the corresponding Locales to be added
        TreeMap<Double, ArrayList<Locale>> locales = new TreeMap<>();

        // Pre process the value to remove all whitespace
        int white = value.indexOf(' ');
        if (white < 0)
            white = value.indexOf('\t');
        if (white >= 0) {
            StringBuilder sb = new StringBuilder();
            int len = value.length();
            for (int i = 0; i < len; i++) {
                char ch = value.charAt(i);
                if ((ch != ' ') && (ch != '\t'))
                    sb.append(ch);
            }
            value = sb.toString();
        }

        // Process each comma-delimited language specification
        parser.setString(value); // ASSERT: parser is available to us
        int length = parser.getLength();
        while (true) {

            // Extract the next comma-delimited entry
            int start = parser.getIndex();
            if (start >= length)
                break;
            int end = parser.findChar(',');
            String entry = parser.extract(start, end).trim();
            parser.advance(); // For the following entry

            // Extract the quality factor for this entry
            double quality = 1.0;
            int semi = entry.indexOf(";q=");
            if (semi >= 0) {
                try {
                    String strQuality = entry.substring(semi + 3);
                    if (strQuality.length() <= 5) {
                        quality = Double.parseDouble(strQuality);
                    } else {
                        quality = 0.0;
                    }
                } catch (NumberFormatException e) {
                    quality = 0.0;
                }
                entry = entry.substring(0, semi);
            }

            // Skip entries we are not going to keep track of
            if (quality < 0.00005)
                continue; // Zero (or effectively zero) quality factors
            if ("*".equals(entry))
                continue; // FIXME - "*" entries are not handled

            // Extract the language and country for this entry
            String language;
            String country;
            String variant;
            int dash = entry.indexOf('-');
            if (dash < 0) {
                language = entry;
                country = "";
                variant = "";
            } else {
                language = entry.substring(0, dash);
                country = entry.substring(dash + 1);
                int vDash = country.indexOf('-');
                if (vDash > 0) {
                    String cTemp = country.substring(0, vDash);
                    variant = country.substring(vDash + 1);
                    country = cTemp;
                } else {
                    variant = "";
                }
            }
            if (!StringUtils.isAlpha(language) || !StringUtils.isAlpha(country) || !StringUtils.isAlpha(variant)) {
                continue;
            }

            // Add a new Locale to the list of Locales for this quality level
            Locale locale = new Locale(language, country, variant);
            Double key = -quality; // Reverse the order
            ArrayList<Locale> values = locales.get(key);
            if (values == null) {
                values = new ArrayList<>();
                locales.put(key, values);
            }
            values.add(locale);
        }

        // Process the quality values in highest->lowest order (due to
        // negating the Double value when creating the key)
        for (Double key : locales.keySet()) {
            ArrayList<Locale> list = locales.get(key);
            if (list != null && list.size() > 0) {
                localeList.addAll(list);
            }
        }
    }

    @Override
    public Cookie[] getCookies() {
        if (cookies == null) {
            String cookieStr = getHeader("Cookie");
            if (VerifyUtils.isEmpty(cookieStr)) {
                cookies = EMPTY_COOKIE_ARR;
            } else {
                List<Cookie> list = CookieParser.parserServletCookie(cookieStr);
                cookies = list.toArray(EMPTY_COOKIE_ARR);
            }
            return cookies;
        } else {
            return cookies;
        }
    }

    /*  get HTTP body */
    PipedStream getBodyPipedStream() {
        if (bodyPipedStream == null) {
            long contentLength = request.getContentLength();
            if (contentLength > 0) {
                if (contentLength > http2Configuration.getHttpBodyThreshold()) {
                    bodyPipedStream = new FilePipedStream(http2Configuration.getTemporaryDirectory());
                } else {
                    bodyPipedStream = new ByteArrayPipedStream((int) contentLength);
                }
            } else {
                bodyPipedStream = new FilePipedStream(http2Configuration.getTemporaryDirectory());
            }
            return bodyPipedStream;
        } else {
            return bodyPipedStream;
        }
    }

    private boolean hasData() {
        return bodyPipedStream != null;
    }

    void completeDataReceiving() {
        if (hasData()) {
            try {
                getBodyPipedStream().getOutputStream().close();
            } catch (IOException e) {
                log.error("close http body piped output stream exception", e);
            }
        }
    }

    @Override
    public void close() {
        if (hasData()) {
            try {
                getBodyPipedStream().close();
            } catch (IOException e) {
                log.error("close http body piped stream exception", e);
            }

            if (parts != null) {
                for (Part part : parts) {
                    try {
                        part.delete();
                    } catch (IOException e) {
                        log.error("delete temporary file exception", e);
                    }
                }
            }
        }
    }

    private class HTTPServletInputStream extends ServletInputStream {

        private volatile boolean finished;

        @Override
        public int available() throws IOException {
            if (hasData()) {
                return getBodyPipedStream().getInputStream().available();
            } else {
                return 0;
            }
        }

        @Override
        public void close() throws IOException {
            if (hasData()) {
                getBodyPipedStream().getInputStream().close();
            }
            finished = true;
        }

        @Override
        public boolean isFinished() {
            return finished;
        }

        @Override
        public boolean isReady() {
            return hasData();
        }

        @Override
        public void setReadListener(ReadListener readListener) {
            if (hasData()) {
                try {
                    readListener.onDataAvailable();
                    readListener.onAllDataRead();
                } catch (IOException e) {
                    readListener.onError(e);
                }
            }
        }

        @Override
        public int read() throws IOException {
            finished = true;
            if (hasData()) {
                return getBodyPipedStream().getInputStream().read();
            } else {
                return -1;
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            finished = true;
            if (hasData()) {
                return getBodyPipedStream().getInputStream().read(b, off, len);
            } else {
                return -1;
            }
        }

    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (servletInputStream == null) {
            servletInputStream = new HTTPServletInputStream();
            return servletInputStream;
        } else {
            return servletInputStream;
        }
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (bufferedReader == null) {
            bufferedReader = new BufferedReader(new InputStreamReader(getInputStream(), encoding));
            return bufferedReader;
        } else {
            return bufferedReader;
        }
    }

    private MultiMap<String> getParameters() {
        if (parameterMap == null) {
            parameterMap = new MultiMap<>();
            try {
                request.getURI().decodeQueryTo(parameterMap, encoding);
            } catch (UnsupportedEncodingException e) {
                log.error("parse parameters exception", e);
            }

            String contentType = getContentType();
            if (hasData() && contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
                if ("POST".equals(request.getMethod()) || "PUT".equals(request.getMethod())) {
                    try (BufferedReader in = getReader()) {
                        String urlencodedForm = IO.toString(in);
                        UrlEncoded.decodeTo(urlencodedForm, parameterMap, encoding);
                    } catch (IOException e) {
                        log.error("parse urlencoded form exception", e);
                    }
                }
            }
            return parameterMap;
        } else {
            return parameterMap;
        }
    }

    @Override
    public String getParameter(String name) {
        List<String> values = getParameters().get(name);
        if (values != null && values.size() > 0) {
            return values.get(0);
        } else {
            return null;
        }
    }

    @Override
    public Enumeration<String> getParameterNames() {
        Set<String> names = getParameters().keySet();
        if (names.size() > 0) {
            return new IteratorWrap<>(names.iterator());
        } else {
            return Collections.emptyEnumeration();
        }
    }

    @Override
    public String[] getParameterValues(String name) {
        List<String> values = getParameters().getValues(name);
        if (values != null) {
            return values.toArray(StringUtils.EMPTY_STRING_ARRAY);
        } else {
            return null;
        }
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        if (_parameterMap == null) {
            _parameterMap = getParameters().toStringArrayMap();
        }
        return _parameterMap;
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        if (parts == null) {
            try (ServletInputStream input = getInputStream()) {
                MultiPartInputStreamParser parser = getMultiPartInputStreamParser(input);
                parser.setDeleteOnExit(true);
                parts = parser.getParts();
            }
            return parts;
        } else {
            return parts;
        }
    }

    private MultiPartInputStreamParser getMultiPartInputStreamParser(ServletInputStream input) {
        if (multipartConfigElement != null) {
            return new MultiPartInputStreamParser(input, getContentType(), multipartConfigElement, new File(http2Configuration.getTemporaryDirectory()));
        } else if (http2Configuration.getMultipartConfigElement() != null) {
            return new MultiPartInputStreamParser(input, getContentType(), http2Configuration.getMultipartConfigElement(), new File(http2Configuration.getTemporaryDirectory()));
        } else {
            return new MultiPartInputStreamParser(input, getContentType(), http2Configuration.getDefaultMultipartConfigElement(), new File(http2Configuration.getTemporaryDirectory()));
        }
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        for (Part part : getParts()) {
            if (part.getName().equals(name))
                return part;
        }
        return null;
    }

    /* get session */
    public static String getSessionId(String uri, String sessionIdName) {
        String sessionId = null;
        int i = uri.indexOf(';');
        int j = uri.indexOf('#');
        if (i > 0) {
            String tmp = j > i ? uri.substring(i + 1, j) : uri.substring(i + 1);
            int m = 0;
            for (int k = 0; k < tmp.length(); k++) {
                if (tmp.charAt(k) == '=') {
                    m = k;
                    break;
                }
            }
            if (m > 0) {
                String name = tmp.substring(0, m);
                String value = tmp.substring(m + 1);
                if (name.equals(sessionIdName)) {
                    sessionId = value;
                }
            }
        }
        return sessionId;
    }

    @Override
    public String getRequestedSessionId() {
        if (requestedSessionId != null) {
            return requestedSessionId;
        } else if (isRequestedSessionIdFromCookie()) {
            return requestedSessionId;
        } else if (isRequestedSessionIdFromURL()) {
            return requestedSessionId;
        } else {
            return null;
        }
    }

    private HttpSession _getSession() {
        if (httpSession == null) {
            String sid = getRequestedSessionId();
            httpSession = sid != null ? http2Configuration.getHttpSessionManager().get(sid) : null;
            return httpSession;
        } else {
            return httpSession;
        }
    }

    @Override
    public HttpSession getSession(boolean create) {
        if (create) {
            httpSession = http2Configuration.getHttpSessionManager().create();
            requestedSessionId = httpSession.getId();
            response.addCookie(new Cookie(http2Configuration.getSessionIdName(), httpSession.getId()));
            return httpSession;
        } else {
            return _getSession();
        }
    }

    @Override
    public HttpSession getSession() {
        httpSession = _getSession();
        if (httpSession == null) {
            return getSession(true);
        } else {
            return httpSession;
        }
    }

    @Override
    public String changeSessionId() {
        getSession(true);
        return requestedSessionId;
    }

    @Override
    public boolean isRequestedSessionIdValid() {
        String sid = getRequestedSessionId();
        return sid != null && http2Configuration.getHttpSessionManager().containsKey(sid);
    }

    @Override
    public boolean isRequestedSessionIdFromCookie() {
        if (requestedSessionId != null)
            return requestedSessionIdFromCookie;

        for (Cookie cookie : getCookies()) {
            if (cookie.getName().equals(http2Configuration.getSessionIdName())) {
                requestedSessionId = cookie.getValue();
                requestedSessionIdFromCookie = true;
                requestedSessionIdFromURL = false;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromURL() {
        if (requestedSessionId != null)
            return requestedSessionIdFromURL;

        String sessionId = getSessionId(getRequestURI(), http2Configuration.getSessionIdName());
        if (VerifyUtils.isNotEmpty(sessionId)) {
            requestedSessionId = sessionId;
            requestedSessionIdFromURL = true;
            requestedSessionIdFromCookie = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return isRequestedSessionIdFromURL();
    }

    // asynchronous servlet

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return startAsync(this, response);
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        if (asyncContext == null) {
            asyncContext = new AsyncContextImpl();
        }

        asyncContext.startAsync(servletRequest, servletResponse,
                (servletRequest == this && servletResponse == response),
                http2Configuration.getAsynchronousContextTimeout());
        return asyncContext;
    }

    @Override
    public boolean isAsyncStarted() {
        return asyncContext != null && asyncContext.isStartAsync();
    }

    @Override
    public boolean isAsyncSupported() {
        return true;
    }

    @Override
    public AsyncContext getAsyncContext() {
        if (!isAsyncStarted())
            throw new IllegalStateException("asynchronous servlet doesn't start!");

        return asyncContext;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        if (requestDispatcher == null) {
            requestDispatcher = new RequestDispatcherImpl();
        }
        requestDispatcher.path = path;
        return requestDispatcher;
    }

    @Override
    public DispatcherType getDispatcherType() {
        return DispatcherType.REQUEST;
    }

    @Override
    public String getContextPath() {
        return "";
    }

    @Override
    public String getServletPath() {
        return "";
    }

    @Override
    public String getRealPath(String path) {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public ServletContext getServletContext() {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public String getAuthType() {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public String getPathInfo() {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public String getPathTranslated() {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public String getRemoteUser() {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public boolean isUserInRole(String role) {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public Principal getUserPrincipal() {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public void login(String username, String password) throws ServletException {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public void logout() throws ServletException {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) throws IOException, ServletException {
        throw new HttpServerException("not implement this method!");
    }

    @Override
    public String getStringBody() {
        return getStringBody(getCharacterEncoding());
    }

    @Override
    public String getStringBody(String charset) {
        if (stringBody != null) {
            return stringBody;
        } else {
            try (InputStream in = getInputStream()) {
                stringBody = IO.toString(in, charset);
            } catch (IOException e) {
                log.error("get http request string body exception", e);
            }
            if (stringBody == null) {
                stringBody = "";
            }
            return stringBody;
        }
    }

    @Override
    public <T> T getJsonBody(Class<T> clazz) {
        return Json.toObject(getStringBody(), clazz);
    }

    @Override
    public JsonObject getJsonObjectBody() {
        return Json.toJsonObject(getStringBody());
    }

    @Override
    public JsonArray getJsonArrayBody() {
        return Json.toJsonArray(getStringBody());
    }

    public void setMultipartConfigElement(MultipartConfigElement multipartConfigElement) {
        this.multipartConfigElement = multipartConfigElement;
    }
}

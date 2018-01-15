package com.firefly.codec.http2.model.servlet;

import com.firefly.codec.http2.model.CookieGenerator;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.server.http2.router.RoutingContext;
import com.firefly.utils.VerifyUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;

/**
 * @author Pengtao Qiu
 */
public class HttpServletResponseAdapter implements HttpServletResponse {

    protected final RoutingContext context;
    protected final HttpServletRequest servletRequest;
    protected Locale locale;
    protected String charset;

    public HttpServletResponseAdapter(RoutingContext context, HttpServletRequest servletRequest) {
        this.context = context;
        this.servletRequest = servletRequest;
    }

    @Override
    public void addCookie(Cookie cookie) {
        context.add(HttpHeader.SET_COOKIE, CookieGenerator.generateServletSetCookie(cookie));
    }

    @Override
    public boolean containsHeader(String name) {
        return context.getResponse().getFields().containsKey(name);
    }

    public static String toEncoded(String url, String sessionId,
                                   String sessionIdName) {
        if (url == null || sessionId == null) {
            return url;
        }

        String path = url;
        String query = "";
        String anchor = "";
        int question = url.indexOf('?');
        if (question >= 0) {
            path = url.substring(0, question);
            query = url.substring(question);
        }
        int pound = path.indexOf('#');
        if (pound >= 0) {
            anchor = path.substring(pound);
            path = path.substring(0, pound);
        }
        StringBuilder sb = new StringBuilder(path);
        if (sb.length() > 0) { // jsessionid can't be first.
            sb.append(";");
            sb.append(sessionIdName);
            sb.append("=");
            sb.append(sessionId);
        }
        sb.append(anchor);
        sb.append(query);
        return sb.toString();
    }

    protected String toAbsolute(String location) {
        if (location.startsWith("http"))
            return location;

        StringBuilder sb = new StringBuilder();
        sb.append(servletRequest.getScheme()).append("://")
          .append(servletRequest.getServerName()).append(":")
          .append(servletRequest.getServerPort());

        if (location.charAt(0) == '/') {
            sb.append(location);
        } else {
            String URI = context.getURI().getPath();
            int last = 0;
            for (int i = URI.length() - 1; i >= 0; i--) {
                if (URI.charAt(i) == '/') {
                    last = i + 1;
                    break;
                }
            }
            sb.append(URI.substring(0, last)).append(location);
        }
        return sb.toString();
    }

    @Override
    public String encodeURL(String url) {
        if (VerifyUtils.isEmpty(url)) {
            return null;
        }
        if (url.contains(";" + context.getSessionIdParameterName() + "=")) {
            return url;
        }
        String absoluteURL = toAbsolute(url);

        if (servletRequest.isRequestedSessionIdFromCookie() || servletRequest.isRequestedSessionIdFromURL()) {
            return toEncoded(absoluteURL, servletRequest.getRequestedSessionId(), context.getSessionIdParameterName());
        }
        return null;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return encodeURL(url);
    }

    @Override
    public String encodeUrl(String url) {
        return encodeURL(url);
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return encodeURL(url);
    }

    @Override
    public void sendError(int sc, String msg) {
        context.setStatus(sc).setReason(msg).end();
    }

    @Override
    public void sendError(int sc) {
        context.setStatus(sc).end();
    }

    @Override
    public void sendRedirect(String location) {
        context.redirect(location);
    }

    @Override
    public void setDateHeader(String name, long date) {
        context.getResponse().getFields().putDateField(name, date);
    }

    @Override
    public void addDateHeader(String name, long date) {
        context.getResponse().getFields().addDateField(name, date);
    }

    @Override
    public void setHeader(String name, String value) {
        context.put(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        context.add(name, value);
    }

    @Override
    public void setIntHeader(String name, int value) {
        context.getResponse().getFields().putLongField(name, value);
    }

    @Override
    public void addIntHeader(String name, int value) {
        context.add(name, Integer.toString(value));
    }

    @Override
    public void setStatus(int sc) {
        context.setStatus(sc);
    }

    @Override
    public void setStatus(int sc, String sm) {
        context.setStatus(sc).setReason(sm);
    }

    @Override
    public int getStatus() {
        return context.getResponse().getStatus();
    }

    @Override
    public String getHeader(String name) {
        return context.getResponse().getFields().get(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return context.getResponse().getFields().getValuesList(name);
    }

    @Override
    public Collection<String> getHeaderNames() {
        return context.getResponse().getFields().getFieldNamesCollection();
    }

    @Override
    public String getCharacterEncoding() {
        return charset;
    }

    @Override
    public String getContentType() {
        return context.getResponse().getFields().get(HttpHeader.CONTENT_TYPE);
    }

    @Override
    public ServletOutputStream getOutputStream() {
        OutputStream outputStream = context.getResponse().getOutputStream();
        return new ServletOutputStream() {

            @Override
            public void write(int b) throws IOException {
                outputStream.write(b);
            }

            public void write(byte[] b, int off, int len) throws IOException {
                outputStream.write(b, off, len);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }
        };
    }

    @Override
    public PrintWriter getWriter() {
        return context.getResponse().getPrintWriter();
    }

    @Override
    public void setCharacterEncoding(String charset) {
        this.charset = charset;
    }

    @Override
    public void setContentLength(int len) {
        context.getResponse().getFields().put(HttpHeader.CONTENT_LENGTH, String.valueOf(len));
    }

    @Override
    public void setContentLengthLong(long len) {
        context.getResponse().getFields().put(HttpHeader.CONTENT_LENGTH, String.valueOf(len));
    }

    @Override
    public void setContentType(String type) {
        if (charset != null) {
            context.getResponse().getFields().put(HttpHeader.CONTENT_TYPE, MimeTypes.getContentTypeWithoutCharset(type) + ";charset=" + charset);
        } else {
            context.getResponse().getFields().put(HttpHeader.CONTENT_TYPE, type);
        }
    }

    @Override
    public void setBufferSize(int size) {

    }

    @Override
    public int getBufferSize() {
        return 0;
    }

    @Override
    public void flushBuffer() throws IOException {

    }

    @Override
    public void resetBuffer() {

    }

    @Override
    public boolean isCommitted() {
        return context.getResponse().isCommitted();
    }

    @Override
    public void reset() {

    }

    @Override
    public void setLocale(Locale locale) {
        if (locale == null || isCommitted()) {
            return;
        }

        this.locale = locale;
        context.getResponse().getFields().put(HttpHeader.CONTENT_LANGUAGE, locale.toString().replace('_', '-'));
    }

    @Override
    public Locale getLocale() {
        if (locale == null) {
            return Locale.getDefault();
        } else {
            return locale;
        }
    }
}

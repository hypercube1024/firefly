package com.firefly.codec.http2.model.servlet;

import com.firefly.codec.http2.model.CookieGenerator;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.server.http2.router.RoutingContext;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
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

    public HttpServletResponseAdapter(RoutingContext context) {
        this.context = context;
    }

    @Override
    public void addCookie(Cookie cookie) {
        context.add(HttpHeader.SET_COOKIE, CookieGenerator.generateServletSetCookie(cookie));
    }

    @Override
    public boolean containsHeader(String name) {
        return context.getResponse().getFields().containsKey(name);
    }

    @Override
    public String encodeURL(String url) {
        return null;
    }

    @Override
    public String encodeRedirectURL(String url) {
        return null;
    }

    @Override
    public String encodeUrl(String url) {
        return null;
    }

    @Override
    public String encodeRedirectUrl(String url) {
        return null;
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
        return null;
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
    public void setLocale(Locale loc) {

    }

    @Override
    public Locale getLocale() {
        return null;
    }
}

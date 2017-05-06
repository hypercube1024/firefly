package com.firefly.server.http2;

import com.firefly.codec.http2.model.*;
import com.firefly.codec.http2.model.MetaData.Response;
import com.firefly.codec.http2.stream.BufferedHTTPOutputStream;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class SimpleResponse implements Closeable {

    private static Logger log = LoggerFactory.getLogger("firefly-system");

    Response response;
    HTTPOutputStream output;
    PrintWriter printWriter;
    BufferedHTTPOutputStream bufferedOutputStream;
    int bufferSize = 8 * 1024;
    String characterEncoding = "UTF-8";
    boolean asynchronous;

    public SimpleResponse(Response response, HTTPOutputStream output) {
        this.output = output;
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

    public void forEach(Consumer<? super HttpField> action) {
        response.forEach(action);
    }

    public Supplier<HttpFields> getTrailerSupplier() {
        return response.getTrailerSupplier();
    }

    public void setTrailerSupplier(Supplier<HttpFields> trailers) {
        response.setTrailerSupplier(trailers);
    }

    public Spliterator<HttpField> spliterator() {
        return response.spliterator();
    }

    public Response getResponse() {
        return response;
    }

    public boolean isAsynchronous() {
        return asynchronous;
    }

    public void setAsynchronous(boolean asynchronous) {
        this.asynchronous = asynchronous;
    }

    public synchronized OutputStream getOutputStream() {
        if (printWriter != null) {
            throw new IllegalStateException("the response has used print writer");
        }

        if (bufferedOutputStream == null) {
            bufferedOutputStream = new BufferedHTTPOutputStream(output, bufferSize);
            return bufferedOutputStream;
        } else {
            return bufferedOutputStream;
        }
    }

    public synchronized PrintWriter getPrintWriter() {
        if (bufferedOutputStream != null) {
            throw new IllegalStateException("the response has used output stream");
        }
        if (printWriter == null) {
            try {
                printWriter = new PrintWriter(new OutputStreamWriter(new BufferedHTTPOutputStream(output, bufferSize), characterEncoding));
            } catch (UnsupportedEncodingException e) {
                log.error("create print writer exception", e);
            }
            return printWriter;
        } else {
            return printWriter;
        }
    }


    public String getCharacterEncoding() {
        return characterEncoding;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public boolean isClosed() {
        return output.isClosed();
    }

    public synchronized void close() throws IOException {
        if (bufferedOutputStream != null) {
            bufferedOutputStream.close();
        } else if (printWriter != null) {
            printWriter.close();
        } else {
            getOutputStream().close();
        }
    }

    public synchronized void flush() throws IOException {
        if (bufferedOutputStream != null) {
            bufferedOutputStream.flush();
        } else if (printWriter != null) {
            printWriter.flush();
        }
    }

    public boolean isCommitted() {
        return output != null && output.isCommitted();
    }


    public SimpleResponse setStatus(int status) {
        response.setStatus(status);
        return this;
    }

    public SimpleResponse setReason(String reason) {
        response.setReason(reason);
        return this;
    }

    public SimpleResponse setHttpVersion(HttpVersion httpVersion) {
        response.setHttpVersion(httpVersion);
        return this;
    }

    public SimpleResponse put(HttpHeader header, String value) {
        getFields().put(header, value);
        return this;
    }

    public SimpleResponse put(String header, String value) {
        getFields().put(header, value);
        return this;
    }

    public SimpleResponse add(HttpHeader header, String value) {
        getFields().add(header, value);
        return this;
    }

    public SimpleResponse add(String name, String value) {
        getFields().add(name, value);
        return this;
    }
    public SimpleResponse addCookie(Cookie cookie) {
        response.getFields().add(HttpHeader.SET_COOKIE, CookieGenerator.generateSetCookie(cookie));
        return this;
    }

    public SimpleResponse write(String value) {
        getPrintWriter().print(value);
        return this;
    }

    public SimpleResponse end(String value) {
        return write(value).end();
    }

    public SimpleResponse end() {
        IO.close(this);
        return this;
    }

    public SimpleResponse write(byte[] b, int off, int len) {
        try {
            getOutputStream().write(b, off, len);
        } catch (IOException e) {
            log.error("write data exception", e);
        }
        return this;
    }

    public SimpleResponse write(byte[] b) {
        return write(b, 0, b.length);
    }

    public SimpleResponse end(byte[] b) {
        return write(b).end();
    }
}

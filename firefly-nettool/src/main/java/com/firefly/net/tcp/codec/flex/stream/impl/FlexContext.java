package com.firefly.net.tcp.codec.flex.stream.impl;

import com.firefly.net.tcp.codec.flex.encode.MetaInfoGenerator;
import com.firefly.net.tcp.codec.flex.model.Request;
import com.firefly.net.tcp.codec.flex.model.Response;
import com.firefly.net.tcp.codec.flex.stream.Context;
import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.codec.flex.stream.Stream;
import com.firefly.utils.Assert;
import com.firefly.utils.io.IO;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public class FlexContext implements Context {

    protected final Request request;
    protected final FlexConnection connection;
    protected final Stream stream;

    protected Response response = new Response();
    protected byte[] requestData;
    protected LazyContextAttribute attribute = new LazyContextAttribute();
    protected FlexBufferedOutputStream bufferedOutputStream;
    protected PrintWriter printWriter;

    public FlexContext(Request request, Stream stream, FlexConnection connection) {
        Assert.notNull(request, "The request must be not null.");

        this.request = request;
        this.connection = connection;
        this.stream = stream;
    }

    @Override
    public Request getRequest() {
        return request;
    }

    @Override
    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    @Override
    public Stream getStream() {
        return stream;
    }

    @Override
    public void end() {
        IO.close(getOutputStream());
    }

    @Override
    public synchronized OutputStream getOutputStream() {
        Assert.state(printWriter == null, "The PrintWriter is initialized");

        if (bufferedOutputStream == null) {
            bufferedOutputStream = newBufferedOutputStream();
        }
        return bufferedOutputStream;
    }

    @Override
    public synchronized PrintWriter getPrintWriter() {
        Assert.state(bufferedOutputStream == null, "The OutputStream is initialized");

        if (printWriter == null) {
            printWriter = new PrintWriter(new OutputStreamWriter(newBufferedOutputStream(), StandardCharsets.UTF_8));
        }
        return printWriter;
    }

    protected FlexBufferedOutputStream newBufferedOutputStream() {
        FlexBufferedOutputStream outputStream;
        MetaInfoGenerator metaInfoGenerator = connection.getConfiguration().getMetaInfoGenerator();
        int bufferSize = connection.getConfiguration().getDefaultOutputBufferSize();
        outputStream = new FlexBufferedOutputStream(
                new FlexOutputStream(response, stream, metaInfoGenerator, stream.isCommitted()), bufferSize);
        return outputStream;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attribute.getAttributes();
    }

    @Override
    public void setAttribute(String key, Object value) {
        attribute.setAttribute(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        return attribute.getAttribute(key);
    }

    @Override
    public FlexConnection getConnection() {
        return connection;
    }
}

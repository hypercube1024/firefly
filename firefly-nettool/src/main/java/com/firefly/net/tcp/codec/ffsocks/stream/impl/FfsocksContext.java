package com.firefly.net.tcp.codec.ffsocks.stream.impl;

import com.firefly.net.tcp.codec.ffsocks.encode.MetaInfoGenerator;
import com.firefly.net.tcp.codec.ffsocks.model.Request;
import com.firefly.net.tcp.codec.ffsocks.model.Response;
import com.firefly.net.tcp.codec.ffsocks.stream.Context;
import com.firefly.net.tcp.codec.ffsocks.stream.FfsocksConnection;
import com.firefly.net.tcp.codec.ffsocks.stream.Stream;
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
public class FfsocksContext implements Context {

    protected final Request request;
    protected final FfsocksConnection connection;
    protected final Stream stream;

    protected Response response = new Response();
    protected byte[] requestData;
    protected LazyContextAttribute attribute = new LazyContextAttribute();
    protected BufferedFfsocksOutputStream bufferedOutputStream;
    protected PrintWriter printWriter;

    public FfsocksContext(Request request, Stream stream, FfsocksConnection connection) {
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
    public byte[] getRequestData() {
        return requestData;
    }

    @Override
    public void setRequestData(byte[] requestData) {
        this.requestData = requestData;
    }

    @Override
    public void end() {
        IO.close(getOutputStream());
    }

    @Override
    public synchronized OutputStream getOutputStream() {
        Assert.state(printWriter == null, "The PrintWriter is initialized");

        if (bufferedOutputStream == null) {
            bufferedOutputStream = newBufferedFfsocksOutputStream();
        }
        return bufferedOutputStream;
    }

    @Override
    public synchronized PrintWriter getPrintWriter() {
        Assert.state(bufferedOutputStream == null, "The OutputStream is initialized");

        if (printWriter == null) {
            printWriter = new PrintWriter(new OutputStreamWriter(newBufferedFfsocksOutputStream(), StandardCharsets.UTF_8));
        }
        return printWriter;
    }

    protected BufferedFfsocksOutputStream newBufferedFfsocksOutputStream() {
        BufferedFfsocksOutputStream outputStream;
        MetaInfoGenerator metaInfoGenerator = connection.getConfiguration().getMetaInfoGenerator();
        int bufferSize = connection.getConfiguration().getDefaultOutputBufferSize();
        outputStream = new BufferedFfsocksOutputStream(
                new FfsocksOutputStream(response, stream, metaInfoGenerator, stream.isCommitted()), bufferSize);
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
    public FfsocksConnection getConnection() {
        return connection;
    }
}

package com.firefly.net.tcp.codec.ffsocks.stream;

import com.firefly.net.tcp.codec.ffsocks.model.Request;
import com.firefly.net.tcp.codec.ffsocks.model.Response;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

/**
 * @author Pengtao Qiu
 */
public interface Context extends ContextAttribute {

    Request getRequest();

    Response getResponse();

    Stream getStream();

    byte[] getRequestData();

    void setRequestData(byte[] requestData);

    void end();

    OutputStream getOutputStream();

    PrintWriter getPrintWriter();

    FfsocksConnection getConnection();

}

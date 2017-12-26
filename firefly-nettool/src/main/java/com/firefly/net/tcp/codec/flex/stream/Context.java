package com.firefly.net.tcp.codec.flex.stream;

import com.firefly.net.tcp.codec.flex.model.Request;
import com.firefly.net.tcp.codec.flex.model.Response;

import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * @author Pengtao Qiu
 */
public interface Context extends ContextAttribute {

    Request getRequest();

    Response getResponse();

    Stream getStream();

    void end();

    OutputStream getOutputStream();

    PrintWriter getPrintWriter();

    FlexConnection getConnection();

}

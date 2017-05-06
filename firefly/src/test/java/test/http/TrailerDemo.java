package test.http;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.server.http2.HTTP2ServerBuilder;

/**
 * @author Pengtao Qiu
 */
public class TrailerDemo {
    public static void main(String[] args) {
        HTTP2ServerBuilder httpServer = $.httpsServer();
        httpServer.router().get("/trailer").handler(ctx -> {
            System.out.println("get request");
            ctx.put(HttpHeader.CONTENT_TYPE, "text/plain");
            ctx.getResponse().setTrailerSupplier(() -> {
                HttpFields trailer = new HttpFields();
                trailer.add("Foo", "s0");
                trailer.add("Bar", "s00");
                return trailer;
            });
            ctx.end("trailer test");
        }).listen("localhost", 3324);
    }
}

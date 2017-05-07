package test.http;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;

/**
 * @author Pengtao Qiu
 */
public class DirectHTTP2ServerDemo {
    public static void main(String[] args) {
        SimpleHTTPServerConfiguration configuration = new SimpleHTTPServerConfiguration();
        configuration.setProtocol(HttpVersion.HTTP_2.asString());
        HTTP2ServerBuilder httpServer = $.httpServer(configuration);
        httpServer.router().get("/test").handler(ctx -> {
            System.out.println(ctx.getHttpVersion());
            System.out.println(ctx.getFields());
            ctx.end("test http2");
        }).listen("localhost", 2242);
    }
}

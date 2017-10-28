package test.http;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class HTTPsServerDemo {
    public static void main(String[] args) {
        $.httpsServer()
         .router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8081);
    }
}

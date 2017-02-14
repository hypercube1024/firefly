package test.http;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class HTTPServerDemo1 {

    public static void main(String[] args) {
        $.httpServer().router().get("/")
         .handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8080);
    }
}

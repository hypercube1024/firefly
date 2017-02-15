package test.http;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class HTTPServerDemo1 {

    public static void main2(String[] args) {
        $.httpServer().router().get("/").handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8080);
    }

    public static void main(String[] args) {
        $.httpServer()
         .router().get("/").handler(ctx -> ctx.write("hello world! ").next())
         .router().get("/").handler(ctx -> ctx.end("end message"))
         .listen("localhost", 8080);

        $.httpClient().get("http://localhost:8080/").submit()
         .thenAccept(res -> System.out.println(res.getStringBody()));
        $.httpClient().get("http://localhost:8080/hello").submit()
         .thenAccept(res -> System.out.println(res.getStringBody()));
    }
}

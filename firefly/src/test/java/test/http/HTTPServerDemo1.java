package test.http;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpStatus;

/**
 * @author Pengtao Qiu
 */
public class HTTPServerDemo1 {

    public static void main2(String[] args) {
        $.httpServer()
         .router().get("/")
         .handler(ctx -> ctx.end("hello world!"))
         .listen("localhost", 8080);
    }

    public static void main(String[] args) {
        $.httpServer()
         .router().get("*").handler(ctx -> {
             if (ctx.hasNext()) {
                 ctx.next();
             } else {
                 ctx.setStatus(HttpStatus.NOT_FOUND_404)
                    .end("the " + ctx.getURI().getPath() + " is not found");
             }
         })
         .router().get("/").handler(ctx -> ctx.write("hello world! ").next())
         .router().get("/").handler(ctx -> ctx.end("end message"))
         .listen("localhost", 8080);
    }
}

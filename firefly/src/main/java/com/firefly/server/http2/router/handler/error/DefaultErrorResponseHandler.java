package com.firefly.server.http2.router.handler.error;

import com.firefly.Version;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.server.http2.router.Handler;
import com.firefly.server.http2.router.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Pengtao Qiu
 */
public class DefaultErrorResponseHandler implements Handler {

    private static final Logger log = LoggerFactory.getLogger("firefly-system");

    @Override
    public void handle(RoutingContext ctx) {
        if (ctx.hasNext()) {
            try {
                ctx.next();
            } catch (Throwable t) {
                log.error("http handler exception", t);
                String content = "The server internal error. <br/>" + t.getMessage();
                render(ctx, HttpStatus.INTERNAL_SERVER_ERROR_500, content);
            }
        } else {
            String content = "The resource " + ctx.getURI().getPath() + " is not found";
            render(ctx, HttpStatus.NOT_FOUND_404, content);
        }
    }

    public static void render(RoutingContext ctx, int status, String content) {
        HttpStatus.Code code = HttpStatus.getCode(status);
        String title = status + " " + (code != null ? code.getMessage() : "error");
        ctx.setStatus(status).put(HttpHeader.CONTENT_TYPE, "text/html")
           .write("<!DOCTYPE html>")
           .write("<html>")
           .write("<head>")
           .write("<title>")
           .write(title)
           .write("</title>")
           .write("</head>")
           .write("<body>")
           .write("<h1> " + title + " </h1>")
           .write("<p>" + content + "</p>")
           .write("<hr/>")
           .write("<footer><em>powered by Firefly " + Version.value + "</em></footer>")
           .write("</body>")
           .end("</html>");
    }

}

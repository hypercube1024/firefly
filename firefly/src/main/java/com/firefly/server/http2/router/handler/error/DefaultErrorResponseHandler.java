package com.firefly.server.http2.router.handler.error;

import com.firefly.Version;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.server.http2.router.RoutingContext;

import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
public class DefaultErrorResponseHandler extends AbstractErrorResponseHandler {

    public void render(RoutingContext ctx, int status, Throwable t) {
        HttpStatus.Code code = Optional.ofNullable(HttpStatus.getCode(status)).orElse(HttpStatus.Code.INTERNAL_SERVER_ERROR);
        String title = status + " " + code.getMessage();
        String content;
        switch (code) {
            case NOT_FOUND: {
                content = "The resource " + ctx.getURI().getPath() + " is not found";
            }
            break;
            case INTERNAL_SERVER_ERROR: {
                content = "The server internal error. <br/>" + (t != null ? t.getMessage() : "");
            }
            break;
            default: {
                content = title + "<br/>" + (t != null ? t.getMessage() : "");
            }
            break;
        }
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

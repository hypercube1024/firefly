package com.firefly.example.http.hello;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class ContextDataSharingDemo {
    public static void main(String[] args) {
        $.httpServer()
         .router().get("/data/foo")
         .handler(ctx -> {
             ctx.setAttribute("fooData", "I'm foo");
             ctx.write("foo sets an attribute").write("\r\n").next();
         })
         .router().get("/data/:other")
         .handler(ctx -> ctx.write((String) ctx.getAttribute("fooData"))
                            .write("\r\n")
                            .end(ctx.getRouterParameter("other") + " is coming"))
         .listen("localhost", 8080);
    }
}

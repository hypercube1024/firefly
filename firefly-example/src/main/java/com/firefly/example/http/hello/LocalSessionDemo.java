package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.codec.http2.model.Cookie;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.router.HTTPSession;
import com.firefly.server.http2.router.handler.session.HTTPSessionConfiguration;
import com.firefly.server.http2.router.handler.session.LocalHTTPSessionHandler;

import java.util.List;
import java.util.concurrent.Phaser;

/**
 * @author Pengtao Qiu
 */
public class LocalSessionDemo {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 8081;
        String uri = "https://" + host + ":" + port;

        int maxGetSession = 3;
        Phaser phaser = new Phaser(1 + maxGetSession + 2);

        HTTP2ServerBuilder httpsServer = $.httpsServer();
        LocalHTTPSessionHandler sessionHandler = new LocalHTTPSessionHandler(new HTTPSessionConfiguration());
        httpsServer.router().path("*").handler(sessionHandler)
                   .router().post("/session/:name")
                   .handler(ctx -> {
                       String name = ctx.getRouterParameter("name");
                       System.out.println("the path param -> " + name);
                       HTTPSession session = ctx.getSessionNow(true);
                       session.getAttributes().put(name, "bar");
                       // 1 second later, the session will expire
                       session.setMaxInactiveInterval(1);
                       ctx.updateSessionNow(session);
                       ctx.end("create session success");
                   })
                   .router().get("/session/:name")
                   .handler(ctx -> {
                       HTTPSession session = ctx.getSessionNow();
                       if (session != null) {
                           ctx.end("session value is " + session.getAttributes().get("foo"));
                       } else {
                           ctx.end("session is invalid");
                       }
                   })
                   .listen(host, port);

        List<Cookie> c = $.httpsClient().post(uri + "/session/foo").submit()
                          .thenApply(res -> {
                              List<Cookie> cookies = res.getCookies();
                              System.out.println(res.getStatus());
                              System.out.println(cookies);
                              System.out.println(res.getStringBody());
                              return cookies;
                          })
                          .thenApply(cookies -> {
                              for (int i = 0; i < maxGetSession; i++) {
                                  $.httpsClient().get(uri + "/session/foo").cookies(cookies).submit()
                                   .thenAccept(res2 -> {
                                       String sessionFoo = res2.getStringBody();
                                       System.out.println(sessionFoo);
                                       phaser.arrive();
                                   });
                              }
                              return cookies;
                          }).get();

        $.httpsClient().get(uri + "/resource-not-found").submit()
         .thenAccept(res -> {
             System.out.println(res.getStatus());
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        $.thread.sleep(3000L); // the session expired
        $.httpsClient().get(uri + "/session/foo").cookies(c).submit()
         .thenAccept(res -> {
             String sessionFoo = res.getStringBody();
             System.out.println(sessionFoo);
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpsServer.stop();
        $.httpsClient().stop();
        sessionHandler.stop();
    }
}

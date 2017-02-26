package test.http.router.handler.session;

import com.firefly.$;
import com.firefly.codec.http2.model.Cookie;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.router.handler.error.DefaultErrorResponseHandler;
import com.firefly.server.http2.router.handler.session.HTTPSessionConfiguration;
import com.firefly.server.http2.router.handler.session.LocalHTTPSessionHandler;
import org.junit.Assert;
import org.junit.Test;
import test.http.router.handler.AbstractHTTPHandlerTest;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestLocalHTTPSessionHandler extends AbstractHTTPHandlerTest {

    @Test
    public void test() throws Exception {
        int maxGetSession = 3;
        Phaser phaser = new Phaser(1 + maxGetSession);
        HTTP2ServerBuilder httpsServer = $.httpsServer();
        LocalHTTPSessionHandler sessionHandler = new LocalHTTPSessionHandler(new HTTPSessionConfiguration());
        httpsServer.router().path("*").handler(sessionHandler)
                   .router().path("*").handler(new DefaultErrorResponseHandler())
                   .router().post("/session/:name")
                   .handler(ctx -> {
                       String name = ctx.getRouterParameter("name");
                       System.out.println("the path param -> " + name);
                       Assert.assertThat(name, is("foo"));
                       HttpSession session = ctx.getSession(true);
                       session.setAttribute(name, "bar");
                       session.setMaxInactiveInterval(1); // 1 second later, the session will expire
                       ctx.end("create session success");
                   })
                   .router().get("/session/:name")
                   .handler(ctx -> {
                       String name = ctx.getRouterParameter("name");
                       Assert.assertThat(name, is("foo"));
                       HttpSession session = ctx.getSession();
                       if (session != null) {
                           Assert.assertThat(session.getAttribute("foo"), is("bar"));
                           ctx.end("session value is " + session.getAttribute("foo"));
                       } else {
                           ctx.end("session is invalid");
                       }
                   })
                   .listen(host, port);

        $.httpsClient().post(uri + "/session/foo").submit()
         .thenApply(res -> {
             List<Cookie> cookies = res.getCookies();
             System.out.println(res.getStatus());
             System.out.println(cookies);
             System.out.println(res.getStringBody());
             Assert.assertThat(res.getStringBody(), is("create session success"));
             return cookies;
         })
         .thenApply(cookies -> {
             for (int i = 0; i < maxGetSession; i++) {
                 $.httpsClient().get(uri + "/session/foo").cookies(cookies).submit()
                  .thenAccept(res2 -> {
                      String sessionFoo = res2.getStringBody();
                      System.out.println(sessionFoo);
                      Assert.assertThat(sessionFoo, is("session value is bar"));
                      phaser.arrive();
                  });
             }
             return cookies;
         });

        phaser.arriveAndAwaitAdvance();
        httpsServer.stop();
        $.httpsClient().stop();
        sessionHandler.stop();
    }
}

package test.http.router.handler.session;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.model.Cookie;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.router.HTTPSession;
import com.firefly.server.http2.router.handler.session.HTTPSessionConfiguration;
import com.firefly.server.http2.router.handler.session.LocalHTTPSessionHandler;
import org.junit.Assert;
import org.junit.Test;
import test.http.router.handler.AbstractHTTPHandlerTest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Pengtao Qiu
 */
public class TestLocalHTTPSessionHandler extends AbstractHTTPHandlerTest {

    @Test
    public void test() throws Exception {
        int maxGetSession = 3;
        Phaser phaser = new Phaser(1 + maxGetSession);
        HTTP2ServerBuilder s = $.httpsServer();
        SimpleHTTPClient c = $.createHTTPsClient();
        LocalHTTPSessionHandler sessionHandler = new LocalHTTPSessionHandler(new HTTPSessionConfiguration());

        s.router().path("*").handler(sessionHandler)
         .router().post("/createSession").handler(ctx -> {
             try {
                 HTTPSession session = ctx.createSession(20, ".fireflysource.com").get();
                 ctx.updateSessionNow(session);
                 ctx.end("ok");
             } catch (Exception e) {
                 e.printStackTrace();
            }
        })
         .router().post("/session/:name").handler(ctx -> {
            String name = ctx.getRouterParameter("name");
            System.out.println("the path param -> " + name);
            Assert.assertThat(name, is("foo"));
            HTTPSession session = ctx.getSessionNow();
            session.getAttributes().put(name, "bar");
            session.setMaxInactiveInterval(60);
            ctx.updateSessionNow(session);
            ctx.end("create session success");
        })
         .router().get("/session/:name").handler(ctx -> {
            String name = ctx.getRouterParameter("name");
            Assert.assertThat(name, is("foo"));
            HTTPSession session = ctx.getSessionNow();
            if (session != null) {
                Assert.assertThat(session.getAttributes().get("foo"), is("bar"));
                ctx.end("session value is " + session.getAttributes().get("foo"));
            } else {
                ctx.end("session is invalid");
            }
        })
         .listen(host, port);

        c.post(uri + "/session/foo").submit()
         .thenApply(res -> {
             List<Cookie> cookies = res.getCookies();
             System.out.println(res.getStatus());
             System.out.println(cookies);
             Optional<Cookie> sessionOpt = cookies.stream().filter(co -> co.getName().equals("jsessionid")).findFirst();
             Assert.assertThat(sessionOpt.isPresent(), is(true));

             System.out.println(res.getStringBody());
             Assert.assertThat(res.getStringBody(), is("create session success"));
             return cookies;
         })
         .thenApply(cookies -> {
             for (int i = 0; i < maxGetSession; i++) {
                 $.httpsClient().get(uri + "/session/foo").cookies(cookies).submit()
                  .thenAccept(res2 -> {
                      List<Cookie> cookies2 = res2.getCookies();
                      System.out.println(res2.getStatus());
                      System.out.println(cookies2);
                      Optional<Cookie> sessionOpt = cookies2.stream().filter(co -> co.getName().equals("jsessionid")).findFirst();
                      Assert.assertThat(sessionOpt.isPresent(), is(false));

                      String sessionFoo = res2.getStringBody();
                      System.out.println(sessionFoo);
                      Assert.assertThat(sessionFoo, is("session value is bar"));
                      phaser.arrive();
                  });
             }
             return cookies;
         });
        phaser.arriveAndAwaitAdvance();

        // test create session
        SimpleResponse fooResp = c.post(uri + "/session/foo").submit().get();
        List<Cookie> cookies = fooResp.getCookies();
        System.out.println(fooResp.getStatus());
        System.out.println(cookies);
        Optional<Cookie> sessionOpt = cookies.stream().filter(co -> co.getName().equals("jsessionid")).findFirst();
        Assert.assertThat(sessionOpt.isPresent(), is(true));
        Assert.assertThat(sessionOpt.get().getDomain(), nullValue());
        Assert.assertThat(sessionOpt.get().getMaxAge(), is(-1));

        System.out.println(fooResp.getStringBody());
        Assert.assertThat(fooResp.getStringBody(), is("create session success"));

        SimpleResponse resp = c.post(uri + "/createSession").cookies(cookies).submit().get();
        cookies = resp.getCookies();
        System.out.println(resp.getStatus());
        System.out.println(cookies);
        sessionOpt = cookies.stream().filter(co -> co.getName().equals("jsessionid")).findFirst();
        Assert.assertThat(sessionOpt.isPresent(), is(true));
        Assert.assertThat(sessionOpt.get().getDomain(), is(".fireflysource.com"));
        Assert.assertThat(sessionOpt.get().getMaxAge(), is(20));

        s.stop();
        c.stop();
        sessionHandler.stop();
    }
}

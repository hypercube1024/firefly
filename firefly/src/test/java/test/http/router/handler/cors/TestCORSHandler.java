package test.http.router.handler.cors;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpMethod;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.router.handler.cors.CORSConfiguration;
import com.firefly.server.http2.router.handler.cors.CORSHandler;
import com.firefly.utils.json.JsonObject;
import org.junit.Assert;
import org.junit.Test;
import test.http.router.handler.AbstractHTTPHandlerTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestCORSHandler extends AbstractHTTPHandlerTest {

    @Test
    public void test() throws Exception {
        CORSConfiguration config = new CORSConfiguration();
        config.setAllowOrigins(new HashSet<>(Arrays.asList("http://foo.com", "http://bar.com")));
        config.setExposeHeaders(Arrays.asList("a1", "a2"));
        config.setAllowHeaders(new HashSet<>(Arrays.asList("a1", "a2", "a3", "a4")));
        CORSHandler corsHandler = new CORSHandler();
        corsHandler.setConfiguration(config);

        HTTP2ServerBuilder s = $.httpServer();
        SimpleHTTPClient c = $.createHTTPClient();

        s.router().path("/cors/*").handler(corsHandler)
         .router().path("/cors/foo").handler(ctx -> ctx.end("foo"))
         .router().path("/cors/bar").handler(ctx -> {
            JsonObject jsonObject = ctx.getJsonObjectBody();
            Map<String, Object> map = new HashMap<>(jsonObject);
            map.put("bar", "x1");
            ctx.writeJson(map).end();
        })
         .listen(host, port);

        SimpleResponse resp = c.get(uri + "/cors/foo")
                               .put(HttpHeader.ORIGIN, "http://foo.com")
                               .put(HttpHeader.HOST, "foo.com")
                               .submit().get(2, TimeUnit.SECONDS);
        Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN), is("http://foo.com"));
        Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_EXPOSE_HEADERS), is("a1, a2"));
        Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS), is("true"));

        resp = c.request(HttpMethod.OPTIONS, uri + "/cors/bar")
                .put(HttpHeader.ORIGIN, "http://bar.com")
                .put(HttpHeader.HOST, "bar.com")
                .put(HttpHeader.ACCESS_CONTROL_REQUEST_METHOD, "GET, POST, PUT, DELETE")
                .put(HttpHeader.ACCESS_CONTROL_REQUEST_HEADERS, "a2, a3, a4")
                .put("a2", "foo_a2")
                .submit().get(2, TimeUnit.SECONDS);
        Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN), is("http://bar.com"));
        Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_CREDENTIALS), is("true"));
        System.out.println(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS));
        System.out.println(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS));
        Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_METHODS).contains("DELETE"), is(true));
        Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_ALLOW_HEADERS).contains("a2"), is(true));
        Assert.assertThat(resp.getFields().get(HttpHeader.ACCESS_CONTROL_MAX_AGE), is("86400"));

        c.stop();
        s.stop();
    }
}

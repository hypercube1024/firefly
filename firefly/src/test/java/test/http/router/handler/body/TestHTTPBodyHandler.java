package test.http.router.handler.body;

import com.firefly.$;
import com.firefly.codec.http2.encode.UrlEncoded;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.utils.concurrent.Promise;
import org.junit.Assert;
import org.junit.Test;
import test.http.router.handler.AbstractHTTPHandlerTest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestHTTPBodyHandler extends AbstractHTTPHandlerTest {

    @Test
    public void testPostData() {
        StringBuilder bigData = new StringBuilder();
        int dataSize = 1024 * 1024;
        for (int i = 0; i < dataSize; i++) {
            bigData.append(i);
        }
        byte[] data = $.string.getBytes(bigData.toString());
        System.out.println("data len: " + data.length);

        Phaser phaser = new Phaser(5);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().post("/data").handler(ctx -> {
            // small data test case
            System.out.println(ctx.getStringBody());
            Assert.assertThat(ctx.getStringBody(), is("test post data"));
            ctx.end("server received data");
            phaser.arrive();
        }).router().post("/bigData").handler(ctx -> {
            // big data test case
            System.out.println("receive big data size: " + ctx.getContentLength());
            Assert.assertThat((int) ctx.getContentLength(), is(data.length));
            Assert.assertThat($.io.toString(ctx.getInputStream()), is(bigData.toString()));
            $.io.close(ctx.getInputStream());
            ctx.end("server received big data");
            phaser.arrive();
        }).listen(host, port);

        $.httpClient().post(uri + "/data").body("test post data").submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("server received data"));
             phaser.arrive();
         });

        // post big data with content length
        $.httpClient().post(uri + "/bigData").put(HttpHeader.CONTENT_LENGTH, data.length + "")
         .write(ByteBuffer.wrap(data))
         .submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("server received big data"));
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }

    @Test
    public void testPostBigDataUsingChunkedEncoding() {
        StringBuilder bigData = new StringBuilder();
        int dataSize = 1024 * 1024;
        for (int i = 0; i < dataSize; i++) {
            bigData.append(i);
        }
        byte[] data = $.string.getBytes(bigData.toString());
        System.out.println("data len: " + data.length);

        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().post("/bigData").handler(ctx -> {
            // big data test case
            System.out.println("receive big data size: " + ctx.getContentLength());
            Assert.assertThat($.io.toString(ctx.getInputStream()), is(bigData.toString()));
            $.io.close(ctx.getInputStream());
            ctx.end("server received big data");
            phaser.arrive();
        }).listen(host, port);

        // post big data using chunked encoding
        List<ByteBuffer> buffers = $.buffer.split(ByteBuffer.wrap(data), 4 * 1024);
        Promise.Completable<HTTPOutputStream> promise = new Promise.Completable<>();
        promise.thenAccept(output -> {
            try (HTTPOutputStream out = output) {
                for (ByteBuffer buf : buffers) {
                    out.write(buf);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        $.httpClient().post(uri + "/bigData").output(promise)
         .submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("server received big data"));
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }

    @Test
    public void testPostForm() {
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().post("/content/form").handler(ctx -> {
            Assert.assertThat(ctx.getParameter("name"), is("你的名字"));
            Assert.assertThat(ctx.getParameter("intro"), is("我要送些东西给你 我的孩子 因为我们同是漂泊在世界的溪流中的"));
            ctx.end("server received form data");
            phaser.arrive();
        }).listen(host, port);

        $.httpClient().post(uri + "/content/form")
         .putFormParam("name", "你的名字")
         .putFormParam("intro", "我要送些东西给你 我的孩子 因为我们同是漂泊在世界的溪流中的")
         .submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("server received form data"));
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }

    @Test
    public void testQueryParam() {
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().get("/query").handler(ctx -> {
            Assert.assertThat(ctx.getParameter("name"), is("你的名字"));
            Assert.assertThat(ctx.getParameter("intro"), is("我要送些东西给你 我的孩子 因为我们同是漂泊在世界的溪流中的"));
            ctx.end("server received form data");
            phaser.arrive();
        }).listen(host, port);

        UrlEncoded enc = $.uri.encode();
        enc.put("name", "你的名字");
        enc.put("intro", "我要送些东西给你 我的孩子 因为我们同是漂泊在世界的溪流中的");
        $.httpClient().get(uri + "/query?" + enc.encode(StandardCharsets.UTF_8, true))
         .submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("server received form data"));
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }

    @Test
    public void testGetWithBody() {
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().get("/queryWithBody").handler(ctx -> {
            String body = ctx.getStringBody();
            System.out.println(body);
            Assert.assertThat(body, is("Get request with body"));
            ctx.end("receive: " + body);
            phaser.arrive();
        }).listen(host, port);

        $.httpClient().get(uri + "/queryWithBody").body("Get request with body")
         .submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("receive: Get request with body"));
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }

}

package test.http.router.handler.body;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.utils.StringUtils;
import com.firefly.utils.lang.URIUtils;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestHTTPBodyHandler {

    @Test
    public void testPostData() {
        String host = "localhost";
        int port = 8083;
        StringBuilder uri = URIUtils.newURIBuilder("http", host, port);
        System.out.println(uri);
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().post("/data").handler(ctx -> {
            System.out.println(ctx.getStringBody());
            Assert.assertThat(ctx.getStringBody(), is("test post data"));
            ctx.end("server received data");
            phaser.arrive();
        }).listen(host, port);

        $.httpClient().post(uri + "/data").body("test post data").submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("server received data"));
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }

    @Test
    public void testPostBigData() {
        String host = "localhost";
        int port = 8084;
        StringBuilder uri = URIUtils.newURIBuilder("http", host, port);
        System.out.println(uri);
        Phaser phaser = new Phaser(3);

        StringBuilder bigData = new StringBuilder();
        int dataSize = 1024 * 1024;
        for (int i = 0; i < dataSize; i++) {
            bigData.append(i);
        }
        byte[] data = StringUtils.getBytes(bigData.toString());
        System.out.println("data len: " + data.length);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().post("/bigData").handler(ctx -> {
            System.out.println("receive big data size: " + ctx.getContentLength());
            Assert.assertThat((int) ctx.getContentLength(), is(data.length));
            Assert.assertThat($.io.toString(ctx.getInputStream()), is(bigData.toString()));
            $.io.close(ctx.getInputStream());
            ctx.end("server received big data");
            phaser.arrive();
        }).listen(host, port);

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
}

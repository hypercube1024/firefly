package test.http.router.handler.body;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.model.MultiPartContentProvider;
import com.firefly.codec.http2.model.StringContentProvider;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestMultiPartContentProvider {

    @Test
    public void test() {
        MultiPartContentProvider multiPartProvider = new MultiPartContentProvider();
        System.out.println(multiPartProvider.getContentType());
        multiPartProvider.addFieldPart("test1", new StringContentProvider("hello multi part1"), null);
        multiPartProvider.addFieldPart("test2", new StringContentProvider("hello multi part2"), null);
        multiPartProvider.close();
        multiPartProvider.setListener(() -> System.out.println("on content"));

        List<ByteBuffer> list = new ArrayList<>();
        for (ByteBuffer buf : multiPartProvider) {
            list.add(buf);
        }
        String value = BufferUtils.toString(list);
        System.out.println(value);
        System.out.println(multiPartProvider.getLength());

        Assert.assertThat(multiPartProvider.getLength(), greaterThan(0L));
    }

    @Test
    public void testStringMultiPart() {
        String host = "localhost";
        int port = 8084;
        StringBuilder uri = $.uri.newURIBuilder("http", host, port);
        System.out.println(uri);

        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().post("/file/upload").handler(ctx -> {
            // small multi part data test case
            Assert.assertThat(ctx.getParts().size(), is(2));
            Part test1 = ctx.getPart("test1");
            Part test2 = ctx.getPart("test2");
            try (InputStream input1 = test1.getInputStream();
                 InputStream input2 = test2.getInputStream()) {
                String value = $.io.toString(input1);
                System.out.println(value);
                Assert.assertThat(value, is("hello multi part1"));

                String value2 = $.io.toString(input2);
                System.out.println(value2);
                Assert.assertThat(value2, is("hello multi part2"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            ctx.end("server received multi part data");
            phaser.arrive();
        }).listen(host, port);

        $.httpClient().post(uri + "/file/upload")
         .addFieldPart("test1", new StringContentProvider("hello multi part1"), null)
         .addFieldPart("test2", new StringContentProvider("hello multi part2"), null)
         .submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("server received multi part data"));
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }
}

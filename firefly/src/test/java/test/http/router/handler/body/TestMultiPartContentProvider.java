package test.http.router.handler.body;

import com.firefly.$;
import com.firefly.codec.http2.model.*;
import com.firefly.server.http2.HTTP2ServerBuilder;
import org.junit.Assert;
import org.junit.Test;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.*;

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
        String value = $.buffer.toString(list);
        System.out.println(value);
        System.out.println(multiPartProvider.getLength());

        Assert.assertThat(multiPartProvider.getLength(), greaterThan(0L));
    }

    @Test
    public void testInputStreamContent() {
        InputStream inputStream = $.class.getResourceAsStream("/poem.txt");
        InputStreamContentProvider inputStreamContentProvider = new InputStreamContentProvider(inputStream);
        MultiPartContentProvider multiPartProvider = new MultiPartContentProvider();
        System.out.println(multiPartProvider.getContentType());
        multiPartProvider.addFilePart("poetry", "poem.txt", inputStreamContentProvider, null);

        multiPartProvider.close();
        multiPartProvider.setListener(() -> System.out.println("on content"));

        List<ByteBuffer> list = new ArrayList<>();
        for (ByteBuffer buf : multiPartProvider) {
            list.add(buf);
        }
        String value = $.buffer.toString(list);
        Assert.assertThat(value.length(), greaterThan(0));
        System.out.println(multiPartProvider.getLength());
        Assert.assertThat(multiPartProvider.getLength(), lessThan(0L));
    }

    @Test
    public void testPathContent() throws URISyntaxException, IOException {
        Path path = Paths.get($.class.getResource("/poem.txt").toURI());
        System.out.println(path.toAbsolutePath());
        PathContentProvider pathContentProvider = new PathContentProvider(path);
        MultiPartContentProvider multiPartProvider = new MultiPartContentProvider();
        multiPartProvider.addFilePart("poetry", "poem.txt", pathContentProvider, null);

        multiPartProvider.close();
        multiPartProvider.setListener(() -> System.out.println("on content"));

        List<ByteBuffer> list = new ArrayList<>();
        for (ByteBuffer buf : multiPartProvider) {
            list.add(buf);
        }
        System.out.println(multiPartProvider.getLength());
        Assert.assertThat(multiPartProvider.getLength(), greaterThan(0L));
        Assert.assertThat(multiPartProvider.getLength(), is($.buffer.remaining(list)));
    }

    @Test
    public void testMultiPart() {
        String host = "localhost";
        int port = 8084;
        StringBuilder uri = $.uri.newURIBuilder("http", host, port);
        System.out.println(uri);

        Phaser phaser = new Phaser(5);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().post("/upload/string").handler(ctx -> {
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
        }).router().post("/upload/poetry").handler(ctx -> {
            // upload poetry
            System.out.println(ctx.getFields());
            Part poetry = ctx.getPart("poetry");
            Assert.assertThat(poetry.getSubmittedFileName(), is("poem.txt"));
            try (InputStream inputStream = $.class.getResourceAsStream("/poem.txt");
                 InputStream in = poetry.getInputStream()) {
                String poem = $.io.toString(inputStream);
                System.out.println(poem);
                Assert.assertThat(poem, is($.io.toString(in)));
            } catch (IOException e) {
                e.printStackTrace();
            }
            ctx.end("server received poetry");
            phaser.arrive();
        }).listen(host, port);

        $.httpClient().post(uri + "/upload/string")
         .addFieldPart("test1", new StringContentProvider("hello multi part1"), null)
         .addFieldPart("test2", new StringContentProvider("hello multi part2"), null)
         .submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("server received multi part data"));
             phaser.arrive();
         });

        InputStream inputStream = $.class.getResourceAsStream("/poem.txt");
        InputStreamContentProvider inputStreamContentProvider = new InputStreamContentProvider(inputStream);
        $.httpClient().post(uri + "/upload/poetry")
         .addFilePart("poetry", "poem.txt", inputStreamContentProvider, null)
         .submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("server received poetry"));
             $.io.close(inputStreamContentProvider);
             $.io.close(inputStream);
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }
}

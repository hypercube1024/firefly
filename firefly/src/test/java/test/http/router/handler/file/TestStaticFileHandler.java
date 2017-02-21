package test.http.router.handler.file;

import com.firefly.$;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.router.handler.file.StaticFileHandler;
import org.junit.Assert;
import org.junit.Test;
import test.http.router.handler.AbstractHTTPHandlerTest;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestStaticFileHandler extends AbstractHTTPHandlerTest {

    @Test
    public void test() throws URISyntaxException {
        Phaser phaser = new Phaser(4);

        HTTP2ServerBuilder httpServer = $.httpServer();
        Path path = Paths.get(TestStaticFileHandler.class.getResource("/").toURI());
        System.out.println(path.toAbsolutePath());
        StaticFileHandler staticFileHandler = new StaticFileHandler(path.toAbsolutePath().toString());
        httpServer.router().get("/static/*").handler(staticFileHandler).listen(host, port);

        $.httpClient().get(uri + "/static/hello.txt")
         .submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
             Assert.assertThat(res.getStringBody(), is("hello static file"));
             phaser.arrive();
         });

        $.httpClient().get(uri + "/static/hello.txt")
         .put(HttpHeader.RANGE, "bytes=10-16")
         .submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.PARTIAL_CONTENT_206));
             Assert.assertThat(res.getStringBody(), is("ic file"));
             phaser.arrive();
         });

        $.httpClient().get(uri + "/static/hello.txt")
         .put(HttpHeader.RANGE, "bytes=0-4,10-17")
         .submit()
         .thenAccept(res -> {
             Assert.assertThat(res.getStatus(), is(HttpStatus.PARTIAL_CONTENT_206));

             String boundary = $.string.split(res.getFields().get(HttpHeader.CONTENT_TYPE), ';')[1]
                     .trim().substring("boundary=".length());
             System.out.println(boundary);

             String state = "boundary";
             HttpFields fields = new HttpFields();
             long currentLen = 0L;
             long count = 0L;
             out:
             for (String row : $.string.split(res.getStringBody(), "\n")) {
                 String r = row.trim();
                 switch (state) {
                     case "boundary": {
                         if (r.equals("--" + boundary)) {
                             state = "head";
                         } else if (r.equals("--" + boundary + "--")) {
                             state = "end";
                         } else {
                             System.out.println("boundary format error");
                             break out;
                         }
                     }
                     break;
                     case "head": {
                         if (r.length() == 0) {
                             state = "content";
                         } else {
                             String[] s = $.string.split(r, ':');
                             String name = s[0].trim();
                             String value = s[1].trim();
                             fields.put(name, value);
                             if (name.equals(HttpHeader.CONTENT_RANGE.asString())) {
                                 String[] strings = $.string.split(value, ' ');
                                 String[] length = $.string.split(strings[1].trim(), '/');
                                 String[] range = $.string.split(length[0], '-');
                                 String unit = strings[0];
                                 long startPos = Long.parseLong(range[0]);
                                 long endPos = Long.parseLong(range[1]);
                                 long rangeLen = Long.parseLong(length[1]);

                                 Assert.assertThat(unit, is("bytes"));
                                 Assert.assertThat(rangeLen, is(17L));
                                 currentLen = endPos - startPos + 1;
                             }
                         }
                     }
                     break;
                     case "content": {
                         Assert.assertThat(fields.get(HttpHeader.CONTENT_TYPE), is("text/plain"));

                         count += r.getBytes().length;
                         if (count == currentLen) {
                             System.out.println(r);
                             state = "boundary";
                             fields = new HttpFields();
                             currentLen = 0L;
                             count = 0L;
                         }
                     }
                     break;
                     case "end": {
                         System.out.println("end");
                     }
                     break out;
                 }
             }
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }
}

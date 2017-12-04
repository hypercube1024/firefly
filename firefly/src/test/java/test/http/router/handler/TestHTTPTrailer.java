package test.http.router.handler;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.net.tcp.secure.openssl.DefaultOpenSSLSecureSessionFactory;
import com.firefly.server.http2.HTTP2ServerBuilder;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;
import com.firefly.utils.io.BufferUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Phaser;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Pengtao Qiu
 */
public class TestHTTPTrailer extends AbstractHTTPHandlerTest {

    @Test
    public void test() {
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        startHttpServer(httpServer);

        SimpleHTTPClient httpClient = $.createHTTPClient();
        testServerResponseTrailer(phaser, httpClient);
        testClientPostTrailer(phaser, httpClient);

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        httpClient.stop();
    }

    @Test
    public void testHttp2() {
        Phaser phaser = new Phaser(2);

        SimpleHTTPServerConfiguration serverConfiguration = new SimpleHTTPServerConfiguration();
        serverConfiguration.setSecureConnectionEnabled(true);
        HTTP2ServerBuilder httpsServer = $.httpServer(serverConfiguration);
        startHttpServer(httpsServer);

        SimpleHTTPClientConfiguration clientConfiguration = new SimpleHTTPClientConfiguration();
        clientConfiguration.setSecureConnectionEnabled(true);
        SimpleHTTPClient httpsClient = new SimpleHTTPClient(clientConfiguration);
        testServerResponseTrailer(phaser, httpsClient);
//        testClientPostTrailer(phaser, httpsClient);

        phaser.arriveAndAwaitAdvance();
        httpsServer.stop();
        httpsClient.stop();
    }

    private void testServerResponseTrailer(Phaser phaser, SimpleHTTPClient httpClient) {
        httpClient.get(uri + "/trailer").submit()
                  .thenAccept(res -> {
                      Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
                      Assert.assertThat(res.getFields().get(HttpHeader.CONTENT_TYPE), is("text/plain"));
                      System.out.println(res.getFields());
                      Assert.assertThat(res.getStringBody().length(), greaterThan(0));
                      System.out.println(res.getStringBody());
                      Assert.assertThat(res.getTrailerSupplier(), notNullValue());
                      HttpFields trailer = res.getTrailerSupplier().get();
                      Assert.assertThat(trailer.size(), greaterThan(0));
                      Assert.assertThat(trailer.get("Foo"), is("s1"));
                      Assert.assertThat(trailer.get("Bar"), is("s2"));
                      System.out.println(trailer);
                      phaser.arrive();
                  });
    }

    private void testClientPostTrailer(Phaser phaser, SimpleHTTPClient httpClient) {
        httpClient.post(uri + "/postTrailer").setTrailerSupplier(() -> {
            HttpFields trailer = new HttpFields();
            trailer.add("ok", "my trailer");
            return trailer;
        }).output(out -> {
            try (HTTPOutputStream output = out) {
                output.write(BufferUtils.toBuffer("hello"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).submit().thenAccept(res -> {
            Assert.assertThat(res.getStatus(), is(HttpStatus.OK_200));
            Assert.assertThat(res.getStringBody().length(), greaterThan(0));
            System.out.println(res.getStringBody());
            Assert.assertThat(res.getStringBody(), is("trailer : my trailer"));
            phaser.arrive();
        });
    }

    private void startHttpServer(HTTP2ServerBuilder httpServer) {
        httpServer.router().get("/trailer").handler(ctx -> {
            System.out.println("get request");
            ctx.put(HttpHeader.CONTENT_TYPE, "text/plain");
            ctx.getResponse().setTrailerSupplier(() -> {
                HttpFields trailer = new HttpFields();
                trailer.add("Foo", "s1");
                trailer.add("Bar", "s2");
                return trailer;
            });
            ctx.end("trailer test");
        }).router().post("/postTrailer").handler(ctx -> {
            System.out.println("post trailer");
            HttpFields trailer = ctx.getRequest().getTrailerSupplier().get();
            ctx.end("trailer : " + trailer.get("ok"));
        }).listen(host, port);
    }


}

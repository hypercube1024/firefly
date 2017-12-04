package test.http;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;
import com.firefly.server.http2.SimpleResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
@RunWith(Parameterized.class)
public class TestHTTPServerAndClient {

    @Parameter
    public Run r;

    static class Run {
        SimpleHTTPClientConfiguration clientConfig;
        SimpleHTTPServerConfiguration serverConfig;
        String requestURL;
        String quitURL;
        int port;
        int maxMsg;
        String testName;

        @Override
        public String toString() {
            return testName;
        }
    }

    @Parameters(name = "{0}")
    public static Collection<Run> data() {
        List<Run> data = new ArrayList<>();
        Run run = new Run();
        run.clientConfig = new SimpleHTTPClientConfiguration();
        run.serverConfig = new SimpleHTTPServerConfiguration();
        run.port = 1332;
        run.maxMsg = 5;
        run.requestURL = "http://localhost:" + run.port + "/";
        run.quitURL = "http://localhost:" + run.port + "/quit";
        run.testName = "Test HTTP server and client";
        data.add(run);

        run = new Run();
        run.clientConfig = new SimpleHTTPClientConfiguration();
        run.clientConfig.setSecureConnectionEnabled(true); // enable HTTPs
        run.serverConfig = new SimpleHTTPServerConfiguration();
        run.serverConfig.setSecureConnectionEnabled(true);
        run.port = 1333;
        run.maxMsg = 15;
        run.requestURL = "https://localhost:" + run.port + "/";
        run.quitURL = "https://localhost:" + run.port + "/quit";
        run.testName = "Test HTTPs server and client";
        data.add(run);

        return data;
    }

    @Test
    public void test() throws InterruptedException {
        SimpleHTTPServer server = $.createHTTPServer(r.serverConfig);
        SimpleHTTPClient client = $.createHTTPClient(r.clientConfig);
        int port = r.port;
        int maxMsg = r.maxMsg;
        CountDownLatch countDownLatch = new CountDownLatch(maxMsg + 1);

        AtomicInteger msgCount = new AtomicInteger();
        server.headerComplete(r -> r.messageComplete(request -> {
            SimpleResponse response = request.getResponse();
            String path = request.getURI().getPath();

            System.out.println("server receives message -> " + request.getStringBody());
            response.getFields().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.asString());
            switch (path) {
                case "/": {
                    msgCount.incrementAndGet();
                    try (PrintWriter writer = response.getPrintWriter()) {
                        writer.print("response message [" + request.getStringBody() + "]");
                    }
                }
                break;
                case "/quit": {
                    try (PrintWriter writer = response.getPrintWriter()) {
                        writer.print("bye!");
                    }
                }
                break;
            }
        })).listen("localhost", port);

        for (int i = 0; i < maxMsg; i++) {
            client.post(r.requestURL).body("hello world" + i + "!").submit().thenAcceptAsync(r -> {
                System.out.println("client receives message -> " + r.getStringBody());
                countDownLatch.countDown();
            });
        }
        client.post(r.quitURL).body("quit test").submit().thenAcceptAsync(r -> {
            System.out.println("client receives message -> " + r.getStringBody());
            countDownLatch.countDown();
        });

        countDownLatch.await();
        Assert.assertThat(msgCount.get(), is(maxMsg));
        client.stop();
        server.stop();
    }
}

package test.net.tcp;

import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.TcpServerConfiguration;
import com.firefly.net.tcp.codec.StringParser;
import com.firefly.utils.io.IO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
@RunWith(Parameterized.class)
public class TestSimpleTcpServerAndClient {

    @Parameter
    public Run r;

    static class Run {
        TcpConfiguration clientConfig;
        TcpServerConfiguration serverConfig;
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
        run.clientConfig = new TcpConfiguration();
        run.serverConfig = new TcpServerConfiguration();
        run.port = 1212;
        run.maxMsg = 5;
        run.testName = "Test TCP server and client";
        data.add(run);

        run = new Run();
        run.clientConfig = new TcpConfiguration();
        run.clientConfig.setSecureConnectionEnabled(true); // enable TLS
        run.serverConfig = new TcpServerConfiguration();
        run.serverConfig.setSecureConnectionEnabled(true);
        run.port = 1213;
        run.maxMsg = 20;
        run.testName = "Test TCP server and client with TLS";
        data.add(run);

        return data;
    }


    @Test
    public void test() {
        SimpleTcpClient client = new SimpleTcpClient(r.clientConfig);
        SimpleTcpServer server = new SimpleTcpServer(r.serverConfig);
        int port = r.port;
        int maxMsg = r.maxMsg;
        Phaser phaser = new Phaser(3);

        server.accept(connection -> {
            StringParser parser = new StringParser();
            AtomicInteger msgCount = new AtomicInteger();
            parser.complete(message -> {
                String s = message.trim();
                System.out.println("server receives message -> " + s);
                switch (s) {
                    case "quit":
                        connection.write("bye!\r\n");
                        IO.close(connection);
                        Assert.assertThat(msgCount.get(), is(maxMsg));
                        phaser.arrive();
                        break;
                    default:
                        msgCount.incrementAndGet();
                        connection.write("response message [" + s + "]\r\n");
                        break;
                }
            });
            connection.receive(parser::receive);
        }).listen("localhost", port);


        client.connect("localhost", port)
              .thenAccept(c -> {
                  StringParser parser = new StringParser();
                  AtomicInteger msgCount = new AtomicInteger();
                  parser.complete(message -> {
                      String s = message.trim();
                      System.out.println("client receives message -> " + s);
                      switch (s) {
                          case "bye!":
                              Assert.assertThat(msgCount.get(), is(maxMsg));
                              phaser.arrive();
                              break;
                          default:
                              msgCount.incrementAndGet();
                              break;
                      }
                  });
                  c.receive(parser::receive);
                  for (int i = 0; i < maxMsg; i++) {
                      c.write("hello world" + i + "!\r\n");
                  }
                  c.write("quit\r\n");
              });

        phaser.arriveAndAwaitAdvance();
        client.stop();
        server.stop();
    }
}

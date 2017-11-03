package test.net.tcp;

import com.firefly.net.SSLContextFactory;
import com.firefly.net.SecureSessionFactory;
import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.TcpServerConfiguration;
import com.firefly.net.tcp.codec.StringParser;
import com.firefly.net.tcp.secure.FileJdkSSLContextFactory;
import com.firefly.net.tcp.secure.JdkSecureSessionFactory;
import com.firefly.utils.io.ClassPathResource;
import com.firefly.utils.io.IO;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.is;

/**
 * @author Pengtao Qiu
 */
public class TestFileSSLContextFactory {

    @Test
    public void test() throws IOException {
        SecureSessionFactory secureSessionFactory = createSecureSessionFactory();
        SimpleTcpClient client = createSimpleTcpClient(secureSessionFactory);
        SimpleTcpServer server = createSimpleTcpServer(secureSessionFactory);

        int port = 1214;
        int maxMsg = 5;
        Phaser phaser = new Phaser(3);

        server.accept(connection -> {
            StringParser parser = new StringParser();
            AtomicInteger msgCount = new AtomicInteger();
            parser.complete(message -> {
                String s = message.trim();
                System.out.println("[file ssl factory] server receives message -> " + s);
                switch (s) {
                    case "quit":
                        connection.write("file ssl session bye!\r\n");
                        IO.close(connection);
                        Assert.assertThat(msgCount.get(), is(maxMsg));
                        phaser.arrive();
                        break;
                    default:
                        msgCount.incrementAndGet();
                        connection.write("test file ssl factory message [" + s + "]\r\n");
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
                      System.out.println("[file ssl factory] client receives message -> " + s);
                      switch (s) {
                          case "file ssl session bye!":
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

    private SimpleTcpServer createSimpleTcpServer(SecureSessionFactory secureSessionFactory) {
        TcpServerConfiguration serverConfiguration = new TcpServerConfiguration();
        serverConfiguration.setSecureSessionFactory(secureSessionFactory);
        serverConfiguration.setSecureConnectionEnabled(true);
        return new SimpleTcpServer(serverConfiguration);
    }

    private SimpleTcpClient createSimpleTcpClient(SecureSessionFactory secureSessionFactory) {
        TcpConfiguration configuration = new TcpConfiguration();
        configuration.setSecureSessionFactory(secureSessionFactory);
        configuration.setSecureConnectionEnabled(true);
        return new SimpleTcpClient(configuration);
    }

    private SecureSessionFactory createSecureSessionFactory() throws IOException {
        ClassPathResource pathResource = new ClassPathResource("/fireflySecureKeys.jks");
        System.out.println(pathResource.getFile().getAbsolutePath());
        SSLContextFactory factory = new FileJdkSSLContextFactory(pathResource.getFile().getAbsolutePath(),
                "123456", "654321");
        return new JdkSecureSessionFactory(factory, factory);
    }
}

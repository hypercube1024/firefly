package test.net.tcp;

import com.firefly.net.SSLContextFactory;
import com.firefly.net.SecureSessionFactory;
import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.TcpServerConfiguration;
import com.firefly.net.tcp.codec.StringParser;
import com.firefly.net.tcp.secure.conscrypt.ConscryptSecureSessionFactory;
import com.firefly.net.tcp.secure.jdk.FileJdkSSLContextFactory;
import com.firefly.net.tcp.secure.jdk.JdkSecureSessionFactory;
import com.firefly.net.tcp.secure.openssl.DefaultOpenSSLSecureSessionFactory;
import com.firefly.net.tcp.secure.openssl.FileCertificateOpenSSLSecureSessionFactory;
import com.firefly.net.tcp.secure.openssl.SelfSignedCertificateOpenSSLSecureSessionFactory;
import com.firefly.utils.RandomUtils;
import com.firefly.utils.io.ClassPathResource;
import com.firefly.utils.io.IO;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
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
    public static Collection<Run> data() throws IOException {
        List<Run> data = new ArrayList<>();
        Run run = new Run();
        run.clientConfig = new TcpConfiguration();
        run.serverConfig = new TcpServerConfiguration();
        run.port = (int) RandomUtils.random(1000, 65534);
        run.maxMsg = 5;
        run.testName = "Test the plaintext";
        data.add(run);

        run = new Run();
        run.clientConfig = new TcpConfiguration();
        run.clientConfig.setSecureSessionFactory(new JdkSecureSessionFactory());
        run.clientConfig.setSecureConnectionEnabled(true);
        run.serverConfig = new TcpServerConfiguration();
        run.serverConfig.setSecureConnectionEnabled(true);
        run.serverConfig.setSecureSessionFactory(new JdkSecureSessionFactory());
        run.port = (int) RandomUtils.random(1000, 65534);
        run.maxMsg = 20;
        run.testName = "Test jdk self signed certificate";
        data.add(run);

        run = new Run();
        run.clientConfig = new TcpConfiguration();
        run.clientConfig.setSecureSessionFactory(createJDKFileSecureSessionFactory());
        run.clientConfig.setSecureConnectionEnabled(true);
        run.serverConfig = new TcpServerConfiguration();
        run.serverConfig.setSecureConnectionEnabled(true);
        run.serverConfig.setSecureSessionFactory(createJDKFileSecureSessionFactory());
        run.port = (int) RandomUtils.random(1000, 65534);
        run.maxMsg = 20;
        run.testName = "Test jdk file certificate";
        data.add(run);

        run = new Run();
        run.clientConfig = new TcpConfiguration();
        run.clientConfig.setSecureSessionFactory(new ConscryptSecureSessionFactory());
        run.clientConfig.setSecureConnectionEnabled(true);
        run.serverConfig = new TcpServerConfiguration();
        run.serverConfig.setSecureConnectionEnabled(true);
        run.serverConfig.setSecureSessionFactory(new ConscryptSecureSessionFactory());
        run.port = (int) RandomUtils.random(1000, 65534);
        run.maxMsg = 20;
        run.testName = "Test conscrypt self signed certificate";
        data.add(run);

        run = new Run();
        run.clientConfig = new TcpConfiguration();
        run.clientConfig.setSecureConnectionEnabled(true);
        run.clientConfig.setSecureSessionFactory(new SelfSignedCertificateOpenSSLSecureSessionFactory());
        run.serverConfig = new TcpServerConfiguration();
        run.serverConfig.setSecureConnectionEnabled(true);
        run.serverConfig.setSecureSessionFactory(new SelfSignedCertificateOpenSSLSecureSessionFactory());
        run.port = (int) RandomUtils.random(1000, 65534);
        run.maxMsg = 20;
        run.testName = "Test openssl self signed certificate";
        data.add(run);

        run = new Run();
        run.clientConfig = new TcpConfiguration();
        run.clientConfig.setSecureConnectionEnabled(true);
        run.clientConfig.setSecureSessionFactory(new DefaultOpenSSLSecureSessionFactory());
        run.serverConfig = new TcpServerConfiguration();
        run.serverConfig.setSecureConnectionEnabled(true);
        run.serverConfig.setSecureSessionFactory(new DefaultOpenSSLSecureSessionFactory());
        run.port = (int) RandomUtils.random(1000, 65534);
        run.maxMsg = 20;
        run.testName = "Test openssl default certificate";
        data.add(run);

        run = new Run();
        run.clientConfig = new TcpConfiguration();
        run.clientConfig.setSecureConnectionEnabled(true);
        run.clientConfig.setSecureSessionFactory(createOpenSSLFileSecureSessionFactory());
        run.serverConfig = new TcpServerConfiguration();
        run.serverConfig.setSecureConnectionEnabled(true);
        run.serverConfig.setSecureSessionFactory(createOpenSSLFileSecureSessionFactory());
        run.port = (int) RandomUtils.random(1000, 65534);
        run.maxMsg = 20;
        run.testName = "Test openssl self signed certificate";
        data.add(run);

        return data;
    }

    private static SecureSessionFactory createJDKFileSecureSessionFactory() throws IOException {
        ClassPathResource pathResource = new ClassPathResource("/fireflySecureKeys.jks");
        System.out.println(pathResource.getFile().getAbsolutePath());
        SSLContextFactory factory = new FileJdkSSLContextFactory(pathResource.getFile().getAbsolutePath(),
                "123456", "654321");
        return new JdkSecureSessionFactory(factory, factory);
    }

    private static SecureSessionFactory createOpenSSLFileSecureSessionFactory() throws IOException {
        ClassPathResource certificate = new ClassPathResource("/myCA.cer");
        ClassPathResource privateKey = new ClassPathResource("/myCAPriv8.key");
        return new FileCertificateOpenSSLSecureSessionFactory(certificate.getFile().getAbsolutePath(), privateKey.getFile().getAbsolutePath());
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

package test.net.tcp;

import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.TcpConnection;
import com.firefly.net.tcp.codec.CharParser;
import com.firefly.net.tcp.codec.DelimiterParser;
import com.firefly.net.tcp.codec.StringParser;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action1;

public class ClientDemo1 {

    public static void main(String[] args) {
//        System.setProperty("javax.net.debug", "all");
//        System.setProperty("debugMode", "true");
        TcpConfiguration config = new TcpConfiguration();
        config.setSecureConnectionEnabled(true);
        config.setTimeout(2 * 60 * 1000);
        SimpleTcpClient client = new SimpleTcpClient(config);

        for (int i = 0; i < 1; i++) {
            final int j = i;

            client.connect("localhost", 1212)
                  .thenApply(connection -> {
                      StringParser parser = new StringParser();
                      parser.complete(message -> System.out.println(message.trim()));
                      connection.receive(parser::receive)
                                .write("hello world" + j + "!\r\n")
                                .write("test" + j + "\r\n")
                                .write("quit\r\n");
                      return connection;
                  });
        }
    }

}

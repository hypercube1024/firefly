package com.firefly.example.tcp;

import com.firefly.$;
import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.TcpConfiguration;
import com.firefly.net.tcp.TcpServerConfiguration;
import com.firefly.net.tcp.codec.ffsocks.decode.StringParser;
import com.firefly.net.tcp.secure.openssl.DefaultOpenSSLSecureSessionFactory;
import com.firefly.utils.RandomUtils;
import com.firefly.utils.io.IO;

import java.util.concurrent.Phaser;

/**
 * @author Pengtao Qiu
 */
public class HelloTcpServerAndClientWithOpenssl {
    public static void main(String[] args) {
        String host = "localhost";
        int port = (int) RandomUtils.random(1000, 65534);
        Phaser phaser = new Phaser(2);

        SimpleTcpServer server = createTcpServer(host, port);
        SimpleTcpClient client = createTcpClient();

        server.accept(connection -> {
            StringParser parser = new StringParser();
            parser.complete(msg -> {
                String str = msg.trim();
                switch (str) {
                    case "quit": {
                        connection.write("bye!\r\n");
                        IO.close(connection);
                    }
                    break;
                    default: {
                        connection.write("The server received " + str + "\r\n");
                    }
                }
            });
            connection.receive(parser::receive);
        }).start();

        client.connect(host, port).thenAccept(connection -> {
            StringParser parser = new StringParser();
            parser.complete(msg -> {
                String str = msg.trim();
                System.out.println(str);
                if (str.equals("bye!")) {
                    phaser.arrive();
                }
            });
            connection.receive(parser::receive);
            connection.write("hello world\r\n").write("quit\r\n");
        });

        phaser.arriveAndAwaitAdvance();
        client.stop();
        server.stop();
    }

    private static SimpleTcpServer createTcpServer(String host, int port) {
        TcpServerConfiguration serverConfig = new TcpServerConfiguration();
        serverConfig.setSecureConnectionEnabled(true);
        serverConfig.setSecureSessionFactory(new DefaultOpenSSLSecureSessionFactory());
        serverConfig.setHost(host);
        serverConfig.setPort(port);
        return $.createTCPServer(serverConfig);
    }

    private static SimpleTcpClient createTcpClient() {
        TcpConfiguration clientConfig = new TcpConfiguration();
        clientConfig.setSecureConnectionEnabled(true);
        clientConfig.setSecureSessionFactory(new DefaultOpenSSLSecureSessionFactory());
        return $.createTCPClient(clientConfig);
    }
}

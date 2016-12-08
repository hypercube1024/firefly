package test.net.tcp;

import com.firefly.net.tcp.SimpleTcpServer;
import com.firefly.net.tcp.TcpServerConfiguration;
import com.firefly.net.tcp.codec.StringParser;

public class ServerDemo1 {

    public static void main(String[] args) {
//        System.setProperty("javax.net.debug", "all");
        TcpServerConfiguration config = new TcpServerConfiguration();
        config.setSecureConnectionEnabled(true);
        config.setTimeout(2 * 60 * 1000);
        SimpleTcpServer server = new SimpleTcpServer(config);

        server.accept(connection -> {
            StringParser parser = new StringParser();
            parser.complete(message -> {
                String s = message.trim();
                System.out.println("message -> " + s);
                switch (s) {
                    case "quit":
                        connection.write("bye!\r\n").close();
                        break;
                    default:
                        connection.write("received message [" + s + "]\r\n");
                        break;
                }
            });
            connection.receive(parser::receive);
        }).listen("localhost", 1212);
    }

}

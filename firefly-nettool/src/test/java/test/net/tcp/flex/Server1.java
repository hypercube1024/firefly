package test.net.tcp.flex;

import com.firefly.net.tcp.flex.server.MultiplexingServer;

import static test.net.tcp.flex.ConnectionManagerDemo.createServer;

/**
 * @author Pengtao Qiu
 */
public class Server1 {
    public static void main(String[] args) {
        MultiplexingServer server = createServer("localhost", 1133);
    }
}

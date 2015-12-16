package test.net.tcp;

import com.firefly.net.tcp.aio.AsynchronousTcpServer;

import test.net.tcp.example.StringLineDecoder;
import test.net.tcp.example.StringLineEncoder;

public class StringLineTcpServer {

    public static void main(String[] args) {
        new AsynchronousTcpServer(new StringLineDecoder(),
                new StringLineEncoder(), new StringLineHandler()).start("localhost", 9900);
    }
}

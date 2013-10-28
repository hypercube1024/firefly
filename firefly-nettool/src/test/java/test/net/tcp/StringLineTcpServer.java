package test.net.tcp;

import test.net.tcp.example.StringLineDecoder;
import test.net.tcp.example.StringLineEncoder;

import com.firefly.net.tcp.TcpServer;

public class StringLineTcpServer {

    public static void main(String[] args) {
        new TcpServer(new StringLineDecoder(),
                new StringLineEncoder(), new StringLineHandler()).start("localhost", 9900);
    }
}

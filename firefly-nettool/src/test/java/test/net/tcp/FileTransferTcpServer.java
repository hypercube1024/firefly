package test.net.tcp;

import java.net.URISyntaxException;

import test.net.tcp.example.StringLineDecoder;
import test.net.tcp.example.StringLineEncoder;

import com.firefly.net.Server;
import com.firefly.net.tcp.nio.TcpServer;

public class FileTransferTcpServer {

    public static void main(String[] args) throws URISyntaxException {
//		System.out.println(SendFileHandler.class.getResource("/testFile.txt").toURI());
        Server server = new TcpServer(new StringLineDecoder(), new StringLineEncoder(), new SendFileHandler(), 6*1000);
        server.start("localhost", 9900);
    }

}

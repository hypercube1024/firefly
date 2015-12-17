package test.net.tcp;

import java.net.URISyntaxException;

import com.firefly.net.Server;
import com.firefly.net.tcp.aio.AsynchronousTcpServer;

import test.net.tcp.example.StringLineDecoder;
import test.net.tcp.example.StringLineEncoder;

public class FileTransferTcpServer {

    public static void main(String[] args) throws URISyntaxException {
//		System.out.println(SendFileHandler.class.getResource("/testFile.txt").toURI());
        Server server = new AsynchronousTcpServer(new StringLineDecoder(), new StringLineEncoder(), new SendFileHandler(), 6*1000);
        server.start("10.62.71.221", 9900);
        
//        AtomicLong test = new AtomicLong(0);
//        test.addAndGet(30);
//        System.out.println(test.get());
//        test.addAndGet(23);
//        System.out.println(test.get());
    }

}

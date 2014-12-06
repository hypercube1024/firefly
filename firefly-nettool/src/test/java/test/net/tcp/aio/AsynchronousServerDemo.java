package test.net.tcp.aio;

import test.net.tcp.SendFileHandler;
import test.net.tcp.example.StringLineDecoder;
import test.net.tcp.example.StringLineEncoder;

import com.firefly.net.tcp.aio.AsynchronousTcpServer;

public class AsynchronousServerDemo {
	
//	private static Log log = LogFactory.getInstance().getLog("firefly-system");

	public static void main(String[] args) {
		AsynchronousTcpServer server = new AsynchronousTcpServer(
		new StringLineDecoder(), 
		new StringLineEncoder(), 
		new SendFileHandler(), 5000);
		server.start("localhost", 8080);
	}

}

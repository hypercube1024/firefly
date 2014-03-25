package test.net.ssl;

import com.firefly.net.Server;
import com.firefly.net.tcp.TcpServer;

public class Test {

	public static void main(String[] args) throws Throwable {
		Server server = new TcpServer(new SSLDecoder(), new SSLEncoder(), new DumpHandler(), 1000 * 60);	
		server.start("localhost", 7676);
	}
	
	@org.junit.Test
	public void test() {

	}

}

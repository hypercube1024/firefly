package test.net.tcp;

import com.firefly.net.tcp.SimpleTcpClient;
import com.firefly.net.tcp.TcpConnection;
import com.firefly.net.tcp.codec.CharParser;
import com.firefly.net.tcp.codec.DelimiterParser;
import com.firefly.utils.function.Action1;

public class ClientDemo1 {

	public static void main(String[] args) {
		SimpleTcpClient client = new SimpleTcpClient();

		client.connect("localhost", 1212, connection -> {
			getParser(message -> {
				String s = message.trim();
				System.out.println(s);
			}).call(connection);
			connection.write("hello world!\r\n").write("test\r\n").write("quit\r\n");
		});

		client.connect("localhost", 1212, connection -> {
			getParser(message -> {
				String s = message.trim();
				System.out.println(s);
			}).call(connection);
			connection.write("hello world2!\r\n").write("test2\r\n").write("quit\r\n");
		});

	}

	public static Action1<TcpConnection> getParser(Action1<String> complete) {
		Action1<TcpConnection> parser = connection -> {
			CharParser charParser = new CharParser();
			DelimiterParser delimiterParser = new DelimiterParser("\n");

			connection.receive(charParser::receive);
			charParser.complete(delimiterParser::receive);
			delimiterParser.complete(complete);
			connection.closeCallback(() -> {
				System.out.println(connection.getSessionId() + " is closed");
			});
		};
		return parser;
	}

}

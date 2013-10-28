package test.net.tcp.example;

public interface MessageReceivedCallback {
	void messageRecieved(TcpConnection session, Object obj);
}

package test.http;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class ServerDebug {

	public static void main(String[] args) throws Throwable{
		String msg =
				"GET /test/index.html HTTP/1.1\r\n" +
				"Host: 127.0.0.1\r\n" +
				"Range: bytes=3-10,-7\r\n\r\n";
//		String msg = 
//				"GET /app/index HTTP/1.1\r\n" +
//				"Host: 127.0.0.1\r\n\r\n";
//		String msg = 
//				"POST /index.html HTTP/1.1\r\n" +
//				"Host: 127.0.0.1\r\n" +
//				"Expect: 100-continue\r\n\r\n";
		test(msg);
	}
	
	public static void test(String msg) throws Throwable{
		Socket socket = new Socket("localhost", 6655);
		OutputStream out = socket.getOutputStream();
		out.write(msg.getBytes("UTF-8"));
		out.flush();
		InputStream in = socket.getInputStream();
		byte[] ret = new byte[32 * 1024];
		in.read(ret);
		System.out.print(new String(ret, "UTF-8"));
		
		ret = new byte[32 * 1024];
		in.read(ret);
		System.out.print(new String(ret, "UTF-8"));
		
		ret = new byte[32 * 1024];
		in.read(ret);
		System.out.print(new String(ret, "UTF-8"));
		
		ret = new byte[32 * 1024];
		in.read(ret);
		System.out.print(new String(ret, "UTF-8"));
		out.close();
		in.close();
		socket.close();
	}

}

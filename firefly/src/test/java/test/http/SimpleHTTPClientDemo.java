package test.http;

import com.firefly.client.http2.SimpleHTTPClient;

public class SimpleHTTPClientDemo {

	public static void main(String[] args) {
		SimpleHTTPClient client = new SimpleHTTPClient();
		
		client.connect("127.0.0.1", 6655);

	}

}

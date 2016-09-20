package test.http;

import com.firefly.client.http2.SimpleHTTPClient;

public class SimpleHTTPClientDemo3 {

	public static void main(String[] args) {
		SimpleHTTPClient client = new SimpleHTTPClient();
		client.post("http://localhost:3322/testPost").body("test post data, hello").submit(r -> {
			System.out.println(r.getResponse().toString());
			System.out.println(r.getResponse().getFields());
			System.out.println(r.getStringBody());
		});
		
		client.get("http://localhost:3322/index").submit(r -> {
			System.out.println(r.getResponse().toString());
			System.out.println(r.getResponse().getFields());
			System.out.println(r.getStringBody());
		});
	}

}

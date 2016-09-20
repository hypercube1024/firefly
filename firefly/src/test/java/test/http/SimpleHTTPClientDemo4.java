package test.http;

import com.firefly.client.http2.SimpleHTTPClient;

public class SimpleHTTPClientDemo4 {

	public static void main(String[] args) {
		SimpleHTTPClient client = new SimpleHTTPClient();

		for (int i = 0; i < 5; i++) {
			client.post("http://localhost:3333/postData").body("test post data, hello foo " + i).submit(r -> {
				System.out.println(r.getResponse().toString());
				System.out.println(r.getResponse().getFields());
				System.out.println(r.getStringBody());
			});
		}

	}

}

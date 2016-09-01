package test.http;

import java.nio.charset.StandardCharsets;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.utils.io.BufferUtils;

public class SimpleHTTPClientDemo {

	public static void main(String[] args) throws Throwable {
		SimpleHTTPClient client = new SimpleHTTPClient();
		client.get("http://localhost:6656/index").content((buf) -> {
			System.out.print(BufferUtils.toString(buf, StandardCharsets.UTF_8));
		}).messageComplete((response) -> {
			System.out.println(response.toString());
			System.out.println(response.getFields());
		}).end();
		
		client.get("http://localhost:6656/index_1").content((buf) -> {
			System.out.print(BufferUtils.toString(buf, StandardCharsets.UTF_8));
		}).messageComplete((response) -> {
			System.out.println(response.toString());
			System.out.println(response.getFields());
		}).end();
		
		Thread.sleep(5000);
		client.stop();
	}

}

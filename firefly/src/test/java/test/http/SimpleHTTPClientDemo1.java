package test.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Future;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClient.SimpleResponse;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.io.BufferUtils;

public class SimpleHTTPClientDemo1 {

	public static void main(String[] args) throws Throwable {
		SimpleHTTPClient client = new SimpleHTTPClient();
		final long start = System.currentTimeMillis();
		client.get("http://localhost:6656/index").content((buf) -> {
			System.out.print(BufferUtils.toString(buf, StandardCharsets.UTF_8));
		}).messageComplete((response) -> {
			long end = System.currentTimeMillis();
			System.out.println();
			System.out.println(response.toString());
			System.out.println(response.getFields());
			System.out.println("------------------------------------ " + (end - start));
		}).end();

		long s2 = System.currentTimeMillis();
		client.get("http://localhost:6656/index_1").content((buf) -> {
			System.out.print(BufferUtils.toString(buf, StandardCharsets.UTF_8));
		}).messageComplete((response) -> {
			long end = System.currentTimeMillis();
			System.out.println();
			System.out.println(response.toString());
			System.out.println(response.getFields());
			System.out.println("------------------------------------ " + (end - s2));
		}).end();

		long s3 = System.currentTimeMillis();
		Future<SimpleResponse> future = client.get("http://localhost:6656/login").submit();
		SimpleResponse simpleResponse = future.get();
		long end = System.currentTimeMillis();
		System.out.println();
		System.out.println(simpleResponse.getStringBody());
		System.out.println(simpleResponse.getResponse().toString());
		System.out.println(simpleResponse.getResponse().getFields());
		System.out.println("------------------------------------ " + (end - s3));

		long s4 = System.currentTimeMillis();
		byte[] test = "content=hello_hello".getBytes(StandardCharsets.UTF_8);
		future = client.post("http://localhost:6656/add").output((o) -> {
			try (HTTPOutputStream out = o) {
				out.write(test);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).put(HttpHeader.CONTENT_LENGTH, String.valueOf(test.length))
		.cookies(simpleResponse.getCookies())
		.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.FORM_ENCODED.asString())
		.submit();
		simpleResponse = future.get();
		long end2 = System.currentTimeMillis();
		System.out.println();
		System.out.println(simpleResponse.getStringBody());
		System.out.println(simpleResponse.getResponse().toString());
		System.out.println(simpleResponse.getResponse().getFields());
		System.out.println("------------------------------------ " + (end2 - s4));

		Thread.sleep(5000);
		client.removeConnectionPool("http://localhost:6656");
		
		long s5 = System.currentTimeMillis();
		client.get("http://localhost:6656/index_1").content((buf) -> {
			System.out.print(BufferUtils.toString(buf, StandardCharsets.UTF_8));
		}).messageComplete((response) -> {
			long e5 = System.currentTimeMillis();
			System.out.println();
			System.out.println(response.toString());
			System.out.println(response.getFields());
			System.out.println("------------------------------------ " + (e5 - s5));
		}).end();
		Thread.sleep(5000);
		client.stop();
	}

}

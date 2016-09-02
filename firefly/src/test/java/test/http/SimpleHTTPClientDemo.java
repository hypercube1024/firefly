package test.http;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.codec.http2.model.CookieGenerator;
import com.firefly.codec.http2.model.CookieParser;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.codec.http2.stream.HTTPOutputStream;
import com.firefly.utils.io.BufferUtils;

public class SimpleHTTPClientDemo {

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
		client.get("http://localhost:6656/login").content((buf) -> {
			System.out.print(BufferUtils.toString(buf, StandardCharsets.UTF_8));
		}).messageComplete((response) -> {
			long end = System.currentTimeMillis();
			System.out.println();
			System.out.println(response.toString());
			System.out.println(response.getFields());
			System.out.println("------------------------------------ " + (end - s3));

			long start2 = System.currentTimeMillis();
			byte[] test = "content=hello_hello".getBytes(StandardCharsets.UTF_8);
			client.post("http://localhost:6656/add").output((o) -> {
				try (HTTPOutputStream out = o) {
					out.write(test);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}).put(HttpHeader.CONTENT_LENGTH, String.valueOf(test.length)).put(HttpHeader.COOKIE,
					CookieGenerator.generateCookies(response.getFields().getValuesList(HttpHeader.SET_COOKIE.asString())
							.stream().map(CookieParser::parseSetCookie).collect(Collectors.toList())))
					.put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.FORM_ENCODED.asString()).content((buf) -> {
						System.out.print(BufferUtils.toString(buf, StandardCharsets.UTF_8));
					}).messageComplete((res) -> {
						long end2 = System.currentTimeMillis();
						System.out.println();
						System.out.println(res.toString());
						System.out.println(res.getFields());
						System.out.println("------------------------------------ " + (end2 - start2));

					}).end();
		}).end();

		Thread.sleep(5000);
		client.stop();
	}

}

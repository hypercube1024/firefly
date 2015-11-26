package test.codec.http2.encode;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.model.DateGenerator;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpHeader;
import com.firefly.codec.http2.model.HttpHeaderValue;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.io.BufferUtils;

public class HttpGeneratorServerTest {

	@Test
	public void testSimple() throws Exception {
		ByteBuffer header = BufferUtils.allocate(8096);
		ByteBuffer content = BufferUtils.toBuffer("0123456789");

		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateResponse(null, null, null, content, true);
		assertEquals(HttpGenerator.Result.NEED_INFO, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), 10);
		info.getFields().add("Content-Type", "test/data");
		info.getFields().add("Last-Modified", DateGenerator.__01Jan1970);

		result = gen.generateResponse(info, null, null, content, true);
		assertEquals(HttpGenerator.Result.NEED_HEADER, result);

		result = gen.generateResponse(info, header, null, content, true);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		String response = BufferUtils.toString(header);
		BufferUtils.clear(header);
		response += BufferUtils.toString(content);
		BufferUtils.clear(content);

		result = gen.generateResponse(null, null, null, content, false);
		assertEquals(HttpGenerator.Result.DONE, result);
		assertEquals(HttpGenerator.State.END, gen.getState());

		assertEquals(10, gen.getContentPrepared());

		assertThat(response, containsString("HTTP/1.1 200 OK"));
		assertThat(response, containsString("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
		assertThat(response, containsString("Content-Length: 10"));
		assertThat(response, containsString("\r\n0123456789"));
	}

	@Test
	public void test204() throws Exception {
		ByteBuffer header = BufferUtils.allocate(8096);
		ByteBuffer content = BufferUtils.toBuffer("0123456789");

		HttpGenerator gen = new HttpGenerator();

		MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 204, "Foo", new HttpFields(), 10);
		info.getFields().add("Content-Type", "test/data");
		info.getFields().add("Last-Modified", DateGenerator.__01Jan1970);

		HttpGenerator.Result result = gen.generateResponse(info, header, null, content, true);

		assertEquals(gen.isNoContent(), true);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		String responseheaders = BufferUtils.toString(header);
		BufferUtils.clear(header);

		result = gen.generateResponse(null, null, null, content, false);
		assertEquals(HttpGenerator.Result.DONE, result);
		assertEquals(HttpGenerator.State.END, gen.getState());

		assertThat(responseheaders, containsString("HTTP/1.1 204 Foo"));
		assertThat(responseheaders, containsString("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
		assertThat(responseheaders, not(containsString("Content-Length: 10")));

		// Note: the HttpConnection.process() method is responsible for actually
		// excluding the content from the response based on
		// generator.isNoContent()==true
	}

	@Test
	public void testComplexChars() throws Exception {
		ByteBuffer header = BufferUtils.allocate(8096);
		ByteBuffer content = BufferUtils.toBuffer("0123456789");

		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateResponse(null, null, null, content, true);
		assertEquals(HttpGenerator.Result.NEED_INFO, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), 10);
		info.getFields().add("Content-Type", "test/data;\r\nextra=value");
		info.getFields().add("Last-Modified", DateGenerator.__01Jan1970);

		result = gen.generateResponse(info, null, null, content, true);
		assertEquals(HttpGenerator.Result.NEED_HEADER, result);

		result = gen.generateResponse(info, header, null, content, true);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		String response = BufferUtils.toString(header);
		BufferUtils.clear(header);
		response += BufferUtils.toString(content);
		BufferUtils.clear(content);

		result = gen.generateResponse(null, null, null, content, false);
		assertEquals(HttpGenerator.Result.DONE, result);
		assertEquals(HttpGenerator.State.END, gen.getState());

		assertEquals(10, gen.getContentPrepared());

		assertThat(response, containsString("HTTP/1.1 200 OK"));
		assertThat(response, containsString("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
		assertThat(response, containsString("Content-Type: test/data;  extra=value"));
		assertThat(response, containsString("Content-Length: 10"));
		assertThat(response, containsString("\r\n0123456789"));
	}

	@Test
	public void testSendServerXPoweredBy() throws Exception {
		ByteBuffer header = BufferUtils.allocate(8096);
		MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), -1);
		HttpFields fields = new HttpFields();
		fields.add(HttpHeader.SERVER, "SomeServer");
		fields.add(HttpHeader.X_POWERED_BY, "SomePower");
		MetaData.Response infoF = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, fields, -1);
		String head;

		HttpGenerator gen = new HttpGenerator(true, true);
		gen.generateResponse(info, header, null, null, true);
		head = BufferUtils.toString(header);
		BufferUtils.clear(header);
		assertThat(head, containsString("HTTP/1.1 200 OK"));
		assertThat(head, containsString("Server: Firefly 3.0"));
		assertThat(head, containsString("X-Powered-By: Firefly 3.0"));
		gen.reset();
		gen.generateResponse(infoF, header, null, null, true);
		head = BufferUtils.toString(header);
		BufferUtils.clear(header);
		assertThat(head, containsString("HTTP/1.1 200 OK"));
		assertThat(head, not(containsString("Server: Firefly 3.0")));
		assertThat(head, containsString("Server: SomeServer"));
		assertThat(head, containsString("X-Powered-By: Firefly 3.0"));
		assertThat(head, containsString("X-Powered-By: SomePower"));
		gen.reset();

		gen = new HttpGenerator(false, false);
		gen.generateResponse(info, header, null, null, true);
		head = BufferUtils.toString(header);
		BufferUtils.clear(header);
		assertThat(head, containsString("HTTP/1.1 200 OK"));
		assertThat(head, not(containsString("Server: Firefly 3.0")));
		assertThat(head, not(containsString("X-Powered-By: Firefly 3.0")));
		gen.reset();
		gen.generateResponse(infoF, header, null, null, true);
		head = BufferUtils.toString(header);
		BufferUtils.clear(header);
		assertThat(head, containsString("HTTP/1.1 200 OK"));
		assertThat(head, not(containsString("Server: Firefly 3.0")));
		assertThat(head, containsString("Server: SomeServer"));
		assertThat(head, not(containsString("X-Powered-By: Firefly 3.0")));
		assertThat(head, containsString("X-Powered-By: SomePower"));
		gen.reset();
	}

	@Test
	public void testResponseNoContent() throws Exception {
		ByteBuffer header = BufferUtils.allocate(8096);

		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateResponse(null, null, null, null, true);
		assertEquals(HttpGenerator.Result.NEED_INFO, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), -1);
		info.getFields().add("Last-Modified", DateGenerator.__01Jan1970);

		result = gen.generateResponse(info, null, null, null, true);
		assertEquals(HttpGenerator.Result.NEED_HEADER, result);

		result = gen.generateResponse(info, header, null, null, true);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		String head = BufferUtils.toString(header);
		BufferUtils.clear(header);

		result = gen.generateResponse(null, null, null, null, false);
		assertEquals(HttpGenerator.Result.DONE, result);
		assertEquals(HttpGenerator.State.END, gen.getState());

		assertEquals(0, gen.getContentPrepared());
		assertThat(head, containsString("HTTP/1.1 200 OK"));
		assertThat(head, containsString("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
		assertThat(head, containsString("Content-Length: 0"));
	}

	@Test
	public void testResponseUpgrade() throws Exception {
		ByteBuffer header = BufferUtils.allocate(8096);

		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateResponse(null, null, null, null, true);
		assertEquals(HttpGenerator.Result.NEED_INFO, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 101, null, new HttpFields(), -1);
		info.getFields().add("Upgrade", "WebSocket");
		info.getFields().add("Connection", "Upgrade");
		info.getFields().add("Sec-WebSocket-Accept", "123456789==");

		result = gen.generateResponse(info, header, null, null, true);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		String head = BufferUtils.toString(header);
		BufferUtils.clear(header);

		result = gen.generateResponse(info, null, null, null, false);
		assertEquals(HttpGenerator.Result.DONE, result);
		assertEquals(HttpGenerator.State.END, gen.getState());

		assertEquals(0, gen.getContentPrepared());

		assertThat(head, startsWith("HTTP/1.1 101 Switching Protocols"));
		assertThat(head, containsString("Upgrade: WebSocket\r\n"));
		assertThat(head, containsString("Connection: Upgrade\r\n"));
	}

	@Test
	public void testResponseWithChunkedContent() throws Exception {
		ByteBuffer header = BufferUtils.allocate(4096);
		ByteBuffer chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);
		ByteBuffer content0 = BufferUtils.toBuffer("Hello World! ");
		ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog. ");
		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateResponse(null, null, null, content0, false);
		assertEquals(HttpGenerator.Result.NEED_INFO, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), -1);
		info.getFields().add("Last-Modified", DateGenerator.__01Jan1970);
		result = gen.generateResponse(info, null, null, content0, false);
		assertEquals(HttpGenerator.Result.NEED_HEADER, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		result = gen.generateResponse(info, header, null, content0, false);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

		String out = BufferUtils.toString(header);
		BufferUtils.clear(header);
		out += BufferUtils.toString(content0);
		BufferUtils.clear(content0);

		result = gen.generateResponse(null, null, chunk, content1, false);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
		out += BufferUtils.toString(chunk);
		BufferUtils.clear(chunk);
		out += BufferUtils.toString(content1);
		BufferUtils.clear(content1);

		result = gen.generateResponse(null, null, chunk, null, true);
		assertEquals(HttpGenerator.Result.CONTINUE, result);
		assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

		result = gen.generateResponse(null, null, chunk, null, true);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		out += BufferUtils.toString(chunk);
		BufferUtils.clear(chunk);

		result = gen.generateResponse(null, null, chunk, null, true);
		assertEquals(HttpGenerator.Result.DONE, result);
		assertEquals(HttpGenerator.State.END, gen.getState());

		assertThat(out, containsString("HTTP/1.1 200 OK"));
		assertThat(out, containsString("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
		assertThat(out, not(containsString("Content-Length")));
		assertThat(out, containsString("Transfer-Encoding: chunked"));
		assertThat(out, containsString("\r\n\r\nD\r\n"));
		assertThat(out, containsString("\r\nHello World! \r\n"));
		assertThat(out, containsString("\r\n2E\r\n"));
		assertThat(out, containsString("\r\nThe quick brown fox jumped over the lazy dog. \r\n"));
		assertThat(out, containsString("\r\n0\r\n"));
	}

	@Test
	public void testResponseWithKnownContent() throws Exception {
		ByteBuffer header = BufferUtils.allocate(4096);
		ByteBuffer content0 = BufferUtils.toBuffer("Hello World! ");
		ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog. ");
		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateResponse(null, null, null, content0, false);
		assertEquals(HttpGenerator.Result.NEED_INFO, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(), 59);
		info.getFields().add("Last-Modified", DateGenerator.__01Jan1970);
		result = gen.generateResponse(info, null, null, content0, false);
		assertEquals(HttpGenerator.Result.NEED_HEADER, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		result = gen.generateResponse(info, header, null, content0, false);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

		String out = BufferUtils.toString(header);
		BufferUtils.clear(header);
		out += BufferUtils.toString(content0);
		BufferUtils.clear(content0);

		result = gen.generateResponse(null, null, null, content1, false);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
		out += BufferUtils.toString(content1);
		BufferUtils.clear(content1);

		result = gen.generateResponse(null, null, null, null, true);
		assertEquals(HttpGenerator.Result.CONTINUE, result);
		assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

		result = gen.generateResponse(null, null, null, null, true);
		assertEquals(HttpGenerator.Result.DONE, result);
		assertEquals(HttpGenerator.State.END, gen.getState());

		assertThat(out, containsString("HTTP/1.1 200 OK"));
		assertThat(out, containsString("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
		assertThat(out, not(containsString("chunked")));
		assertThat(out, containsString("Content-Length: 59"));
		assertThat(out, containsString("\r\n\r\nHello World! The quick brown fox jumped over the lazy dog. "));
	}

	@Test
	public void test100ThenResponseWithContent() throws Exception {
		ByteBuffer header = BufferUtils.allocate(4096);
		ByteBuffer content0 = BufferUtils.toBuffer("Hello World! ");
		ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog. ");
		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateResponse(HttpGenerator.CONTINUE_100_INFO, null, null, null, false);
		assertEquals(HttpGenerator.Result.NEED_HEADER, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		result = gen.generateResponse(HttpGenerator.CONTINUE_100_INFO, header, null, null, false);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMPLETING_1XX, gen.getState());
		String out = BufferUtils.toString(header);

		result = gen.generateResponse(null, null, null, null, false);
		assertEquals(HttpGenerator.Result.DONE, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		assertThat(out, containsString("HTTP/1.1 100 Continue"));

		result = gen.generateResponse(null, null, null, content0, false);
		assertEquals(HttpGenerator.Result.NEED_INFO, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_1, 200, null, new HttpFields(),
				BufferUtils.length(content0) + BufferUtils.length(content1));
		info.getFields().add("Last-Modified", DateGenerator.__01Jan1970);
		result = gen.generateResponse(info, null, null, content0, false);
		assertEquals(HttpGenerator.Result.NEED_HEADER, result);
		assertEquals(HttpGenerator.State.START, gen.getState());

		result = gen.generateResponse(info, header, null, content0, false);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

		out = BufferUtils.toString(header);
		BufferUtils.clear(header);
		out += BufferUtils.toString(content0);
		BufferUtils.clear(content0);

		result = gen.generateResponse(null, null, null, content1, false);
		assertEquals(HttpGenerator.Result.FLUSH, result);
		assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
		out += BufferUtils.toString(content1);
		BufferUtils.clear(content1);

		result = gen.generateResponse(null, null, null, null, true);
		assertEquals(HttpGenerator.Result.CONTINUE, result);
		assertEquals(HttpGenerator.State.COMPLETING, gen.getState());

		result = gen.generateResponse(null, null, null, null, true);
		assertEquals(HttpGenerator.Result.DONE, result);
		assertEquals(HttpGenerator.State.END, gen.getState());

		assertThat(out, containsString("HTTP/1.1 200 OK"));
		assertThat(out, containsString("Last-Modified: Thu, 01 Jan 1970 00:00:00 GMT"));
		assertThat(out, not(containsString("chunked")));
		assertThat(out, containsString("Content-Length: 59"));
		assertThat(out, containsString("\r\n\r\nHello World! The quick brown fox jumped over the lazy dog. "));
	}

	@Test
	public void testConnectionKeepAliveWithAdditionalCustomValue() throws Exception {
		HttpGenerator generator = new HttpGenerator();

		HttpFields fields = new HttpFields();
		fields.put(HttpHeader.CONNECTION, HttpHeaderValue.KEEP_ALIVE);
		String customValue = "test";
		fields.add(HttpHeader.CONNECTION, customValue);
		MetaData.Response info = new MetaData.Response(HttpVersion.HTTP_1_0, 200, "OK", fields, -1);
		ByteBuffer header = BufferUtils.allocate(4096);
		HttpGenerator.Result result = generator.generateResponse(info, header, null, null, true);
		Assert.assertSame(HttpGenerator.Result.FLUSH, result);
		String headers = BufferUtils.toString(header);
		Assert.assertTrue(headers.contains(HttpHeaderValue.KEEP_ALIVE.asString()));
		Assert.assertTrue(headers.contains(customValue));
	}
}

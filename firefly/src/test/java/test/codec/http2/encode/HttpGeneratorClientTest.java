package test.codec.http2.encode;

import java.nio.ByteBuffer;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.encode.HttpGenerator;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpURI;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;
import com.firefly.utils.io.BufferUtils;

public class HttpGeneratorClientTest {
	public final static String[] connect = { null, "keep-alive", "close" };

	class Info extends MetaData.Request {
		Info(String method, String uri) {
			super(method, new HttpURI(uri), HttpVersion.HTTP_1_1, new HttpFields(), -1);
		}

		public Info(String method, String uri, int contentLength) {
			super(method, new HttpURI(uri), HttpVersion.HTTP_1_1, new HttpFields(), contentLength);
		}
	}

	@Test
	public void testGETRequestNoContent() throws Exception {
		ByteBuffer header = BufferUtils.allocate(2048);
		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateRequest(null, null, null, null, true);
		Assert.assertEquals(HttpGenerator.Result.NEED_INFO, result);
		Assert.assertEquals(HttpGenerator.State.START, gen.getState());

		Info info = new Info("GET", "/index.html");
		info.getFields().add("Host", "something");
		info.getFields().add("User-Agent", "test");
		Assert.assertTrue(!gen.isChunking());

		result = gen.generateRequest(info, null, null, null, true);
		Assert.assertEquals(HttpGenerator.Result.NEED_HEADER, result);
		Assert.assertEquals(HttpGenerator.State.START, gen.getState());

		result = gen.generateRequest(info, header, null, null, true);
		Assert.assertEquals(HttpGenerator.Result.FLUSH, result);
		Assert.assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		Assert.assertTrue(!gen.isChunking());
		String out = BufferUtils.toString(header);
		BufferUtils.clear(header);

		result = gen.generateResponse(null, null, null, null, false);
		Assert.assertEquals(HttpGenerator.Result.DONE, result);
		Assert.assertEquals(HttpGenerator.State.END, gen.getState());
		Assert.assertTrue(!gen.isChunking());

		Assert.assertEquals(0, gen.getContentPrepared());
		Assert.assertThat(out, Matchers.containsString("GET /index.html HTTP/1.1"));
		Assert.assertThat(out, Matchers.not(Matchers.containsString("Content-Length")));
	}

	@Test
	public void testPOSTRequestNoContent() throws Exception {
		ByteBuffer header = BufferUtils.allocate(2048);
		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateRequest(null, null, null, null, true);
		Assert.assertEquals(HttpGenerator.Result.NEED_INFO, result);
		Assert.assertEquals(HttpGenerator.State.START, gen.getState());

		Info info = new Info("POST", "/index.html");
		info.getFields().add("Host", "something");
		info.getFields().add("User-Agent", "test");
		Assert.assertTrue(!gen.isChunking());

		result = gen.generateRequest(info, null, null, null, true);
		Assert.assertEquals(HttpGenerator.Result.NEED_HEADER, result);
		Assert.assertEquals(HttpGenerator.State.START, gen.getState());

		result = gen.generateRequest(info, header, null, null, true);
		Assert.assertEquals(HttpGenerator.Result.FLUSH, result);
		Assert.assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		Assert.assertTrue(!gen.isChunking());
		String out = BufferUtils.toString(header);
		BufferUtils.clear(header);

		result = gen.generateResponse(null, null, null, null, false);
		Assert.assertEquals(HttpGenerator.Result.DONE, result);
		Assert.assertEquals(HttpGenerator.State.END, gen.getState());
		Assert.assertTrue(!gen.isChunking());

		Assert.assertEquals(0, gen.getContentPrepared());
		Assert.assertThat(out, Matchers.containsString("POST /index.html HTTP/1.1"));
		Assert.assertThat(out, Matchers.containsString("Content-Length: 0"));
	}

	@Test
	public void testRequestWithContent() throws Exception {
		String out;
		ByteBuffer header = BufferUtils.allocate(4096);
		ByteBuffer content0 = BufferUtils.toBuffer("Hello World. The quick brown fox jumped over the lazy dog.");
		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateRequest(null, null, null, content0, true);
		Assert.assertEquals(HttpGenerator.Result.NEED_INFO, result);
		Assert.assertEquals(HttpGenerator.State.START, gen.getState());

		Info info = new Info("POST", "/index.html");
		info.getFields().add("Host", "something");
		info.getFields().add("User-Agent", "test");

		result = gen.generateRequest(info, null, null, content0, true);
		Assert.assertEquals(HttpGenerator.Result.NEED_HEADER, result);
		Assert.assertEquals(HttpGenerator.State.START, gen.getState());

		result = gen.generateRequest(info, header, null, content0, true);
		Assert.assertEquals(HttpGenerator.Result.FLUSH, result);
		Assert.assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		Assert.assertTrue(!gen.isChunking());
		out = BufferUtils.toString(header);
		BufferUtils.clear(header);
		out += BufferUtils.toString(content0);
		BufferUtils.clear(content0);

		result = gen.generateResponse(null, null, null, null, false);
		Assert.assertEquals(HttpGenerator.Result.DONE, result);
		Assert.assertEquals(HttpGenerator.State.END, gen.getState());
		Assert.assertTrue(!gen.isChunking());

		Assert.assertThat(out, Matchers.containsString("POST /index.html HTTP/1.1"));
		Assert.assertThat(out, Matchers.containsString("Host: something"));
		Assert.assertThat(out, Matchers.containsString("Content-Length: 58"));
		Assert.assertThat(out, Matchers.containsString("Hello World. The quick brown fox jumped over the lazy dog."));

		Assert.assertEquals(58, gen.getContentPrepared());
	}

	@Test
	public void testRequestWithChunkedContent() throws Exception {
		String out;
		ByteBuffer header = BufferUtils.allocate(4096);
		ByteBuffer chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);
		ByteBuffer content0 = BufferUtils.toBuffer("Hello World. ");
		ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog.");
		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateRequest(null, null, null, content0, false);
		Assert.assertEquals(HttpGenerator.Result.NEED_INFO, result);
		Assert.assertEquals(HttpGenerator.State.START, gen.getState());

		Info info = new Info("POST", "/index.html");
		info.getFields().add("Host", "something");
		info.getFields().add("User-Agent", "test");

		result = gen.generateRequest(info, null, null, content0, false);
		Assert.assertEquals(HttpGenerator.Result.NEED_HEADER, result);
		Assert.assertEquals(HttpGenerator.State.START, gen.getState());

		result = gen.generateRequest(info, header, null, content0, false);
		Assert.assertEquals(HttpGenerator.Result.FLUSH, result);
		Assert.assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
		Assert.assertTrue(gen.isChunking());
		out = BufferUtils.toString(header);
		BufferUtils.clear(header);
		out += BufferUtils.toString(content0);
		BufferUtils.clear(content0);

		result = gen.generateRequest(null, header, null, content1, false);
		Assert.assertEquals(HttpGenerator.Result.NEED_CHUNK, result);
		Assert.assertEquals(HttpGenerator.State.COMMITTED, gen.getState());

		result = gen.generateRequest(null, null, chunk, content1, false);
		Assert.assertEquals(HttpGenerator.Result.FLUSH, result);
		Assert.assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
		Assert.assertTrue(gen.isChunking());
		out += BufferUtils.toString(chunk);
		BufferUtils.clear(chunk);
		out += BufferUtils.toString(content1);
		BufferUtils.clear(content1);

		result = gen.generateResponse(null, null, chunk, null, true);
		Assert.assertEquals(HttpGenerator.Result.CONTINUE, result);
		Assert.assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		Assert.assertTrue(gen.isChunking());

		result = gen.generateResponse(null, null, chunk, null, true);
		Assert.assertEquals(HttpGenerator.Result.FLUSH, result);
		Assert.assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		out += BufferUtils.toString(chunk);
		BufferUtils.clear(chunk);
		Assert.assertTrue(!gen.isChunking());

		result = gen.generateResponse(null, null, chunk, null, true);
		Assert.assertEquals(HttpGenerator.Result.DONE, result);
		Assert.assertEquals(HttpGenerator.State.END, gen.getState());

		Assert.assertThat(out, Matchers.containsString("POST /index.html HTTP/1.1"));
		Assert.assertThat(out, Matchers.containsString("Host: something"));
		Assert.assertThat(out, Matchers.containsString("Transfer-Encoding: chunked"));
		Assert.assertThat(out, Matchers.containsString("\r\nD\r\nHello World. \r\n"));
		Assert.assertThat(out, Matchers.containsString("\r\n2D\r\nThe quick brown fox jumped over the lazy dog.\r\n"));
		Assert.assertThat(out, Matchers.containsString("\r\n0\r\n\r\n"));

		Assert.assertEquals(58, gen.getContentPrepared());

	}

	@Test
	public void testRequestWithKnownContent() throws Exception {
		String out;
		ByteBuffer header = BufferUtils.allocate(4096);
		ByteBuffer chunk = BufferUtils.allocate(HttpGenerator.CHUNK_SIZE);
		ByteBuffer content0 = BufferUtils.toBuffer("Hello World. ");
		ByteBuffer content1 = BufferUtils.toBuffer("The quick brown fox jumped over the lazy dog.");
		HttpGenerator gen = new HttpGenerator();

		HttpGenerator.Result result = gen.generateRequest(null, null, null, content0, false);
		Assert.assertEquals(HttpGenerator.Result.NEED_INFO, result);
		Assert.assertEquals(HttpGenerator.State.START, gen.getState());

		Info info = new Info("POST", "/index.html", 58);
		info.getFields().add("Host", "something");
		info.getFields().add("User-Agent", "test");

		result = gen.generateRequest(info, null, null, content0, false);
		Assert.assertEquals(HttpGenerator.Result.NEED_HEADER, result);
		Assert.assertEquals(HttpGenerator.State.START, gen.getState());

		result = gen.generateRequest(info, header, null, content0, false);
		Assert.assertEquals(HttpGenerator.Result.FLUSH, result);
		Assert.assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
		Assert.assertTrue(!gen.isChunking());
		out = BufferUtils.toString(header);
		BufferUtils.clear(header);
		out += BufferUtils.toString(content0);
		BufferUtils.clear(content0);

		result = gen.generateRequest(null, null, null, content1, false);
		Assert.assertEquals(HttpGenerator.Result.FLUSH, result);
		Assert.assertEquals(HttpGenerator.State.COMMITTED, gen.getState());
		Assert.assertTrue(!gen.isChunking());
		out += BufferUtils.toString(content1);
		BufferUtils.clear(content1);

		result = gen.generateResponse(null, null, null, null, true);
		Assert.assertEquals(HttpGenerator.Result.CONTINUE, result);
		Assert.assertEquals(HttpGenerator.State.COMPLETING, gen.getState());
		Assert.assertTrue(!gen.isChunking());

		result = gen.generateResponse(null, null, null, null, true);
		Assert.assertEquals(HttpGenerator.Result.DONE, result);
		Assert.assertEquals(HttpGenerator.State.END, gen.getState());
		out += BufferUtils.toString(chunk);
		BufferUtils.clear(chunk);

		Assert.assertThat(out, Matchers.containsString("POST /index.html HTTP/1.1"));
		Assert.assertThat(out, Matchers.containsString("Host: something"));
		Assert.assertThat(out, Matchers.containsString("Content-Length: 58"));
		Assert.assertThat(out,
				Matchers.containsString("\r\n\r\nHello World. The quick brown fox jumped over the lazy dog."));

		Assert.assertEquals(58, gen.getContentPrepared());

	}

}

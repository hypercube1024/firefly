package test.http;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.util.List;

import javax.servlet.ServletInputStream;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.server.http.Config;
import com.firefly.server.http.HttpDecoder;
import com.firefly.server.http.HttpServletRequestImpl;
import com.firefly.server.http.HttpServletResponseImpl;

public class TestHttpDecoder {
	private static final Config config = new Config();
	private static final HttpDecoder httpDecoder = new HttpDecoder(config);

	@Test
	public void testRequestLine() throws Throwable {
		byte[] buf1 = "GET /firefly-demo/app/hel"
				.getBytes(config.getEncoding());
		byte[] buf2 = "lo HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n".getBytes(config
				.getEncoding());
		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1),
				ByteBuffer.wrap(buf2) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}

		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		Assert.assertThat(req.getMethod(), is("GET"));
		Assert.assertThat(req.getRequestURI(), is("/firefly-demo/app/hello"));
		Assert.assertThat(req.getProtocol(), is("HTTP/1.1"));
	}

	@Test
	public void testRequestLine2() throws Throwable {
		byte[] buf1 = "GET /firefly-demo/app/hello HTTP/1.1\r\n\r\n"
				.getBytes(config.getEncoding());
		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}

		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		Assert.assertThat(req.getMethod(), is("GET"));
		Assert.assertThat(req.getRequestURI(), is("/firefly-demo/app/hello"));
		Assert.assertThat(req.getProtocol(), is("HTTP/1.1"));
	}

	@Test
	public void testRequestLine3() throws Throwable {
		byte[] buf1 = "GET /firefly-demo/app/hello?query=3.3&test=4 HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n"
				.getBytes(config.getEncoding());
		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}

		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		Assert.assertThat(req.getMethod(), is("GET"));
		Assert.assertThat(req.getRequestURI(), is("/firefly-demo/app/hello"));
		Assert.assertThat(req.getProtocol(), is("HTTP/1.1"));
		Assert.assertThat(req.getQueryString(), is("query=3.3&test=4"));
	}

	@Test
	public void testHead() throws Throwable {
		byte[] buf1 = "GET /firefly-demo/app/hel"
				.getBytes(config.getEncoding());
		byte[] buf2 = "lo?query=3.3&test=4 HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n"
				.getBytes(config.getEncoding());
		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1),
				ByteBuffer.wrap(buf2) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}

		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		Assert.assertThat(req.getHeader("host"), is("127.0.0.1"));
		Assert.assertThat(req.getHeader("Host"), is("127.0.0.1"));
	}

	@Test
	public void testHead2() throws Throwable {
		byte[] buf1 = "GET /firefly-demo/app/hel"
				.getBytes(config.getEncoding());
		byte[] buf2 = "lo HTTP/1.1\r\nHost:127.0.0.1\r\nAccept-Language:zh-CN,"
				.getBytes(config.getEncoding());
		byte[] buf3 = "zh;q=0.8\r\nConnection:keep-alive\r\n\r\n"
				.getBytes(config.getEncoding());
		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1),
				ByteBuffer.wrap(buf2), ByteBuffer.wrap(buf3) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}

		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		Assert.assertThat(req.getHeader("host"), is("127.0.0.1"));
		Assert.assertThat(req.getHeader("connection"), is("keep-alive"));
		Assert.assertThat(req.getHeader("Accept-Language"), is("zh-CN,zh;q=0.8"));
	}

	@Test
	public void testHead3() throws Throwable {
		byte[] buf1 = "GET /firefly-demo/app/hello HTTP/1.1\r\n"
				.getBytes(config.getEncoding());
		byte[] buf2 = "Host:127.0.0.1\r\n".getBytes(config.getEncoding());
		byte[] buf3 = "Accept-Language:zh-CN,zh;q=0.8\r\nConnection:keep-alive\r\n"
				.getBytes(config.getEncoding());
		byte[] buf4 = "Accept-Encoding: gzip,deflate,sdch\r\n\r\n"
				.getBytes(config.getEncoding());
		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1),
				ByteBuffer.wrap(buf2), ByteBuffer.wrap(buf3),
				ByteBuffer.wrap(buf4) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}

		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		Assert.assertThat(req.getHeader("host"), is("127.0.0.1"));
		Assert.assertThat(req.getHeader("connection"), is("keep-alive"));
		Assert.assertThat(req.getHeader("Accept-Language"),
				is("zh-CN,zh;q=0.8"));
		Assert.assertThat(req.getHeader("Accept-Encoding"),
				is("gzip,deflate,sdch"));
	}

	@Test
	public void testHead4() throws Throwable {
		byte[] buf1 = "GET /firefly-demo/app/hello HTTP/1.1\r\nHost:127.0.0.1\r\nAccept-Language:zh-CN,zh;q=0.8\r\nConnection:keep-alive\r\nAccept-Encoding: gzip,deflate,sdch\r\n\r\n"
				.getBytes(config.getEncoding());
		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}

		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		Assert.assertThat(req.getMethod(), is("GET"));
		Assert.assertThat(req.getRequestURI(), is("/firefly-demo/app/hello"));
		Assert.assertThat(req.getProtocol(), is("HTTP/1.1"));
		Assert.assertThat(req.getHeader("host"), is("127.0.0.1"));
		Assert.assertThat(req.getHeader("connection"), is("keep-alive"));
		Assert.assertThat(req.getHeader("Accept-Language"),
				is("zh-CN,zh;q=0.8"));
		Assert.assertThat(req.getHeader("Accept-Encoding"),
				is("gzip,deflate,sdch"));
	}
	
	@Test
	public void testPipelineDecode() throws Throwable {
		byte[] buf1 = ("GET /firefly-demo/app/hello HTTP/1.1\r\nHost:127.0.0.1\r\nAccept-Language:zh-CN,zh;q=0.8\r\nConnection:keep-alive\r\nAccept-Encoding: gzip,deflate,sdch\r\n\r\n" +
				"GET /firefly-demo/app/hello2 HTTP/1.1\r\nHost:127.0.0.1\r\n\r\n").getBytes(config.getEncoding());
		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}
		
		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(2));
		System.out.println(list.get(0).getRequestURI());
		System.out.println(list.get(1).getRequestURI());
		Assert.assertThat(list.get(0).getRequestURI(), is("/firefly-demo/app/hello"));
		Assert.assertThat(list.get(1).getRequestURI(), is("/firefly-demo/app/hello2"));
	}

	@Test
	public void testBody() throws Throwable {
		byte[] buf1 = "POST /firefly-demo/app/hel".getBytes(config
				.getEncoding());
		byte[] buf2 = "lo HTTP/1.1\r\nHost:127.0.0.1\r\nAccept-Language:zh-CN,"
				.getBytes(config.getEncoding());
		byte[] buf3 = "zh;q=0.8\r\nConnection:keep-alive\r\n".getBytes(config
				.getEncoding());
		byte[] buf4 = "Accept-Encoding:gzip,deflate,sdch\r\nContent-Type:app"
				.getBytes(config.getEncoding());
		byte[] buf5 = "lication/x-www-form-urlencoded\r\nContent-Length:34\r\n\r\n"
				.getBytes(config.getEncoding());
		byte[] buf6 = "title=%E6%B5%8B%E8%AF%95&price=3.3".getBytes(config
				.getEncoding());

		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1),
				ByteBuffer.wrap(buf2), ByteBuffer.wrap(buf3),
				ByteBuffer.wrap(buf4), ByteBuffer.wrap(buf5),
				ByteBuffer.wrap(buf6) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}

		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		Assert.assertThat(req.getParameter("title"), is("测试"));
		Assert.assertThat(req.getParameter("price"), is("3.3"));
	}

	@Test
	public void testBody2() throws Throwable {
		byte[] buf1 = "POST /firefly-demo/app/hel".getBytes(config
				.getEncoding());
		byte[] buf2 = "lo HTTP/1.1\r\nHost:127.0.0.1\r\nAccept-Language:zh-CN,"
				.getBytes(config.getEncoding());
		byte[] buf3 = "zh;q=0.8\r\nConnection:keep-alive\r\n".getBytes(config
				.getEncoding());
		byte[] buf4 = "Accept-Encoding:gzip,deflate,sdch\r\nContent-Type:app"
				.getBytes(config.getEncoding());
		byte[] buf5 = "lication/x-www-form-urlencoded\r\nContent-Length:31\r\n\r\n"
				.getBytes(config.getEncoding());
		byte[] buf6 = "title=%E6%B5%8B%E8%AF%95&price=".getBytes(config
				.getEncoding());

		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1),
				ByteBuffer.wrap(buf2), ByteBuffer.wrap(buf3),
				ByteBuffer.wrap(buf4), ByteBuffer.wrap(buf5),
				ByteBuffer.wrap(buf6) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}

		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		Assert.assertThat(req.getParameter("title"), is("测试"));
		Assert.assertThat(req.getParameter("price"), is(""));
	}

	@Test
	public void testBody3() throws Throwable {
		byte[] buf1 = "POST /firefly-demo/app/hel".getBytes(config
				.getEncoding());
		byte[] buf2 = "lo HTTP/1.1\r\nHost:127.0.0.1\r\nAccept-Language:zh-CN,"
				.getBytes(config.getEncoding());
		byte[] buf3 = "zh;q=0.8\r\nConnection:keep-alive\r\n".getBytes(config
				.getEncoding());
		byte[] buf4 = "Accept-Encoding:gzip,deflate,sdch\r\nContent-Type:app"
				.getBytes(config.getEncoding());
		byte[] buf5 = "lication/x-www-form-urlencoded\r\nContent-Length:24\r\n\r\ntit"
				.getBytes(config.getEncoding());
		byte[] buf6 = "le=%E6%B5%8B%E8%AF%95".getBytes(config.getEncoding());

		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1),
				ByteBuffer.wrap(buf2), ByteBuffer.wrap(buf3),
				ByteBuffer.wrap(buf4), ByteBuffer.wrap(buf5),
				ByteBuffer.wrap(buf6) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}
		
		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		System.out.println(req.getParameter("title"));
		System.out.println(req.getLocale().toString());
		System.out.println(req.getRequestURL().toString());
		Assert.assertThat(req.getRequestURL().toString(), is("http://localhost/firefly-demo/app/hello"));
		Assert.assertThat(req.getLocale().toString(), is("zh_CN"));
		Assert.assertThat(req.getParameter("title"), is("测试"));
		Assert.assertThat(req.getParameter("price"), nullValue());
		Assert.assertThat(req.getContentLength(), is(24));
		Assert.assertThat(req.getContentType(), is("application/x-www-form-urlencoded"));
	}
	
	@Test
	public void testBody4() throws Throwable {
		byte[] buf1 = "POST /firefly-demo/app/hel".getBytes(config
				.getEncoding());
		byte[] buf2 = "lo HTTP/1.1\r\nHost:127.0.0.1\r\nAccept-Language:zh-CN,"
				.getBytes(config.getEncoding());
		byte[] buf3 = "zh;q=0.8\r\nConnection:keep-alive\r\n".getBytes(config
				.getEncoding());
		byte[] buf4 = "Accept-Encoding:gzip,deflate,sdch".getBytes(config
				.getEncoding());
		byte[] buf5 = "\r\nContent-Length:47\r\n\r\n".getBytes(config
				.getEncoding());
		byte[] buf6 = "| 90 | 测试 | 测试当前book | 3.3 | true |".getBytes(config
				.getEncoding());

		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1),
				ByteBuffer.wrap(buf2), ByteBuffer.wrap(buf3),
				ByteBuffer.wrap(buf4), ByteBuffer.wrap(buf5),
				ByteBuffer.wrap(buf6) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}

		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		ServletInputStream input = req.getInputStream();
		byte[] temp = new byte[30];
		byte[] data = null;
		for (int len = 0; (len = input.read(temp)) != -1;) {
			if (data == null) {
				data = new byte[len];
				System.arraycopy(temp, 0, data, 0, len);
			} else {
				byte[] pre = data;
				data = new byte[pre.length + len];
				System.arraycopy(pre, 0, data, 0, pre.length);
				System.arraycopy(temp, 0, data, pre.length, len);
			}
		}
		input.close();

		Assert.assertThat(new String(data, config.getEncoding()), is("| 90 | 测试 | 测试当前book | 3.3 | true |"));
		Assert.assertThat(data, is(buf6));
	}
	
	@Test
	public void testBody5() throws Throwable {
		byte[] buf1 = "POST /firefly-demo/app/hel".getBytes(config
				.getEncoding());
		byte[] buf2 = "lo HTTP/1.1\r\nHost:127.0.0.1\r\nAccept-Language:zh-CN,"
				.getBytes(config.getEncoding());
		byte[] buf3 = "zh;q=0.8\r\nConnection:keep-alive\r\n".getBytes(config
				.getEncoding());
		byte[] buf4 = "Accept-Encoding:gzip,deflate,sdch".getBytes(config
				.getEncoding());
		byte[] buf5 = "\r\nContent-Length:47\r\n\r\n".getBytes(config
				.getEncoding());
		byte[] buf6 = "| 90 | 测试 | 测试当前book | 3.3 | true |".getBytes(config
				.getEncoding());

		ByteBuffer[] buf = new ByteBuffer[] { ByteBuffer.wrap(buf1),
				ByteBuffer.wrap(buf2), ByteBuffer.wrap(buf3),
				ByteBuffer.wrap(buf4), ByteBuffer.wrap(buf5),
				ByteBuffer.wrap(buf6) };
		MockSession session = new MockSession();

		for (int i = 0; i < buf.length; i++) {
			httpDecoder.decode(buf[i], session);
		}

		List<HttpServletRequestImpl> list = session.request;
		Assert.assertThat(list.size(), is(1));
		
		HttpServletRequestImpl req = list.get(0);
		BufferedReader reader = req.getReader();
		StringBuilder sb = new StringBuilder();
		for (String line = null; (line = reader.readLine()) != null;) {
			sb.append(line);
		}
		reader.close();

		Assert.assertThat(sb.toString(), is("| 90 | 测试 | 测试当前book | 3.3 | true |"));
	}
	
	@Test
	public void testGetSessionId() {
		String sessionIdName = "jsessionid";
		String uri = "/app/hello;jsessionid=33342424jkl#apple";
		Assert.assertThat(HttpServletRequestImpl.getSessionId(uri, sessionIdName), is("33342424jkl"));
		uri = "/app/hello;jsessionid=33342424jkl";
		Assert.assertThat(HttpServletRequestImpl.getSessionId(uri, sessionIdName), is("33342424jkl"));
		
		uri = "http://www.firefly.com/app/hello?q=333";
		Assert.assertThat(HttpServletResponseImpl.toEncoded(uri, "ccccccccccccc", sessionIdName), is("http://www.firefly.com/app/hello;jsessionid=ccccccccccccc?q=333"));
		uri = "http://www.firefly.com/app/hello#ddddc?q=333";
		Assert.assertThat(HttpServletResponseImpl.toEncoded(uri, "ccccccccccccc", sessionIdName), is("http://www.firefly.com/app/hello;jsessionid=ccccccccccccc#ddddc?q=333"));
	}

	public static void main(String[] args) throws Throwable {
		TestHttpDecoder decode = new TestHttpDecoder();
		decode.testBody();
		
	}



}

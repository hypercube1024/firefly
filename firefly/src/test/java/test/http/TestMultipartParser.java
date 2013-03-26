package test.http;

import static org.hamcrest.Matchers.is;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.ServletInputStream;
import javax.servlet.http.Part;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.server.ServerBootstrap;
import com.firefly.server.http.MultipartFormData;
import com.firefly.server.http.MultipartFormDataParser;
import com.firefly.server.http.PartImpl;

public class TestMultipartParser {

	@Test
	public void testMultipartParser() throws Throwable {
		PartImpl.tempdir = new File(ServerBootstrap.class.getResource("/page/template/_firefly_tmpdir").toURI()).getAbsolutePath();
		File testFile = new File(ServerBootstrap.class.getResource("/83a6ad14-b318-4ffd-857c-d476129f3fb1").toURI());
		
		final BufferedInputStream in = new BufferedInputStream(new FileInputStream(testFile));
		Collection<Part> col = MultipartFormDataParser.parse(new ServletInputStream() {
			
			@Override
			public int read() throws IOException {
				return in.read();
			}
			
			@Override
			public int available() throws IOException {
				return in.available();
			}

			@Override
			public void close() throws IOException {
				in.close();
			}

			public int read(byte[] b, int off, int len) throws IOException {
				return in.read(b, off, len);
			}
		}, "multipart/form-data; boundary=----WebKitFormBoundarysQJ5u5rwAAwNHszq", "UTF-8");
		
		
		MultipartFormData m = new MultipartFormData(col);
		Assert.assertThat(col.size(), is(4));
		Assert.assertThat(m.getPart("price").getName(), is("price"));
		Assert.assertThat(m.getPart("price").getSize(), is(4L));

		in.close();
		m.close();
	}
}

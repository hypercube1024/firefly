package test.http;

import static org.hamcrest.Matchers.equalTo;

import java.io.ByteArrayInputStream;
import java.io.File;
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
import com.firefly.utils.codec.Base64;

public class TestMultipartParser {

	@Test
	public void testMultipartParser() throws Throwable {
		PartImpl.tempdir = new File(ServerBootstrap.class.getResource("/page/template/_firefly_tmpdir").toURI()).getAbsolutePath();
		String s = "LS0tLS0tV2ViS2l0Rm9ybUJvdW5kYXJ5bzZPV0pGWm9HOHc2MkxCTQ0KQ29udGVudC1EaXNwb3NpdGlvbjogZm9ybS1kYXRhOyBuYW1lPSJuYW1lIg0KDQrpgrHpuY/mu5QNCi0tLS0tLVdlYktpdEZvcm1Cb3VuZGFyeW82T1dKRlpvRzh3NjJMQk0NCkNvbnRlbnQtRGlzcG9zaXRpb246IGZvcm0tZGF0YTsgbmFtZT0ibnVtIg0KDQpzZGZmZXcyMzQNCi0tLS0tLVdlYktpdEZvcm1Cb3VuZGFyeW82T1dKRlpvRzh3NjJMQk0NCkNvbnRlbnQtRGlzcG9zaXRpb246IGZvcm0tZGF0YTsgbmFtZT0iY29udGVudDEiOyBmaWxlbmFtZT0i5rWL6K+VMi50eHQiDQpDb250ZW50LVR5cGU6IHRleHQvcGxhaW4NCg0K5rWL6K+V5paH5Lu25LiK5LygMQ0KLS0tLS0tV2ViS2l0Rm9ybUJvdW5kYXJ5bzZPV0pGWm9HOHc2MkxCTQ0KQ29udGVudC1EaXNwb3NpdGlvbjogZm9ybS1kYXRhOyBuYW1lPSJjb250ZW50MiI7IGZpbGVuYW1lPSJ0ZXN0MS50eHQiDQpDb250ZW50LVR5cGU6IHRleHQvcGxhaW4NCg0K5rWL6K+V5paH5Lu25LiK5LygMjENCi0tLS0tLVdlYktpdEZvcm1Cb3VuZGFyeW82T1dKRlpvRzh3NjJMQk0tLQ0K";
		
		final ByteArrayInputStream in = new ByteArrayInputStream(Base64.decode(s));
		Collection<Part> col = null;
		try {
			col = MultipartFormDataParser.parse(new ServletInputStream() {
			
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
			}, "multipart/form-data; boundary=----WebKitFormBoundaryo6OWJFZoG8w62LBM", "UTF-8");
		} finally {
			in.close();
		}
		
		MultipartFormData m = new MultipartFormData(col);
		try {
			Assert.assertThat(col.size(), equalTo(4));
			Assert.assertThat(m.getPart("name").getName(), equalTo("name"));
			Assert.assertThat(m.getPart("num").getSize(), equalTo(9L));
			Assert.assertThat(m.getPart("content1").getSize(), equalTo(19L));
			Assert.assertThat(m.getPart("content2").getSize(), equalTo(20L));
			System.out.println(m.getPart("content1").getSize());
			System.out.println(m.getPart("content2").getSize());
		} finally {
			m.close();
		}
	}
	
	public static void main(String[] args) throws Throwable {
//		new TestMultipartParser().testMultipartParser();
//		File testFile = new File(ServerBootstrap.class.getResource("/testMutilpart.txt").toURI());
//		BufferedInputStream input = new BufferedInputStream(new FileInputStream(testFile));
//		ByteArrayOutputStream out = new ByteArrayOutputStream();
//		
//		try {
//			byte[] buf = new byte[1024];
//			for (int len = 0; (len = input.read(buf)) != -1;) {
//				out.write(buf, 0, len);
//			}
//		} finally {
//			input.close();
//			out.close();
//		}
		
//		System.out.println(new String(out.toByteArray(), "UTF-8"));
//		System.out.println(Base64.encodeToString(out.toByteArray(), false));

		String s = "LS0tLS0tV2ViS2l0Rm9ybUJvdW5kYXJ5bzZPV0pGWm9HOHc2MkxCTQ0KQ29udGVudC1EaXNwb3NpdGlvbjogZm9ybS1kYXRhOyBuYW1lPSJuYW1lIg0KDQrpgrHpuY/mu5QNCi0tLS0tLVdlYktpdEZvcm1Cb3VuZGFyeW82T1dKRlpvRzh3NjJMQk0NCkNvbnRlbnQtRGlzcG9zaXRpb246IGZvcm0tZGF0YTsgbmFtZT0ibnVtIg0KDQpzZGZmZXcyMzQNCi0tLS0tLVdlYktpdEZvcm1Cb3VuZGFyeW82T1dKRlpvRzh3NjJMQk0NCkNvbnRlbnQtRGlzcG9zaXRpb246IGZvcm0tZGF0YTsgbmFtZT0iY29udGVudDEiOyBmaWxlbmFtZT0i5rWL6K+VMi50eHQiDQpDb250ZW50LVR5cGU6IHRleHQvcGxhaW4NCg0K5rWL6K+V5paH5Lu25LiK5LygMQ0KLS0tLS0tV2ViS2l0Rm9ybUJvdW5kYXJ5bzZPV0pGWm9HOHc2MkxCTQ0KQ29udGVudC1EaXNwb3NpdGlvbjogZm9ybS1kYXRhOyBuYW1lPSJjb250ZW50MiI7IGZpbGVuYW1lPSJ0ZXN0MS50eHQiDQpDb250ZW50LVR5cGU6IHRleHQvcGxhaW4NCg0K5rWL6K+V5paH5Lu25LiK5LygMjENCi0tLS0tLVdlYktpdEZvcm1Cb3VuZGFyeW82T1dKRlpvRzh3NjJMQk0tLQ0K";
		System.out.println(new String(Base64.decode(s), "UTF-8"));
	
	}
}

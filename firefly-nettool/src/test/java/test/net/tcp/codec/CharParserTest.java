package test.net.tcp.codec;

import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.net.tcp.codec.CharParser;

public class CharParserTest {

	@Test
	public void test() {
		String msg = "测试的字符串";
		StringBuilder ret = new StringBuilder();
		CharParser parser = new CharParser();
		parser.complete(str -> {
			ret.append(str);
			System.out.println(ret);
			if (ret.length() == msg.length()) {
				System.out.println("complete -> " + ret);
				Assert.assertThat(ret.toString(), is(msg));
			}
		});

		List<ByteBuffer> buffers = new ArrayList<>();
		ByteBuffer b = ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8));
		byte[] b1 = new byte[6];
		b.get(b1);
		buffers.add(ByteBuffer.wrap(b1));
		byte[] b2 = new byte[2];
		b.get(b2);
		buffers.add(ByteBuffer.wrap(b2));
		byte[] b3 = new byte[4];
		b.get(b3);
		buffers.add(ByteBuffer.wrap(b3));
		byte[] b4 = new byte[5];
		b.get(b4);
		buffers.add(ByteBuffer.wrap(b4));
		byte[] b5 = new byte[1];
		b.get(b5);
		buffers.add(ByteBuffer.wrap(b5));

		buffers.forEach(buffer -> {
			parser.receive(buffer);
		});

	}
}

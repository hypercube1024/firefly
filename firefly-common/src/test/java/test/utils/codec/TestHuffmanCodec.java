package test.utils.codec;

import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.codec.HexUtils;
import com.firefly.utils.codec.HuffmanCodec;
import com.firefly.utils.codec.HuffmanCodec.BitBuilder;
import com.firefly.utils.codec.HuffmanCodec.HuffmanCode;

public class TestHuffmanCodec {
	
	@Test
	public void test() {
		String str = "hello world! 测试一些测试";// "beep boop beer!";
		Character[] chars = new Character[str.length()];
		for (int i = 0; i < chars.length; i++) {
			chars[i] = str.charAt(i);
		}
		
		HuffmanCodec<Character> codec = new HuffmanCodec<Character>(chars);
		Map<Character, HuffmanCode> map = codec.getCodecMap();
		System.out.println(map);
		System.out.println("str length: " + str.getBytes(StandardCharsets.UTF_8).length);
		
		// encode
		BitBuilder bits = codec.encode(chars);
		System.out.println("encode length: " + bits.toByteArray().length + "| bit length: " + bits.getLength());
		System.out.println(HexUtils.bytesToHex(bits.toByteArray()));
		System.out.println(bits);
		System.out.println(BitSet.valueOf(bits.toByteArray()));
		
		// decode
		StringBuilder sb = new StringBuilder();
		List<Character> c = codec.decode(bits);
		for(char ch : c) {
			sb.append(ch);
		}
		System.out.println(sb);
		Assert.assertThat(sb.toString(), is(str));
	}
}

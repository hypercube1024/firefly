package test.codec.spdy.frames;

import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.spdy.decode.control.HeadersBlockParser;
import com.firefly.codec.spdy.frames.control.Fields;
import com.firefly.codec.spdy.frames.control.Fields.Field;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;

public class TestFields {
	
	@Test
	public void testFieldParser() {
		String name1 = "Key1";
		String value1 = "Value1";
		String name2 = "Key2";
		String value2 = "Value2\u0000Value2-2\u0000Value2-3\u0000Value2-4";
		String name3 = "Key3";
		String value3 = "testvalue3";
		
		Fields headers = new Fields(new HashMap<String, Field>());
		headers.put(name1, value1);
		String[] values = StringUtils.split(value2, "\u0000");
		for(String v : values) {
			if(VerifyUtils.isNotEmpty(v))
				headers.add(name2, v);
		}
		headers.put(name3, value3);
		
		ByteBuffer compressed = headers.toByteBuffer();
		Fields decompressed = HeadersBlockParser.DEFAULT_PARSER.parse(1, compressed.remaining(), compressed, new MockSession());
		
		Assert.assertThat(decompressed.getSize(), is(headers.getSize()));
		Assert.assertThat(decompressed.get(name1), is(headers.get(name1)));
		Assert.assertThat(decompressed.get(name2), is(headers.get(name2)));
		Assert.assertThat(decompressed.get(name3), is(headers.get(name3)));
		Assert.assertThat(decompressed, is(headers));
	}
	
	@Test
	public void testFields() {
		String name = "Get-TEST";
		String valueString = "value1\u0000APPLE2\u0000WINDOWS";
		Fields headers = new Fields(new HashMap<String, Field>());
		
		String[] values = StringUtils.split(valueString, "\u0000");
		for(String v : values) {
			if(VerifyUtils.isNotEmpty(v))
				headers.add(name, v);
		}
		Field field = headers.get(name);
		Assert.assertThat(field.getName(), is("get-test"));
		Assert.assertThat(field.getValues().size(), is(3));
		Assert.assertThat(field.getValues().get(0), is("value1"));
		Assert.assertThat(field.getValues().get(1), is("APPLE2"));
		Assert.assertThat(field.getValues().get(2), is("WINDOWS"));
		
		headers.put("get2", "v2");
		field = headers.get("get2");
		Assert.assertThat(field.getValue(), is("v2"));
	}
	
	public static void main(String[] args) {
		System.out.println("sssss\u0000AAAAA");
		System.out.println(Arrays.toString(StringUtils.split("sssss\u0000AAAAA", "\u0000")));
	}
}

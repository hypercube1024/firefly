package test.utils.json.reader;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.json.support.JsonStringReader;

import static org.hamcrest.Matchers.*;

public class TestReader {
	@Test
	public void testReadAndSkipBlank() {
		JsonStringReader reader = new JsonStringReader("  tt");
		Assert.assertThat(reader.readAndSkipBlank(), is('t'));
		Assert.assertThat(reader.position(), is(3));
	}
	
	@Test
	public void testReadField() {
		JsonStringReader reader = new JsonStringReader("  \"testField\":");
		char[] t1 = "test".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.get(reader.position()), is(':'));
	}
	
	@Test
	public void testReadField2() {
		JsonStringReader reader = new JsonStringReader("  \"testField\" :");
		char[] t1 = "testField".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(ret, nullValue());
		Assert.assertThat(reader.isColon(), is(true));
	}
	
	@Test
	public void testReadField3() {
		JsonStringReader reader = new JsonStringReader("  \"testField\":");
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
	}
	
	@Test
	public void testReadInt() {
		JsonStringReader reader = new JsonStringReader("  { \"testField\": 333 }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readInt(), is(333));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
	}
	
	@Test
	public void testReadLong() {
		JsonStringReader reader = new JsonStringReader("  { \"testField\": -3334}");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readLong(), is(-3334L));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
	}
	
	public static void main(String[] args) {
//		char[] str = "0123456789".toCharArray();
//		for (int i = 0; i < str.length; i++) {
//			System.out.println(str[i] - '0');
//		}
		
		char[] str = "833".toCharArray();
		int value = 0;
		for (int i = 0; i < str.length; i++) {
			value = (value << 3) + (value << 1) + (str[i] - '0');
			System.out.println(value);
		}
		System.out.println(value);
		
		char c = '-';
		System.out.println("char is " + c);
	}
}

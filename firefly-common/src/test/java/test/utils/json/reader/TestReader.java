package test.utils.json.reader;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.json.support.JsonStringReader;

import static org.hamcrest.Matchers.*;

public class TestReader {
	@Test
	public void testReadAndSkipBlank() {
		JsonStringReader reader = new JsonStringReader("  tt".trim());
		Assert.assertThat(reader.readAndSkipBlank(), is('t'));
		Assert.assertThat(reader.position(), is(1));
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
	public void testReadInt2() {
		JsonStringReader reader = new JsonStringReader("  { \"testField\": \" -333\" , \"testField2\": \" -334\" }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readInt(), is(-333));
		Assert.assertThat(reader.isComma(), is(true));
		
		ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField2"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readInt(), is(-334));
		Assert.assertThat(reader.isObjectEnd(), is(true));
	}
	
	@Test
	public void testReadInt3() {
		JsonStringReader reader = new JsonStringReader("  \" -333\" ");
		Assert.assertThat(reader.readInt(), is(-333));
	}
	
	@Test
	public void testReadInt4() {
		JsonStringReader reader = new JsonStringReader("   -333 ");
		Assert.assertThat(reader.readInt(), is(-333));
	}
	
	@Test
	public void testReadInt5() {
		JsonStringReader reader = new JsonStringReader("  { \"testField\": null }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readInt(), is(0));
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
	
	@Test
	public void testReadString() {
		JsonStringReader reader = new JsonStringReader("  { \"testField\": \"ddsfseee\",  \"testField2\": \"dddfd\"}");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readString(), is("ddsfseee"));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is(','));
		
		ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField2"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readString(), is("dddfd"));
		ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
	}
	
	@Test
	public void testReadString2() {
		JsonStringReader reader = new JsonStringReader("  { \"testField\": \"dds\\\"fseee\",  \"testField2\": \"d\\nddfd\"}");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		String s = reader.readString();
		System.out.println(s);
		Assert.assertThat(s, is("dds\"fseee"));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is(','));
		
		ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField2"));
		Assert.assertThat(reader.isColon(), is(true));
		s = reader.readString();
		System.out.println(s);
		Assert.assertThat(s, is("d\nddfd"));
		ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
	}
	
	@Test
	public void testReadString3() {
		JsonStringReader reader = new JsonStringReader("  { \"testField\": null,  \"testField2\": \"d\\nddfd\"}");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		String s = reader.readString();
		Assert.assertThat(s, nullValue());
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is(','));
	}
	
	@Test
	public void testIsNull() {
		JsonStringReader reader = new JsonStringReader("  { \"testField\": null ,  \"testField2\": \"d\\nddfd\"}");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.isNull(), is(true));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is(','));
		
		ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField2"));
		Assert.assertThat(reader.isColon(), is(true));
		String s = reader.readString();
		System.out.println(s);
		Assert.assertThat(s, is("d\nddfd"));
		ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
	}
	
	@Test
	public void testIsNull2() {
		JsonStringReader reader = new JsonStringReader("  null ".trim());
		Assert.assertThat(reader.isNull(), is(true));
	}
	
	@Test
	public void testIsNull3() {
		JsonStringReader reader = new JsonStringReader(" nul");
		Assert.assertThat(reader.isNull(), is(false));
	}
	
	@Test
	public void testReadBoolean() {
		JsonStringReader reader = new JsonStringReader("{ \"testField\": true}");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readBoolean(), is(true));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
	}
	
	@Test
	public void testReadBoolean2() {
		JsonStringReader reader = new JsonStringReader("{ \"testField\": false }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readBoolean(), is(false));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
	}
	
	@Test
	public void testReadBoolean3() {
		JsonStringReader reader = new JsonStringReader("{ \"testField\": \"true\" }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readBoolean(), is(true));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
	}
	
	@Test
	public void testReadBoolean4() {
		JsonStringReader reader = new JsonStringReader("{ \"testField\": \"false\" }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readBoolean(), is(false));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
	}
	
	@Test
	public void testReadBoolean5() {
		JsonStringReader reader = new JsonStringReader("{ \"testField\": null }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readBoolean(), is(false));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
	}

}

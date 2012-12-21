package test.utils.json.reader;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.json.JsonReader;
import com.firefly.utils.json.io.JsonStringReader;

import static org.hamcrest.Matchers.*;

public class TestReader {
	@Test
	public void testReadAndSkipBlank() throws IOException {
		JsonReader reader = new JsonStringReader("  tt".trim());
		Assert.assertThat(reader.readAndSkipBlank(), is('t'));
//		Assert.assertThat(reader.position(), is(1));
		reader.close();
	}
	
	@Test
	public void testReadField() throws IOException {
		JsonReader reader = new JsonStringReader("  \"testField\":");
		char[] t1 = "test".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
//		Assert.assertThat(reader.get(reader.position()), is(':'));
		reader.close();
	}
	
	@Test
	public void testReadField2() throws IOException {
		JsonReader reader = new JsonStringReader("  \"testField\" :");
		char[] t1 = "testField".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(ret, nullValue());
		Assert.assertThat(reader.isColon(), is(true));
		reader.close();
	}
	
	@Test
	public void testReadField3() throws IOException {
		JsonReader reader = new JsonStringReader("  \"testField\":");
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		reader.close();
	}
	
	@Test
	public void testReadDouble() throws IOException {
		JsonReader reader = new JsonStringReader("  { \"testField\": 3332.44 }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readDouble(), is(3332.44));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
		reader.close();
	}
	
	@Test
	public void testReadDouble2() throws IOException {
		JsonReader reader = new JsonStringReader("  { \"testField\": -17.44320 , \"testField2\": \" -334\" }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readDouble(), is(-17.44320));
		Assert.assertThat(reader.isComma(), is(true));
		
		ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField2"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readInt(), is(-334));
		reader.close();
	}
	
	@Test
	public void testReadFloat() throws IOException {
		JsonReader reader = new JsonStringReader("  { \"testField\": \" -17.44320\" , \"testField2\": \" -334\" }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readFloat(), is(-17.44320F));
		Assert.assertThat(reader.isComma(), is(true));
		
		ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField2"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readInt(), is(-334));
		reader.close();
	}
	
	@Test
	public void testReadInt() throws IOException {
		JsonReader reader = new JsonStringReader("  { \"testField\": 333 }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readInt(), is(333));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
		reader.close();
	}
	
	@Test
	public void testReadInt2() throws IOException {
		JsonReader reader = new JsonStringReader("  { \"testField\": \" -333\" , \"testField2\": \" -334\" }");
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
		reader.close();
	}
	
	@Test
	public void testReadInt3() throws IOException {
		JsonReader reader = new JsonStringReader("  \" -333\" ");
		Assert.assertThat(reader.readInt(), is(-333));
		reader.close();
	}
	
	@Test
	public void testReadInt4() throws IOException {
		JsonReader reader = new JsonStringReader("   -333 ");
		Assert.assertThat(reader.readInt(), is(-333));
		reader.close();
	}
	
	@Test
	public void testReadInt5() throws IOException {
		JsonReader reader = new JsonStringReader("  { \"testField\": null }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readInt(), is(0));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
		reader.close();
	}
	
	@Test
	public void testReadLong() throws IOException {
		JsonReader reader = new JsonStringReader("  { \"testField\": -3334}");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readLong(), is(-3334L));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
		reader.close();
	}
	
	@Test
	public void testReadString() throws IOException {
		JsonReader reader = new JsonStringReader("  { \"testField\": \"ddsfseee\",  \"testField2\": \"dddfd\"}");
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
		reader.close();
	}
	
	@Test
	public void testReadString2() throws IOException {
		JsonReader reader = new JsonStringReader("  { \"testField\": \"dds\\\"fseee\",  \"testField2\": \"d\\nddfd\"}");
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
		reader.close();
	}
	
	@Test
	public void testReadString3() throws IOException {
		JsonReader reader = new JsonStringReader("  { \"testField\": null,  \"testField2\": \"d\\nddfd\"}");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		String s = reader.readString();
		Assert.assertThat(s, nullValue());
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is(','));
		reader.close();
	}
	
	@Test
	public void testIsNull() throws IOException {
		JsonReader reader = new JsonStringReader("  { \"testField\": null ,  \"testField2\": \"d\\nddfd\"}");
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
		reader.close();
	}
	
	@Test
	public void testIsNull2() throws IOException {
		JsonReader reader = new JsonStringReader("  null ".trim());
		Assert.assertThat(reader.isNull(), is(true));
		reader.close();
	}
	
	@Test
	public void testIsNull3() throws IOException {
		JsonReader reader = new JsonStringReader(" nul");
		Assert.assertThat(reader.isNull(), is(false));
		reader.close();
	}
	
	@Test
	public void testReadBoolean() throws IOException {
		JsonReader reader = new JsonStringReader("{ \"testField\": true}");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readBoolean(), is(true));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
		reader.close();
	}
	
	@Test
	public void testReadBoolean2() throws IOException {
		JsonReader reader = new JsonStringReader("{ \"testField\": false }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readBoolean(), is(false));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
		reader.close();
	}
	
	@Test
	public void testReadBoolean3() throws IOException {
		JsonReader reader = new JsonStringReader("{ \"testField\": \"true\" }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readBoolean(), is(true));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
		reader.close();
	}
	
	@Test
	public void testReadBoolean4() throws IOException {
		JsonReader reader = new JsonStringReader("{ \"testField\": \"false\" }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readBoolean(), is(false));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
		reader.close();
	}
	
	@Test
	public void testReadBoolean5() throws IOException {
		JsonReader reader = new JsonStringReader("{ \"testField\": null }");
		Assert.assertThat(reader.isObject(), is(true));
		char[] t1 = "dsffsfsf".toCharArray();
		char[] ret = reader.readField(t1);
		Assert.assertThat(new String(ret), is("testField"));
		Assert.assertThat(reader.isColon(), is(true));
		Assert.assertThat(reader.readBoolean(), is(false));
		char ch = reader.readAndSkipBlank();
		Assert.assertThat(ch, is('}'));
		reader.close();
	}
	
	@Test
	public void testSkipValue() throws IOException {
		JsonReader reader = new JsonStringReader("{ \"testField\": null, \"ssdd\" : \"sdf\\\"sdfsdf\" }");
		Assert.assertThat(reader.isObject(), is(true));
		Assert.assertThat(Arrays.equals("testField".toCharArray(), reader.readChars()), is(true));
		Assert.assertThat(reader.isColon(), is(true));
		reader.skipValue();
		
		Assert.assertThat(reader.isComma(), is(true));
		Assert.assertThat(Arrays.equals("ssdd".toCharArray(), reader.readChars()), is(true));
		Assert.assertThat(reader.isColon(), is(true));
		reader.skipValue();
		reader.close();
	}
	
	@Test
	public void testSkipValue2() throws IOException {
		JsonReader reader = new JsonStringReader("{ \"testField\": [ [[2 , 3],[3]], [[3,4]] ], \"ssdd\" : \"sdf\\\"sdfsdf\" }");
		Assert.assertThat(reader.isObject(), is(true));
		Assert.assertThat(Arrays.equals("testField".toCharArray(), reader.readChars()), is(true));
		Assert.assertThat(reader.isColon(), is(true));
		reader.skipValue();
		
		Assert.assertThat(reader.isComma(), is(true));
		Assert.assertThat(Arrays.equals("ssdd".toCharArray(), reader.readChars()), is(true));
		Assert.assertThat(reader.isColon(), is(true));
		reader.skipValue();
		reader.close();
	}
	
	@Test
	public void testSkipValue3() throws IOException {
		JsonReader reader = new JsonStringReader("{ \"testField\": [ [[{} , {\"t1\" : { \"t2\": {\"t3\" : [\"332f\", \"dsfdsf\\\"sd\"] } } }],[]], [[3,4]] ], \"ssdd\" : \"sdf\\\"sdfsdf\" }");
		Assert.assertThat(reader.isObject(), is(true));
		Assert.assertThat(Arrays.equals("testField".toCharArray(), reader.readChars()), is(true));
		Assert.assertThat(reader.isColon(), is(true));
		reader.skipValue();
		
		Assert.assertThat(reader.isComma(), is(true));
		Assert.assertThat(Arrays.equals("ssdd".toCharArray(), reader.readChars()), is(true));
		Assert.assertThat(reader.isColon(), is(true));
		reader.skipValue();
		reader.close();
	}
	
	public static void main(String[] args) throws IOException {
		JsonReader reader = new JsonStringReader("{ \"testField\": [ [[{} , {\"t1\" : { \"t2\": {\"t3\" : [\"332f\", \"dsfdsf\\\"sd\"] } } }],[]], [[3,4]] ], \"ssdd\" : \"sdf\\\"sdfsdf\" }");
		reader.isObject();
		reader.readChars();
		reader.isColon();
		reader.skipValue();
		System.out.println(reader.isComma());
		reader.close();
	}

}

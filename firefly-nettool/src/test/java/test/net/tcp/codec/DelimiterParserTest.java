package test.net.tcp.codec;

import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.net.tcp.codec.DelimiterParser;

public class DelimiterParserTest {

	@Test
	public void test() {
		StringBuilder str = new StringBuilder();
		DelimiterParser parser = new DelimiterParser("||");
		parser.complete(msg -> {
			System.out.println(msg);
			str.append(msg);
		});

		List<String> list = new ArrayList<>();
		list.add("哈哈哈||dfsfs||e");
		list.add("mail||");
		list.add("hello world||");
		list.forEach(parser::receive);
		Assert.assertThat(str.toString(), is("哈哈哈dfsfsemailhello world"));
	}
}

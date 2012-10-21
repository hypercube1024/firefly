package test.utils;

import static org.hamcrest.Matchers.*;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.mvc.web.support.URLParser;

public class TestURLParser {
	
	@Test
	public void testParser() {
		List<String> uri = URLParser.parse("/app/index/");
		System.out.println(uri);
		Assert.assertThat(uri.get(0), is("app"));
		Assert.assertThat(uri.get(1), is("index"));
		
		uri = URLParser.parse("/app/q_{}_{}.html");
		System.out.println(uri);
		Assert.assertThat(uri.get(0), is("app"));
		Assert.assertThat(uri.get(1), is("q_{}_{}.html"));
		
		uri = URLParser.parse("/apple");
		Assert.assertThat(uri.get(0), is("apple"));
	}
	
	
}

package test.utils.pattern;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.pattern.Pattern;

public class TestPattern {
	@Test
	public void testPattern() {
		Pattern p = Pattern.compile("?ddaaad?", "?");
		Assert.assertThat(p.match("ddaaad")[1], is(""));
		Assert.assertThat(p.match("ddaaadxwww")[1], is("xwww"));
		Assert.assertThat(p.match("addaaadxwww")[1], is("xwww"));
		Assert.assertThat(p.match("addaaadxwww")[0], is("a"));
		Assert.assertThat(p.match("addaaad")[0], is("a"));
		Assert.assertThat(p.match("orange"), nullValue());
		
		p = Pattern.compile("?", "?");
		Assert.assertThat(p.match("orange")[0], is("orange"));
		
		p = Pattern.compile("??????", "?");
		Assert.assertThat(p.match("orange")[0], is("orange"));
		Assert.assertThat(p.match("orange").length, is(1));
		
		p = Pattern.compile("org", "?");
		Assert.assertThat(p.match("orange"), nullValue());
		Assert.assertThat(p.match("org").length, is(0));
		
		p = Pattern.compile("?org", "?");
		Assert.assertThat(p.match("org")[0], is(""));
		Assert.assertThat(p.match("aassorg")[0], is("aass"));
		Assert.assertThat(p.match("ssorg").length, is(1));
		
		p = Pattern.compile("org?", "?");
		Assert.assertThat(p.match("org")[0], is(""));
		Assert.assertThat(p.match("orgaaa")[0], is("aaa"));
		Assert.assertThat(p.match("orgaaa").length, is(1));
		
		p = Pattern.compile("www.?.com?", "?");
		Assert.assertThat(p.match("www.fireflysource.com")[0], is("fireflysource"));
		Assert.assertThat(p.match("www.fireflysource.com")[1], is(""));
		Assert.assertThat(p.match("www.fireflysource.com/cn/")[1], is("/cn/"));
		Assert.assertThat(p.match("www.fireflysource.com/cn/").length, is(2));
		Assert.assertThat(p.match("orange"), nullValue());
		
		p = Pattern.compile("www.?.com/?/app", "?");
		Assert.assertThat(p.match("orange"), nullValue());
		Assert.assertThat(p.match("www.fireflysource.com/cn/app").length, is(2));
		Assert.assertThat(p.match("www.fireflysource.com/cn/app")[0], is("fireflysource"));
		Assert.assertThat(p.match("www.fireflysource.com/cn/app")[1], is("cn"));
		
		p = Pattern.compile("?www.?.com/?/app", "?");
		Assert.assertThat(p.match("orange"), nullValue());
		Assert.assertThat(p.match("www.fireflysource.com/cn/app").length, is(3));
		Assert.assertThat(p.match("www.fireflysource.com/cn/app")[0], is(""));
		Assert.assertThat(p.match("www.fireflysource.com/cn/app")[1], is("fireflysource"));
		Assert.assertThat(p.match("www.fireflysource.com/cn/app")[2], is("cn"));
		Assert.assertThat(p.match("http://www.fireflysource.com/cn/app")[0], is("http://"));
		
		p = Pattern.compile("?www.?.com/?/app?", "?");
		Assert.assertThat(p.match("orange"), nullValue());
		Assert.assertThat(p.match("www.fireflysource.com/cn/app").length, is(4));
		Assert.assertThat(p.match("www.fireflysource.com/cn/app")[0], is(""));
		Assert.assertThat(p.match("www.fireflysource.com/cn/app")[1], is("fireflysource"));
		Assert.assertThat(p.match("www.fireflysource.com/cn/app")[2], is("cn"));
		Assert.assertThat(p.match("http://www.fireflysource.com/cn/app")[0], is("http://"));
		Assert.assertThat(p.match("http://www.fireflysource.com/cn/app")[3], is(""));
		Assert.assertThat(p.match("http://www.fireflysource.com/cn/app/1334")[3], is("/1334"));
		
		p = Pattern.compile("abc*abc", "*");
		Assert.assertThat(p.match("abcabcabc")[0], is(""));
		
		p = Pattern.compile("aa*aa", "*");
		Assert.assertThat(p.match("aaaaa")[0], is(""));
	}
}

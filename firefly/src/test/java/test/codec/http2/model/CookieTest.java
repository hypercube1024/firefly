package test.codec.http2.model;

import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.model.Cookie;
import com.firefly.codec.http2.model.CookieGenerator;
import com.firefly.codec.http2.model.CookieParser;

public class CookieTest {
	
	@Test
	public void setCookieTest() {
		Cookie cookie = new Cookie("test31", "hello");
		cookie.setDomain("www.fireflysource.com");
		cookie.setPath("/test/hello");
		cookie.setMaxAge(10);
		cookie.setSecure(true);
		cookie.setComment("commenttest");
		cookie.setVersion(20);
		
		String setCookieString = CookieGenerator.generateSetCookie(cookie);
		
		Cookie setCookie = CookieParser.parseSetCookie(setCookieString);
		Assert.assertThat(setCookie.getName(), is("test31"));
		Assert.assertThat(setCookie.getValue(), is("hello"));
		Assert.assertThat(setCookie.getDomain(), is("www.fireflysource.com"));
		Assert.assertThat(setCookie.getPath(), is("/test/hello"));
		Assert.assertThat(setCookie.getSecure(), is(true));
		Assert.assertThat(setCookie.getComment(), is("commenttest"));
		Assert.assertThat(setCookie.getVersion(), is(20));
	}

	@Test
	public void cookieTest() {
		Cookie cookie = new Cookie("test21", "hello");
		String cookieString = CookieGenerator.generateCookie(cookie);
		
		List<Cookie> list = CookieParser.parseCookie(cookieString);
		Assert.assertThat(list.size(), is(1));
		Assert.assertThat(list.get(0).getName(), is("test21"));
		Assert.assertThat(list.get(0).getValue(), is("hello"));
	}
	
	@Test
	public void cookieListTest() {
		List<Cookie> list = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			list.add(new Cookie("test" + i, "hello" + i));
		}
		String cookieString = CookieGenerator.generateCookies(list);
		
		List<Cookie> ret = CookieParser.parseCookie(cookieString);
		Assert.assertThat(ret.size(), is(10));
		for (int i = 0; i < 10; i++) {
			Assert.assertThat(ret.get(i).getName(), is("test" + i));
			Assert.assertThat(ret.get(i).getValue(), is("hello" + i));
		}
	}
}

package test.http;

import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.server.http2.servlet.utils.ClientIPUtils;

public class TestClientIPUtils {

	@Test
	public void test() {
		String ip = "192.168.1.110, 192.168.1.120, 192.168.1.130, 192.168.1.100";
		String remoteAddr = ClientIPUtils.parseRemoteAddr(ip);
		Assert.assertThat(remoteAddr, is("192.168.1.110"));
		
		ip = "192.168.1.120";
		remoteAddr = ClientIPUtils.parseRemoteAddr(ip);
		Assert.assertThat(remoteAddr, is("192.168.1.120"));
		
		ip = "unknown, 192.168.1.120, 192.168.1.110";
		remoteAddr = ClientIPUtils.parseRemoteAddr(ip);
		Assert.assertThat(remoteAddr, is("192.168.1.120"));
		
		ip = "unknown, unknown, 192.168.1.120, 192.168.1.110";
		remoteAddr = ClientIPUtils.parseRemoteAddr(ip);
		Assert.assertThat(remoteAddr, is("192.168.1.120"));
	}
}

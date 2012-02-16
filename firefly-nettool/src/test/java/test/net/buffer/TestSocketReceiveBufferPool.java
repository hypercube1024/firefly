package test.net.buffer;

import org.junit.Assert;
import org.junit.Test;
import com.firefly.net.buffer.SocketReceiveBufferPool;
import static org.hamcrest.Matchers.*;


public class TestSocketReceiveBufferPool {
//	private static Logger log = LoggerFactory.getLogger(TestSocketReceiveBufferPool.class);
	
	@Test
	public void testNormalizeCapacity() {
		Assert.assertThat(SocketReceiveBufferPool.normalizeCapacity(5), is(1024));
		Assert.assertThat(SocketReceiveBufferPool.normalizeCapacity(1023), is(1024));
		Assert.assertThat(SocketReceiveBufferPool.normalizeCapacity(1024), is(1024));
		Assert.assertThat(SocketReceiveBufferPool.normalizeCapacity(70), is(1024));
		Assert.assertThat(SocketReceiveBufferPool.normalizeCapacity(1025), is(1024 * 2));
		Assert.assertThat(SocketReceiveBufferPool.normalizeCapacity(1900), is(1024 * 2));
		Assert.assertThat(SocketReceiveBufferPool.normalizeCapacity(2048), is(1024 * 2));
		Assert.assertThat(SocketReceiveBufferPool.normalizeCapacity(2049), is(1024 * 3));
		Assert.assertThat(SocketReceiveBufferPool.normalizeCapacity(5000), is(1024 * 5));
	}
}

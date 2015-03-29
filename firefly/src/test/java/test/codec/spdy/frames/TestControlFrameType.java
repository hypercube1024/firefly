package test.codec.spdy.frames;

import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.spdy.frames.ControlFrameType;

public class TestControlFrameType {

	@Test
	public void testControlFrameType() {
		Assert.assertThat(ControlFrameType.from((short)1), is(ControlFrameType.SYN_STREAM));
		Assert.assertThat(ControlFrameType.from((short)6), is(ControlFrameType.PING));
		Assert.assertThat(ControlFrameType.from((short)10), is(ControlFrameType.CREDENTIAL));
		
		Assert.assertThat(ControlFrameType.SYN_STREAM.getCode(), is((short)1));
		Assert.assertThat(ControlFrameType.PING.getCode(), is((short)6));
		Assert.assertThat(ControlFrameType.CREDENTIAL.getCode(), is((short)10));
	}
	
}

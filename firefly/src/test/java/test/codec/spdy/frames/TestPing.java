package test.codec.spdy.frames;

import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.spdy.decode.SpdyDecoder;
import com.firefly.codec.spdy.decode.SpdyDecodingEventListener;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.spdy.frames.control.PingFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.SettingsFrame;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.frames.control.WindowUpdateFrame;
import com.firefly.net.Session;

public class TestPing extends TestBase{

	@Test
	public void testPing() throws Throwable {
		try(SpdySessionAttachment attachment = new SpdySessionAttachment()) {
			MockSession session = new MockSession();
			session.attachObject(attachment);
			
			final PingFrame s = newInstance();
			SpdyDecoder decoder = new SpdyDecoder(new PingFrameEvent(){

				@Override
				public void onPing(PingFrame pingFrame, Session session) {
					System.out.println("receive ping frame: " + pingFrame);
					Assert.assertThat(pingFrame, is(s));
				}});
			testSpdyFrame(decoder, s, session);
		}
	}
	
	public static PingFrame newInstance() {
		return new PingFrame((short)3, 20);
	}
	
	abstract static class PingFrameEvent implements SpdyDecodingEventListener {
		@Override
		public void onSynStream(SynStreamFrame synStreamFrame, Session session) {}

		@Override
		public void onRstStream(RstStreamFrame rstStreamFrame,Session session) {}

		@Override
		public void onSynReply(SynReplyFrame synReplyFrame, Session session) {}

		@Override
		public void onSettings(SettingsFrame settingsFrame, Session session) {}

		@Override
		public void onGoAway(GoAwayFrame goAwayFrame, Session session) {}

		@Override
		public void onHeaders(HeadersFrame headersFrame, Session session) {}

		@Override
		public void onWindowUpdate(WindowUpdateFrame windowUpdateFrame,Session session) {}

		@Override
		public void onData(DataFrame dataFrame, Session session) {}
	}
}

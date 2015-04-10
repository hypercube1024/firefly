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
import com.firefly.codec.spdy.stream.Connection;
import com.firefly.net.Session;

public class TestWindowUpdate extends TestBase {

	@Test
	public void testWindowUpdate() throws Throwable {
		MockSession session = new MockSession();
		Connection connection = new Connection(session, false);
		try(SpdySessionAttachment attachment = new SpdySessionAttachment(connection)) {
			session.attachObject(attachment);
			
			final WindowUpdateFrame s = newInstance();
			SpdyDecoder decoder = new SpdyDecoder(new WindowUpdateEvent(){

				@Override
				public void onWindowUpdate(WindowUpdateFrame windowUpdateFrame,
						Session session) {
					System.out.println("receive window update frame: " + windowUpdateFrame);
					Assert.assertThat(windowUpdateFrame, is(s));
				}});
			testSpdyFrame(decoder, s, session);
		}
	}
	
	public static WindowUpdateFrame newInstance() {
		WindowUpdateFrame windowUpdateFrame = new WindowUpdateFrame((short)3, 2427, 70000);
		return windowUpdateFrame;
	}
	
	abstract static class WindowUpdateEvent implements SpdyDecodingEventListener {
		@Override
		public void onSynStream(SynStreamFrame synStreamFrame, Session session) {}

		@Override
		public void onRstStream(RstStreamFrame rstStreamFrame,Session session) {}

		@Override
		public void onSettings(SettingsFrame settings, Session session) {}

		@Override
		public void onPing(PingFrame pingFrame, Session session) {}

		@Override
		public void onGoAway(GoAwayFrame goAwayFrame, Session session) {}

		@Override
		public void onSynReply(SynReplyFrame synReplyFrame, Session session) {}

		@Override
		public void onHeaders(HeadersFrame headersFrame, Session session) {}

		@Override
		public void onData(DataFrame dataFrame, Session session) {}
	}
}

package test.codec.spdy.frames;

import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.spdy.decode.SpdyDecoder;
import com.firefly.codec.spdy.decode.SpdyDecodingEvent;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.spdy.frames.control.PingFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.SessionStatus;
import com.firefly.codec.spdy.frames.control.SettingsFrame;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.frames.control.WindowUpdateFrame;
import com.firefly.net.Session;

public class TestGoAway extends TestBase {

	@Test
	public void testGoAway() throws Throwable {
		try(SpdySessionAttachment attachment = new SpdySessionAttachment()) {
			MockSession session = new MockSession();
			session.attachObject(attachment);
			
			final GoAwayFrame s = newInstance();
			SpdyDecoder decoder = new SpdyDecoder(new GoAwayFrameEvent(){

				@Override
				public void onGoAway(GoAwayFrame goAwayFrame, Session session) {
					System.out.println("receive go away frame: " + goAwayFrame);
					Assert.assertThat(goAwayFrame, is(s));
				}});
			testSpdyFrame(decoder, s, session);
		}
	}
	
	@Test
	public void testSessionStatus() {
		Assert.assertThat(SessionStatus.from(0), is(SessionStatus.OK));
		Assert.assertThat(SessionStatus.from(1), is(SessionStatus.PROTOCOL_ERROR));
		Assert.assertThat(SessionStatus.from(2), is(SessionStatus.INTERNAL_ERROR));
	}
	
	public static GoAwayFrame newInstance() {
		GoAwayFrame goAwayFrame = new GoAwayFrame((short)3, 77, SessionStatus.PROTOCOL_ERROR);
		return goAwayFrame;
	}
	
	abstract static class GoAwayFrameEvent implements SpdyDecodingEvent {
		@Override
		public void onSynStream(SynStreamFrame synStreamFrame, Session session) {}

		@Override
		public void onRstStream(RstStreamFrame rstStreamFrame,Session session) {}

		@Override
		public void onSynReply(SynReplyFrame synReplyFrame, Session session) {}

		@Override
		public void onSettings(SettingsFrame settingsFrame, Session session) {}

		@Override
		public void onPing(PingFrame pingFrame, Session session) {}

		@Override
		public void onHeaders(HeadersFrame headersFrame, Session session) {}

		@Override
		public void onWindowUpdate(WindowUpdateFrame windowUpdateFrame,Session session) {}

		@Override
		public void onData(DataFrame dataFrame, Session session) {}
	}
}

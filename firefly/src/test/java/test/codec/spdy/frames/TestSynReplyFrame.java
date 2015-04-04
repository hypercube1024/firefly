package test.codec.spdy.frames;

import static org.hamcrest.Matchers.is;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.spdy.decode.SpdyDecoder;
import com.firefly.codec.spdy.decode.SpdyDecodingEvent;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.control.Fields;
import com.firefly.codec.spdy.frames.control.Fields.Field;
import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.spdy.frames.control.PingFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.SettingsFrame;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.frames.control.WindowUpdateFrame;
import com.firefly.net.Session;

public class TestSynReplyFrame extends TestBase {

	@Test
	public void testSynReplyFrame() throws Throwable {
		try(SpdySessionAttachment attachment = new SpdySessionAttachment()) {
			MockSession session = new MockSession();
			session.attachObject(attachment);
			
			final SynReplyFrame s = newInstance(attachment);
			SpdyDecoder decoder = new SpdyDecoder(new SynReplyEvent(){

				@Override
				public void onSynReply(SynReplyFrame synReplyFrame, Session session) {
					System.out.println("receive syn reply frame: " + synReplyFrame);
					Assert.assertThat(synReplyFrame, is(s));
				}});
			
			testControlFrame(decoder, s, session);
		}
	}
	
	public static SynReplyFrame newInstance(SpdySessionAttachment attachment) {
		Fields headers = new Fields(new HashMap<String, Field>(), attachment.headersBlockGenerator);
		headers.put("testReply1", "testReplyValue1");
		headers.put("testReply2", "testReplyValue2");		
		for (int i = 0; i < 15; i++) {
			headers.add("testMReply", "testm" + i);
		}

		SynReplyFrame synReplyFrame = new SynReplyFrame((short)3, (byte)0, 20, headers);
		return synReplyFrame;
	}
	
	abstract static class SynReplyEvent implements SpdyDecodingEvent {
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
		public void onHeaders(HeadersFrame headersFrame, Session session) {}

		@Override
		public void onWindowUpdate(WindowUpdateFrame windowUpdateFrame,Session session) {}

		@Override
		public void onData(DataFrame dataFrame, Session session) {}
	}
}

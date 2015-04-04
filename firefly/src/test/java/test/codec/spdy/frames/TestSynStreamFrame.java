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

public class TestSynStreamFrame extends TestBase{

	@Test
	public void testSynStream() throws Throwable {
		try(SpdySessionAttachment attachment = new SpdySessionAttachment()) {
			MockSession session = new MockSession();
			session.attachObject(attachment);
			
			final SynStreamFrame s = newSynStreamFrame(attachment);
			SpdyDecoder decoder = new SpdyDecoder(new SynStreamEvent(){
				@Override
				public void onSynStream(SynStreamFrame synStreamFrame, Session session) {
					System.out.println("receive syn stream frame: " + synStreamFrame);
					Assert.assertThat(synStreamFrame, is(s));
				}});

			testSpdyFrame(decoder, s, session);
		}
	}
	
	public static SynStreamFrame newSynStreamFrame(SpdySessionAttachment attachment) {
		Fields headers = new Fields(new HashMap<String, Field>(), attachment.headersBlockGenerator);
		headers.put("test1", "testValue1");
		headers.put("test2", "testValue2");
		headers.add("testM1", "testm1");
		headers.add("testM2", "testm2");

		SynStreamFrame s = new SynStreamFrame((short)3, SynStreamFrame.FLAG_FIN, 1, 0, (byte)1, (byte)0, headers);
		return s;
	}
	
	abstract static class SynStreamEvent implements SpdyDecodingEvent {
		@Override
		public void onSynReply(SynReplyFrame synReplyFrame, Session session) {}

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

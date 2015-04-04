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

public class TestHeaders extends TestBase {

	@Test
	public void testHeaders() throws Throwable {
		try(SpdySessionAttachment attachment = new SpdySessionAttachment()) {
			MockSession session = new MockSession();
			session.attachObject(attachment);
			
			final HeadersFrame s = newInstance(attachment);
			SpdyDecoder decoder = new SpdyDecoder(new HeadersEvent(){

				@Override
				public void onHeaders(HeadersFrame headersFrame, Session session) {
					System.out.println("receive headers frame: " + headersFrame);
					Assert.assertThat(headersFrame, is(s));
				}});
			testSpdyFrame(decoder, s, session);
		}
	}
	
	public static HeadersFrame newInstance(SpdySessionAttachment attachment) {
		Fields headers = new Fields(new HashMap<String, Field>(), attachment.headersBlockGenerator);
		headers.put("testHeaders1", "testHeadersValue1");
		headers.put("testHeaders2", "testHeadersValue2");		
		for (int i = 0; i < 110; i++) {
			headers.add("testHeaders", "testh" + i);
		}
		
		HeadersFrame headersFrame = new HeadersFrame((short)3, HeadersFrame.FLAG_FIN, 7727, headers);
		return headersFrame;
	}
	
	abstract static class HeadersEvent implements SpdyDecodingEvent {
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
		public void onWindowUpdate(WindowUpdateFrame windowUpdateFrame,Session session) {}

		@Override
		public void onData(DataFrame dataFrame, Session session) {}
	}
}

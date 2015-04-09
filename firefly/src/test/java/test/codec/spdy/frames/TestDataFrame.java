package test.codec.spdy.frames;

import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;

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

public class TestDataFrame extends TestBase {

	@Test
	public void testDataFrame() throws Throwable {
		try(SpdySessionAttachment attachment = new SpdySessionAttachment()) {
			MockSession session = new MockSession();
			session.attachObject(attachment);
			
			final DataFrame s = newInstance();
			SpdyDecoder decoder = new SpdyDecoder(new DataEvent(){

				@Override
				public void onData(DataFrame dataFrame, Session session) {
					System.out.println("receive data frame: " + dataFrame);
					Assert.assertThat(dataFrame, is(s));
				}});
			testSpdyFrame(decoder, s, session);
		}
	}
	
	public static DataFrame newInstance() {
		DataFrame dataFrame = new DataFrame(4242, DataFrame.FLAG_FIN);
		String data = "";
		for (int i = 0; i < 1000; i++) {
			data += "hello data";
		}
		dataFrame.setData(data.getBytes(StandardCharsets.UTF_8));
		return dataFrame;
	}
	
	abstract static class DataEvent implements SpdyDecodingEventListener {
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
		public void onWindowUpdate(WindowUpdateFrame windowUpdateFrame, Session session) {}
	}
}

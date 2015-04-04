package test.codec.spdy.frames;

import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.spdy.decode.SpdyDecoder;
import com.firefly.codec.spdy.decode.SpdyDecodingEvent;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.Serialization;
import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.spdy.frames.control.PingFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.SettingsFrame;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.frames.control.WindowUpdateFrame;
import com.firefly.net.Session;

public class TestMultiFrames extends TestBase {

	@Test
	public void testMutiFrames() throws Throwable {
		try(SpdySessionAttachment attachment = new SpdySessionAttachment()) {
			MockSession session = new MockSession();
			session.attachObject(attachment);
			
			final List<Serialization> s = Arrays.asList(
				TestSynStreamFrame.newSynStreamFrame(attachment),
				TestDataFrame.newInstance(),
				TestSettings.newInstance(),
				TestSynReplyFrame.newInstance(attachment),
				TestGoAway.newInstance(),
				TestPing.newInstance(),
				TestHeaders.newInstance(attachment),
				TestWindowUpdate.newInstance(),
				TestRstStream.newInstance()
			);
			
			SpdyDecoder decoder = new SpdyDecoder(new SpdyDecodingEvent() {
				
				@Override
				public void onWindowUpdate(WindowUpdateFrame windowUpdateFrame, Session session) {
					System.out.println("receive window update frame: " + windowUpdateFrame);
					Assert.assertThat(windowUpdateFrame, is(s.get(s.indexOf(windowUpdateFrame))));
				}
				
				@Override
				public void onSynStream(SynStreamFrame synStreamFrame, Session session) {
					System.out.println("receive syn stream frame: " + synStreamFrame);
					Assert.assertThat(synStreamFrame, is(s.get(s.indexOf(synStreamFrame))));
				}
				
				@Override
				public void onSynReply(SynReplyFrame synReplyFrame, Session session) {
					System.out.println("receive syn reply frame: " + synReplyFrame);
					Assert.assertThat(synReplyFrame, is(s.get(s.indexOf(synReplyFrame))));
				}
				
				@Override
				public void onSettings(SettingsFrame settingsFrame, Session session) {
					System.out.println("receive settings frame: " + settingsFrame);
					Assert.assertThat(settingsFrame, is(s.get(s.indexOf(settingsFrame))));
				}
				
				@Override
				public void onRstStream(RstStreamFrame rstStreamFrame, Session session) {
					System.out.println("receive rst frame: " + rstStreamFrame);
					Assert.assertThat(rstStreamFrame, is(s.get(s.indexOf(rstStreamFrame))));
				}
				
				@Override
				public void onPing(PingFrame pingFrame, Session session) {
					System.out.println("receive ping frame: " + pingFrame);
					Assert.assertThat(pingFrame, is(s.get(s.indexOf(pingFrame))));
				}
				
				@Override
				public void onHeaders(HeadersFrame headersFrame, Session session) {
					System.out.println("receive headers frame: " + headersFrame);
					Assert.assertThat(headersFrame, is(s.get(s.indexOf(headersFrame))));
				}
				
				@Override
				public void onGoAway(GoAwayFrame goAwayFrame, Session session) {
					System.out.println("receive go away frame: " + goAwayFrame);
					Assert.assertThat(goAwayFrame, is(s.get(s.indexOf(goAwayFrame))));
				}
				
				@Override
				public void onData(DataFrame dataFrame, Session session) {
					System.out.println("receive data frame: " + dataFrame);
					Assert.assertThat(dataFrame, is(s.get(s.indexOf(dataFrame))));
				}
			});
			testSpdyFrame(decoder, s, session);
		}
	}
}

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
import com.firefly.codec.spdy.frames.control.Settings;
import com.firefly.codec.spdy.frames.control.SettingsFrame;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.frames.control.WindowUpdateFrame;
import com.firefly.net.Session;

public class TestSettings extends TestBase {

	@Test
	public void testSettingsID() {
		Assert.assertThat(Settings.ID.from(1), is(Settings.ID.UPLOAD_BANDWIDTH));
		Assert.assertThat(Settings.ID.from(2), is(Settings.ID.DOWNLOAD_BANDWIDTH));
		Assert.assertThat(Settings.ID.from(8), is(Settings.ID.CLIENT_CERTIFICATE_VECTOR_SIZE));
	}
	
	@Test
	public void testSettingsFlag() {
		Assert.assertThat(Settings.Flag.from((byte)0), is(Settings.Flag.NONE));
		Assert.assertThat(Settings.Flag.from((byte)1), is(Settings.Flag.PERSIST));
		Assert.assertThat(Settings.Flag.from((byte)2), is(Settings.Flag.PERSISTED));
		
		Assert.assertThat(Settings.Flag.from(0), is(Settings.Flag.NONE));
		Assert.assertThat(Settings.Flag.from(1), is(Settings.Flag.PERSIST));
		Assert.assertThat(Settings.Flag.from(2), is(Settings.Flag.PERSISTED));
	}
	
	@Test
	public void testSettings() throws Throwable {
		try(SpdySessionAttachment attachment = new SpdySessionAttachment()) {
			MockSession session = new MockSession();
			session.attachObject(attachment);
			
			final SettingsFrame s = newInstance();
			SpdyDecoder decoder = new SpdyDecoder(new SettingsFrameEvent(){

				@Override
				public void onSettings(SettingsFrame settingsFrame,
						Session session) {
					System.out.println("receive settings frame: " + settingsFrame);
					Assert.assertThat(settingsFrame, is(s));
				}});
			testSpdyFrame(decoder, s, session);
		}
	}
	
	public static SettingsFrame newInstance() {
		Settings settings = new Settings();
		Settings.Setting setting = new Settings.Setting(
				Settings.ID.CURRENT_CONGESTION_WINDOW, 
				Settings.Flag.PERSIST, 5000);
		settings.put(setting);
		
		setting = new Settings.Setting(
				Settings.ID.DOWNLOAD_BANDWIDTH, 
				Settings.Flag.PERSISTED, 1001);
		settings.put(setting);
		
		setting = new Settings.Setting(
				Settings.ID.ROUND_TRIP_TIME, 
				Settings.Flag.NONE, 200);
		settings.put(setting);
		
		setting = new Settings.Setting(
				Settings.ID.MAX_CONCURRENT_STREAMS, 
				Settings.Flag.NONE, 500000);
		settings.put(setting);
		SettingsFrame settingsFrame = new SettingsFrame((short)3, SettingsFrame.CLEAR_PERSISTED, settings);
		return settingsFrame;
	}
	
	abstract static class SettingsFrameEvent implements SpdyDecodingEventListener {
		@Override
		public void onSynStream(SynStreamFrame synStreamFrame, Session session) {}

		@Override
		public void onRstStream(RstStreamFrame rstStreamFrame,Session session) {}

		@Override
		public void onSynReply(SynReplyFrame synReplyFrame, Session session) {}

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

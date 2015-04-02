package test.codec.spdy.frames;

import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import com.firefly.codec.spdy.frames.control.Settings;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.frames.control.WindowUpdateFrame;
import com.firefly.net.Session;

public class TestSynStreamFrame {

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

			// test sync flush
			List<ByteBuffer> list = new ArrayList<ByteBuffer>();
			for (int i = 0; i < 5; i++) {
				ByteBuffer b1 = s.toByteBuffer();
				System.out.println("b1's size is " + b1.remaining());
				list.add(b1);
			}
			for(ByteBuffer b : list) {
				decoder.decode(b, session);
			}
			Assert.assertThat(attachment.isInitialized(), is(true));
			
			// test split
			ByteBuffer b2 = s.toByteBuffer();
			System.out.println("b2's size is " + b2.remaining());
			list = split(b2, 7);
			for(ByteBuffer b : list) {
				decoder.decode(b, session);
			}
			Assert.assertThat(attachment.isInitialized(), is(true));
			
			ByteBuffer b3 = s.toByteBuffer();
			System.out.println("b3's size is " + b3.remaining());
			list = split(b3, 10);
			for(ByteBuffer b : list) {
				decoder.decode(b, session);
			}
			Assert.assertThat(attachment.isInitialized(), is(true));
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
	
	
	public static List<ByteBuffer> split(ByteBuffer buf, int size) {
		List<ByteBuffer> list = new ArrayList<>();
		while(buf.hasRemaining()) {
			byte[] bytes = new byte[Math.min(size, buf.remaining())];
			buf.get(bytes);
			list.add(ByteBuffer.wrap(bytes));
		}
		return list;
	}
	
	abstract static class SynStreamEvent implements SpdyDecodingEvent {
		@Override
		public void onSynReply(SynReplyFrame synReplyFrame, Session session) {}

		@Override
		public void onRstStream(RstStreamFrame rstStreamFrame,Session session) {}

		@Override
		public void onSettings(Settings settings, Session session) {}

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

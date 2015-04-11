package test.codec.spdy.stream;

import static org.hamcrest.Matchers.is;

import org.junit.Assert;
import org.junit.Test;

import test.codec.spdy.frames.MockSession;

import com.firefly.codec.spdy.decode.SpdyDecoder;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.control.Fields;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.stream.Connection;
import com.firefly.codec.spdy.stream.DefaultSpdyDecodingEventListener;
import com.firefly.codec.spdy.stream.SettingsManager;
import com.firefly.codec.spdy.stream.Stream;
import com.firefly.codec.spdy.stream.StreamEventListener;

public class TestStream {

	@Test
	public void testWindowUpdate() throws Throwable {
		MockSession clientSession = new MockSession();
		MockSession serverSession = new MockSession();
		
		SpdySessionAttachment clientAttachment = new SpdySessionAttachment(new Connection(clientSession, true));
		SpdySessionAttachment serverAttachment = new SpdySessionAttachment(new Connection(serverSession, false));
		
		clientSession.attachObject(clientAttachment);
		serverSession.attachObject(serverAttachment);
		
		SpdyDecoder clientDecoder = new SpdyDecoder(new DefaultSpdyDecodingEventListener(new StreamEventListener(){

			@Override
			public void onSynStream(SynStreamFrame synStreamFrame, Stream stream, Connection connection) {

			}

			@Override
			public void onSynReply(SynReplyFrame synReplyFrame, Stream stream, Connection connection) {

			}

			@Override
			public void onRstStream(RstStreamFrame rstStreamFrame, Stream stream, Connection connection) {

			}

			@Override
			public void onHeaders(HeadersFrame headersFrame, Stream stream, Connection connection) {

			}

			@Override
			public void onData(DataFrame dataFrame, Stream stream, Connection connection) {

			}}, new SettingsManager(null, "localhost", 7777)));
		
		SpdyDecoder serverDecoder = new SpdyDecoder(new DefaultSpdyDecodingEventListener(new StreamEventListener(){

			@Override
			public void onSynStream(SynStreamFrame synStreamFrame, Stream stream, Connection connection) {
				System.out.println("Server receives syn stream -> " + synStreamFrame);
				
			}

			@Override
			public void onSynReply(SynReplyFrame synReplyFrame, Stream stream, Connection connection) {

			}

			@Override
			public void onRstStream(RstStreamFrame rstStreamFrame, Stream stream, Connection connection) {

			}

			@Override
			public void onHeaders(HeadersFrame headersFrame, Stream stream, Connection connection) {

			}

			@Override
			public void onData(DataFrame dataFrame, Stream stream, Connection connection) {

			}}, null));
		
		// Client creates a stream
		Stream clientStream = clientAttachment.getConnection().createStream(new StreamEventListener(){

			@Override
			public void onSynStream(SynStreamFrame synStreamFrame, Stream stream, Connection connection) {

			}

			@Override
			public void onSynReply(SynReplyFrame synReplyFrame, Stream stream, Connection connection) {

			}

			@Override
			public void onRstStream(RstStreamFrame rstStreamFrame, Stream stream, Connection connection) {

			}

			@Override
			public void onHeaders(HeadersFrame headersFrame, Stream stream, Connection connection) {

			}

			@Override
			public void onData(DataFrame dataFrame, Stream stream, Connection connection) {

			}});
		
		Assert.assertThat(clientStream.getId(), is(1));
		Assert.assertThat(clientStream.getPriority(), is((byte)0));
		Assert.assertThat(clientAttachment.getConnection().getStream(1) == clientStream, is(true));
		
		// Client sends a SYN stream to server
		Fields headers = clientStream.createFields();
		headers.put("test1", "testValue1");
		headers.put("test2", "testValue2");
		headers.add("testM1", "testm1");
		headers.add("testM2", "testm2");
		clientStream.syn((short)3, (byte)0, 0, (byte)0, headers);
		Assert.assertThat(clientStream.getWindowSize(), is(64 * 1024));
		Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(64 * 1024));
		
		// Server receives a SYN stream
		serverDecoder.decode(clientSession.outboundData.poll(), serverSession);
		
		
		
		int currentWindowSize = 64 * 1024;
		byte[] data = "hello world".getBytes();
		clientStream.sendData(data);
		currentWindowSize -= data.length;
		Assert.assertThat(clientStream.getWindowSize(), is(currentWindowSize));
		Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(currentWindowSize));
		

		data = "data2".getBytes();
		clientStream.sendData(data);
		currentWindowSize -= data.length;
		Assert.assertThat(clientStream.getWindowSize(), is(currentWindowSize));
		Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(currentWindowSize));
	}
	
	public static void main(String[] args) {
		int currentWindowSize = 64 * 1024;
		System.out.println(currentWindowSize);
		byte[] data = "hello world".getBytes();
		currentWindowSize -= data.length;
		System.out.println(currentWindowSize);
	}
}

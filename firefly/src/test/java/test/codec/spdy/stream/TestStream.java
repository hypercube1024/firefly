package test.codec.spdy.stream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import test.codec.spdy.frames.MockSession;

import com.firefly.codec.spdy.decode.SpdyDecoder;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.Version;
import com.firefly.codec.spdy.frames.control.Fields;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.spdy.frames.control.PingFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.stream.Connection;
import com.firefly.codec.spdy.stream.DefaultSpdyDecodingEventListener;
import com.firefly.codec.spdy.stream.PingEventListener;
import com.firefly.codec.spdy.stream.SettingsManager;
import com.firefly.codec.spdy.stream.Stream;
import com.firefly.codec.spdy.stream.StreamEventListener;
import com.firefly.codec.spdy.stream.WindowControl;

public class TestStream {
	
	SpdyDecoder clientDecoder = new SpdyDecoder(new DefaultSpdyDecodingEventListener(new SettingsManager(null, "localhost", 7777)));
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
			System.out.println("Server receives data -> " + dataFrame);
			if(dataFrame.getFlags() == DataFrame.FLAG_FIN) {
				Fields headers = stream.createFields();
				headers.put("response", "ok");
				stream.reply(Version.V3, (byte)0, headers);
				stream.sendLastData("the server has received messages".getBytes());
			}
		}}));

	@Test
	public void testWindowUpdate() throws Throwable {
		MockSession clientSession = new MockSession();
		MockSession serverSession = new MockSession();
		
		try(SpdySessionAttachment clientAttachment = new SpdySessionAttachment(new Connection(clientSession, true));
		SpdySessionAttachment serverAttachment = new SpdySessionAttachment(new Connection(serverSession, false));) {
		
			clientSession.attachObject(clientAttachment);
			serverSession.attachObject(serverAttachment);
			
			// Client creates a stream
			Stream clientStream = clientAttachment.getConnection().createStream(new StreamEventListener(){
	
				@Override
				public void onSynStream(SynStreamFrame synStreamFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onSynReply(SynReplyFrame synReplyFrame, Stream stream, Connection connection) {
					System.out.println("Client receives reply frame -> " + synReplyFrame);
					Assert.assertThat(synReplyFrame.getHeaders().get("response").getValue(), is("ok"));
				}
	
				@Override
				public void onRstStream(RstStreamFrame rstStreamFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onHeaders(HeadersFrame headersFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onData(DataFrame dataFrame, Stream stream, Connection connection) {
					System.out.println("Client receives data -> " + dataFrame);
					if(dataFrame.getFlags() == DataFrame.FLAG_FIN) {
						Assert.assertThat(new String(dataFrame.getData()), is("the server has received messages"));
					}
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
			clientStream.syn(Version.V3, (byte)0, 0, (byte)0, headers);
			Assert.assertThat(clientStream.getWindowSize(), is(64 * 1024));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(64 * 1024));
			
			// Server receives a SYN stream
			serverDecoder.decode(clientSession.outboundData.poll(), serverSession);
			
			// Client sends data frames
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
			
			data = "data3".getBytes();
			clientStream.sendLastData(data);
			currentWindowSize -= data.length;
			Assert.assertThat(clientStream.getWindowSize(), is(currentWindowSize));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(currentWindowSize));
			Assert.assertThat(clientStream.isOutboundClosed(), is(true));
			Assert.assertThat(clientStream.isInboundClosed(), is(false));
			
			// Server receives data
			ByteBuffer buf = null;
			while( (buf = clientSession.outboundData.poll()) != null ) {
				serverDecoder.decode(buf, serverSession);
			}
			
			// Server sends window update and replies
			while( (buf = serverSession.outboundData.poll()) != null ) {
				clientDecoder.decode(buf, clientSession);
			}
			Assert.assertThat(clientStream.isOutboundClosed(), is(true));
			Assert.assertThat(clientStream.isInboundClosed(), is(true));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(64 * 1024));
			
			System.out.println("===================================================================");
		}
	}
	
	@Test
	public void testWindowSizeIsNotEnough() throws Throwable {
		MockSession clientSession = new MockSession();
		MockSession serverSession = new MockSession();
		
		try(SpdySessionAttachment clientAttachment = new SpdySessionAttachment(new Connection(clientSession, true));
		SpdySessionAttachment serverAttachment = new SpdySessionAttachment(new Connection(serverSession, false));) {
		
			clientSession.attachObject(clientAttachment);
			serverSession.attachObject(serverAttachment);
			
			// Client creates a stream
			Stream clientStream = clientAttachment.getConnection().createStream(new StreamEventListener(){
	
				@Override
				public void onSynStream(SynStreamFrame synStreamFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onSynReply(SynReplyFrame synReplyFrame, Stream stream, Connection connection) {
					System.out.println("Client receives reply frame -> " + synReplyFrame);
					Assert.assertThat(synReplyFrame.getHeaders().get("response").getValue(), is("ok"));
				}
	
				@Override
				public void onRstStream(RstStreamFrame rstStreamFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onHeaders(HeadersFrame headersFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onData(DataFrame dataFrame, Stream stream, Connection connection) {
					System.out.println("Client receives data -> " + dataFrame);
					if(dataFrame.getFlags() == DataFrame.FLAG_FIN) {
						Assert.assertThat(new String(dataFrame.getData()), is("the server has received messages"));
					}
				}});
			
			Assert.assertThat(clientStream.getId(), is(1));
			Assert.assertThat(clientStream.getPriority(), is((byte)0));
			Assert.assertThat(clientAttachment.getConnection().getStream(1) == clientStream, is(true));
			
			
			// Client sends a SYN stream to server
			Fields headers = clientStream.createFields();
			headers.put("testBigData", "testBigData");
			clientStream.syn(Version.V3, (byte)0, 0, (byte)0, headers);
			Assert.assertThat(clientStream.getWindowSize(), is(64 * 1024));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(64 * 1024));
			
			// Server receives a SYN stream
			serverDecoder.decode(clientSession.outboundData.poll(), serverSession);
			
			StringBuilder s = new StringBuilder(40 * 1024);
			for (int i = 0; i < 40 * 1024; i++) {
				s.append('b');
			}
			byte[] data1 = s.toString().getBytes();
			byte[] data2 = s.toString().getBytes();
			clientStream.sendData(data1);
			clientStream.sendLastData(data2);
			Assert.assertThat(clientStream.getWindowSize(), is(0));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(0));
			Assert.assertThat(clientSession.outboundData.size(), is(2));
			
			
			ByteBuffer buf = null;
			// Server receives data
			while( (buf = clientSession.outboundData.poll()) != null ) {
				serverDecoder.decode(buf, serverSession);
			}
			// Client receives window update
			while( (buf = serverSession.outboundData.poll()) != null ) {
				clientDecoder.decode(buf, clientSession);
			}
			Assert.assertThat(clientStream.isOutboundClosed(), is(true));
			Assert.assertThat(clientStream.isInboundClosed(), is(false));
			
			
			// Server receives the last data
			while( (buf = clientSession.outboundData.poll()) != null ) {
				serverDecoder.decode(buf, serverSession);
			}
			// Client receives reply frame
			while( (buf = serverSession.outboundData.poll()) != null ) {
				clientDecoder.decode(buf, clientSession);
			}
			Assert.assertThat(clientStream.isOutboundClosed(), is(true));
			Assert.assertThat(clientStream.isInboundClosed(), is(true));
			System.out.println("===================================================================");
		}
	}
	
	@Test
	public void testSetCurrentInitializedWindowSize() {
		WindowControl windowControl = new WindowControl(WindowControl.DEFAULT_INITIALIZED_WINDOW_SIZE);
		windowControl.reduceWindowSize(60 * 1024);
		windowControl.setCurrentInitializedWindowSize(16 * 1024);
		Assert.assertThat(windowControl.windowSize(), is(-44 * 1024));
		
		windowControl = new WindowControl(WindowControl.DEFAULT_INITIALIZED_WINDOW_SIZE);
		windowControl.reduceWindowSize(60 * 1024);
		windowControl.setCurrentInitializedWindowSize(128 * 1024);
		Assert.assertThat(windowControl.windowSize(), is(68 * 1024));
	}
	
	@Test
	public void testStreamPriority() throws Throwable {
		MockSession clientSession = new MockSession();
		MockSession serverSession = new MockSession();
		
		try(SpdySessionAttachment clientAttachment = new SpdySessionAttachment(new Connection(clientSession, true));
		SpdySessionAttachment serverAttachment = new SpdySessionAttachment(new Connection(serverSession, false));) {
		
			clientSession.attachObject(clientAttachment);
			serverSession.attachObject(serverAttachment);
			
			// Client creates a stream
			Stream clientStream1 = clientAttachment.getConnection().createStream((byte) 1, new StreamEventListener(){
	
				@Override
				public void onSynStream(SynStreamFrame synStreamFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onSynReply(SynReplyFrame synReplyFrame, Stream stream, Connection connection) {
					System.out.println("Client 1 receives reply frame -> " + synReplyFrame);
					Assert.assertThat(synReplyFrame.getHeaders().get("response").getValue(), is("ok"));
				}
	
				@Override
				public void onRstStream(RstStreamFrame rstStreamFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onHeaders(HeadersFrame headersFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onData(DataFrame dataFrame, Stream stream, Connection connection) {
					System.out.println("Client 1 receives data -> " + dataFrame);
					if(dataFrame.getFlags() == DataFrame.FLAG_FIN) {
						Assert.assertThat(new String(dataFrame.getData()), is("the server has received messages"));
					}
				}});
			
			Assert.assertThat(clientStream1.getId(), is(1));
			Assert.assertThat(clientStream1.getPriority(), is((byte)1));
			Assert.assertThat(clientAttachment.getConnection().getStream(1) == clientStream1, is(true));
			
			Stream clientStream2 = clientAttachment.getConnection().createStream((byte) 2, new StreamEventListener(){
				
				@Override
				public void onSynStream(SynStreamFrame synStreamFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onSynReply(SynReplyFrame synReplyFrame, Stream stream, Connection connection) {
					System.out.println("Client 2 receives reply frame -> " + synReplyFrame);
					Assert.assertThat(synReplyFrame.getHeaders().get("response").getValue(), is("ok"));
				}
	
				@Override
				public void onRstStream(RstStreamFrame rstStreamFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onHeaders(HeadersFrame headersFrame, Stream stream, Connection connection) {
	
				}
	
				@Override
				public void onData(DataFrame dataFrame, Stream stream, Connection connection) {
					System.out.println("Client 2 receives data -> " + dataFrame);
					if(dataFrame.getFlags() == DataFrame.FLAG_FIN) {
						Assert.assertThat(new String(dataFrame.getData()), is("the server has received messages"));
					}
				}});
			
			Assert.assertThat(clientStream2.getId(), is(3));
			Assert.assertThat(clientStream2.getPriority(), is((byte)2));
			Assert.assertThat(clientAttachment.getConnection().getStream(3) == clientStream2, is(true));
			
			
			// Client sends a SYN stream to server
			Fields headers = clientStream1.createFields();
			headers.put("testStream1", "testStream1");
			clientStream1.syn(Version.V3, (byte)0, 0, (byte)0, headers);
			Assert.assertThat(clientStream1.getWindowSize(), is(64 * 1024));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(64 * 1024));
			
			headers = clientStream2.createFields();
			headers.put("testStream2", "testStream2");
			clientStream2.syn(Version.V3, (byte)0, 0, (byte)0, headers);
			Assert.assertThat(clientStream2.getWindowSize(), is(64 * 1024));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(64 * 1024));
			Assert.assertThat(clientSession.outboundData.size(), is(2));
			
			// Client sends data to server
			StringBuilder s = new StringBuilder(40 * 1024);
			for (int i = 0; i < 40 * 1024; i++) {
				s.append('b');
			}
			byte[] data1 = s.toString().getBytes();
			byte[] data2 = s.toString().getBytes();
			
			s = new StringBuilder(23 * 1024);
			for (int i = 0; i < 23 * 1024; i++) {
				s.append('c');
			}
			byte[] data3 = s.toString().getBytes();
			byte[] data4 = s.toString().getBytes();
			
			clientStream2.sendData(data3);
			clientStream2.sendLastData(data4);
			clientStream1.sendData(data1);
			clientStream1.sendLastData(data2);
			
			
			Assert.assertThat(clientStream1.getWindowSize(), is(46 * 1024));
			Assert.assertThat(clientStream2.getWindowSize(), is(18 * 1024));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(0));
			Assert.assertThat(clientSession.outboundData.size(), is(5));
			
			ByteBuffer buf = null;
			// Server receives data
			while( (buf = clientSession.outboundData.poll()) != null ) {
				serverDecoder.decode(buf, serverSession);
			}
			
			// Server sends window update and replies
			clientDecoder.decode(serverSession.outboundData.poll(), clientSession); // connection window update
			Assert.assertThat(clientStream1.getWindowSize(), is(23 * 1024));
			Assert.assertThat(clientStream2.getWindowSize(), is(18 * 1024));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(0));
			
			clientDecoder.decode(serverSession.outboundData.poll(), clientSession); // stream 3 window update
			Assert.assertThat(clientStream1.getWindowSize(), is(23 * 1024));
			Assert.assertThat(clientStream2.getWindowSize(), is(41 * 1024));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(0));
			
			clientDecoder.decode(serverSession.outboundData.poll(), clientSession); // stream 3 reply
			Assert.assertThat(clientStream1.getWindowSize(), is(23 * 1024));
			Assert.assertThat(clientStream2.getWindowSize(), is(41 * 1024));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(0));
			
			clientDecoder.decode(serverSession.outboundData.poll(), clientSession); // stream 3 data
			Assert.assertThat(clientStream1.getWindowSize(), is(23 * 1024));
			Assert.assertThat(clientStream2.getWindowSize(), is(41 * 1024));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(0));
			Assert.assertThat(clientStream2.isOutboundClosed(), is(true));
			Assert.assertThat(clientStream2.isInboundClosed(), is(true));
			
			clientDecoder.decode(serverSession.outboundData.poll(), clientSession); // connection window update, client retains 16kb
			Assert.assertThat(clientStream1.getWindowSize(), is(0 * 1024));
			Assert.assertThat(clientStream2.getWindowSize(), is(41 * 1024));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(0));
			
			// Client receives reply frame
			while( (buf = serverSession.outboundData.poll()) != null ) {
				clientDecoder.decode(buf, clientSession);
			}
			Assert.assertThat(clientStream1.getWindowSize(), is(2 * 1024));
			Assert.assertThat(clientAttachment.getConnection().getWindowSize(), is(2 * 1024));
			
			// Server receives the last data
			while( (buf = clientSession.outboundData.poll()) != null ) {
				serverDecoder.decode(buf, serverSession);
			}
			// Client receives reply frame
			while( (buf = serverSession.outboundData.poll()) != null ) {
				clientDecoder.decode(buf, clientSession);
			}
			Assert.assertThat(clientStream1.isOutboundClosed(), is(true));
			Assert.assertThat(clientStream1.isInboundClosed(), is(true));
		}
	}
	
	@Test
	public void testPing() throws Throwable {
		MockSession clientSession = new MockSession();
		MockSession serverSession = new MockSession();
		
		try(SpdySessionAttachment clientAttachment = new SpdySessionAttachment(new Connection(clientSession, true));
		SpdySessionAttachment serverAttachment = new SpdySessionAttachment(new Connection(serverSession, false));) {
		
			clientSession.attachObject(clientAttachment);
			serverSession.attachObject(serverAttachment);
			
			serverAttachment.getConnection().ping(new PingEventListener(){

				@Override
				public void onPing(PingFrame pingFrame, Connection connection) {
					Assert.assertThat(pingFrame.getPingId() % 2, is(0));
					System.out.println("Server receives ping " + pingFrame);
				}});
			
			ByteBuffer buf = null;
			while( (buf = serverSession.outboundData.poll()) != null ) {
				clientDecoder.decode(buf, clientSession);
			}
			
			while( (buf = clientSession.outboundData.poll()) != null ) {
				serverDecoder.decode(buf, serverSession);
			}
			
			clientAttachment.getConnection().ping(new PingEventListener(){

				@Override
				public void onPing(PingFrame pingFrame, Connection connection) {
					Assert.assertThat(pingFrame.getPingId() % 2, is(not(0)));
					System.out.println("Client receives ping " + pingFrame);
				}});
			
			while( (buf = clientSession.outboundData.poll()) != null ) {
				serverDecoder.decode(buf, serverSession);
			}
			
			while( (buf = serverSession.outboundData.poll()) != null ) {
				clientDecoder.decode(buf, clientSession);
			}
		}
	}
	
	public void testSettings() throws Throwable {
		
	}
	
	public void testRst() throws Throwable {
		
	}
	
	public void testGoAway() throws Throwable {
	
	}
}

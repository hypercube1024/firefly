package com.firefly.codec.spdy.decode;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.control.ControlFrameHeader;
import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.stream.Connection;
import com.firefly.net.tcp.ssl.SSLSession;

public class SpdySessionAttachment implements Closeable {
	
	public SSLSession sslSession;
	public ByteBuffer byteBuffer;
	
	// stream state
	public FrameType frameType;
	public ControlFrameParserState controlFrameParserState = ControlFrameParserState.HEAD;
	public DataFrameParserState dataFrameParserState = DataFrameParserState.HEAD;
	public ControlFrameHeader controlFrameHeader;
	public DataFrame dataFrame;
	
	// spdy connection
	private Connection connection;
	
	// other attachment
	public Object attachment;
	
	public SpdySessionAttachment(Connection connection) {
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection;
	}

	public void reset() {
		byteBuffer = null;
		frameType = null;
		controlFrameParserState = ControlFrameParserState.HEAD;
		dataFrameParserState = DataFrameParserState.HEAD;
		controlFrameHeader = null;
		dataFrame = null;
	}
	
	public boolean isInitialized() {
		return byteBuffer == null 
				&& frameType == null
				&& controlFrameParserState == ControlFrameParserState.HEAD
				&& dataFrameParserState == DataFrameParserState.HEAD
				&& controlFrameHeader == null
				&& dataFrame == null;
	}

	@Override
	public void close() throws IOException {
		reset();
		if(sslSession != null) {
			sslSession.close();
		}
		connection.close();
		connection = null;
		attachment = null;
	}
}

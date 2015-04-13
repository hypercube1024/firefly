package com.firefly.codec.spdy.stream;

import com.firefly.codec.spdy.decode.SpdyDecodingEventListener;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.Version;
import com.firefly.codec.spdy.frames.control.GoAwayFrame;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.spdy.frames.control.PingFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame.StreamErrorCode;
import com.firefly.codec.spdy.frames.control.SettingsFrame;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.frames.control.WindowUpdateFrame;
import com.firefly.codec.spdy.frames.exception.StreamException;
import com.firefly.net.Session;

public class DefaultSpdyDecodingEventListener implements SpdyDecodingEventListener {

	private final StreamEventListener inboundStreamEventListener;
	private final SettingsManager settingsManager;
	
	public DefaultSpdyDecodingEventListener(StreamEventListener inboundStreamEventListener, SettingsManager settingsManager) {
		this.inboundStreamEventListener = inboundStreamEventListener;
		this.settingsManager = settingsManager;
	}

	@Override
	public void onSynStream(SynStreamFrame synStreamFrame, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment) session.getAttachment();
		Connection connection = attachment.getConnection();
		int initWindowSize = connection.getCurrentInitWindowSize();
		
		Stream stream = new Stream(connection, synStreamFrame.getStreamId(), synStreamFrame.getPriority(), true, inboundStreamEventListener, initWindowSize);
		try {
			connection.addStream(stream);
			stream.getStreamEventListener().onSynStream(synStreamFrame, stream, connection);
		} finally {
			if(synStreamFrame.getFlags() == SynStreamFrame.FLAG_FIN) {
				stream.closeInbound();
			}
		}
	}

	@Override
	public void onSynReply(SynReplyFrame synReplyFrame, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment) session.getAttachment();
		Connection connection = attachment.getConnection();
		
		Stream stream = connection.getStream(synReplyFrame.getStreamId());
		if(stream == null) {
			throw new StreamException(synReplyFrame.getStreamId(), StreamErrorCode.PROTOCOL_ERROR, "The stream " + synReplyFrame.getStreamId() + " does not exist");
		}
		try {
			stream.getStreamEventListener().onSynReply(synReplyFrame, stream, connection);
		} finally {
			if(synReplyFrame.getFlags() == SynReplyFrame.FLAG_FIN) {
				stream.closeInbound();
			}
		}
	}
	
	@Override
	public void onHeaders(HeadersFrame headersFrame, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment) session.getAttachment();
		Connection connection = attachment.getConnection();
		
		Stream stream = connection.getStream(headersFrame.getStreamId());
		if(stream == null) {
			throw new StreamException(headersFrame.getStreamId(), StreamErrorCode.PROTOCOL_ERROR, "The stream " + headersFrame.getStreamId() + " does not exist");
		}
		try {
			stream.getStreamEventListener().onHeaders(headersFrame, stream, connection);
		} finally {
			if(headersFrame.getFlags() == HeadersFrame.FLAG_FIN) {
				stream.closeInbound();
			}
		}
	}
	
	@Override
	public void onData(DataFrame dataFrame, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment) session.getAttachment();
		Connection connection = attachment.getConnection();
		
		Stream stream = connection.getStream(dataFrame.getStreamId());
		if(stream == null) {
			throw new StreamException(dataFrame.getStreamId(), StreamErrorCode.PROTOCOL_ERROR, "The stream " + dataFrame.getStreamId() + " does not exist");
		}
		try {
			stream.getStreamEventListener().onData(dataFrame, stream, connection);
			
			// window update
			session.encode(new WindowUpdateFrame(Version.V3, 0, dataFrame.getLength()));
			session.encode(new WindowUpdateFrame(Version.V3, stream.getId(), dataFrame.getLength()));
		} finally {
			if(dataFrame.getFlags() == DataFrame.FLAG_FIN) {
				stream.closeInbound();
			}
		}
	}

	@Override
	public void onRstStream(RstStreamFrame rstStreamFrame, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment) session.getAttachment();
		Connection connection = attachment.getConnection();
		
		Stream stream = connection.getStream(rstStreamFrame.getStreamId());
		if(stream == null) {
			throw new StreamException(rstStreamFrame.getStreamId(), StreamErrorCode.PROTOCOL_ERROR, "The stream " + rstStreamFrame.getStreamId() + " does not exist");
		}
		try {
			stream.getStreamEventListener().onRstStream(rstStreamFrame, stream, connection);
		} finally {
			stream.closeInbound();
			stream.closeOutbound();
		}
	}
	
	@Override
	public void onWindowUpdate(WindowUpdateFrame windowUpdateFrame, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment) session.getAttachment();
		Connection connection = attachment.getConnection();
		
		int streamId = windowUpdateFrame.getStreamId();
		if(streamId == 0) {
			connection.updateWindow(windowUpdateFrame.getWindowDelta());
		} else {
			Stream stream = connection.getStream(streamId);
			if(stream != null) {
				stream.updateWindow(windowUpdateFrame.getWindowDelta());
			}
		}
	}

	@Override
	public void onSettings(SettingsFrame settingsFrame, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment) session.getAttachment();
		Connection connection = attachment.getConnection();
		connection.setInboundSettingsFrame(settingsFrame);
		if(connection.isClientMode()) {
			settingsManager.saveSettings(settingsFrame);
		}
	}

	@Override
	public void onPing(PingFrame pingFrame, Session session) {
		SpdySessionAttachment attachment = (SpdySessionAttachment) session.getAttachment();
		Connection connection = attachment.getConnection();
		connection.responsePing(pingFrame);
	}

	@Override
	public void onGoAway(GoAwayFrame goAwayFrame, Session session) {
		// TODO Auto-generated method stub

	}

}

package com.firefly.codec.spdy.stream;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.control.Fields;
import com.firefly.codec.spdy.frames.control.Fields.Field;
import com.firefly.codec.spdy.frames.control.HeadersFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame;
import com.firefly.codec.spdy.frames.control.RstStreamFrame.StreamErrorCode;
import com.firefly.codec.spdy.frames.control.SynReplyFrame;
import com.firefly.codec.spdy.frames.control.SynStreamFrame;
import com.firefly.codec.spdy.frames.exception.StreamException;
import com.firefly.codec.utils.ByteArrayUtils;

public class Stream {
	
	private final WindowControl windowControl;
	private final Connection connection;
	private final int id;
	private final LinkedList<DataFrame> outboundBuffer = new LinkedList<>();
	private final byte priority;
	private volatile boolean isSyn;
	private volatile boolean inboundClosed = false;
	private volatile boolean outboundClosed = false;
	private final StreamEventListener streamEventListener;
	public Object attachment;
	
	public Stream(Connection connection, int id, byte priority, boolean isSyn, StreamEventListener streamEventListener, int initWindowSize) {
		this.connection = connection;
		this.id = id;
		this.priority = priority;
		this.isSyn = isSyn;
		this.streamEventListener = streamEventListener;
		this.windowControl = new WindowControl(initWindowSize);
	}

	public int getId() {
		return id;
	}
	
	public byte getPriority() {
		return priority;
	}

	public StreamEventListener getStreamEventListener() {
		return streamEventListener;
	}
	
	public int getWindowSize() {
		return windowControl.windowSize();
	}
	
	public int getAvailableWindowSize() {
		int connectionWindowSize = connection.getWindowControl().windowSize();
		int streamWindowSize = this.windowControl.windowSize();
		return Math.min(connectionWindowSize, streamWindowSize);
	}

	void setCurrentInitializedWindowSize(int currentInitializedWindowSize) {
		windowControl.setCurrentInitializedWindowSize(currentInitializedWindowSize);
	}
	
	void updateWindow(int delta) {
		windowControl.addWindowSize(delta);
		flush();
	}
	
	public Fields createFields() {
		return new Fields(new HashMap<String, Field>(), connection.getHeadersBlockGenerator());
	}
	
	public synchronized Stream syn(short version, byte flags, int associatedStreamId, byte slot, Fields fields) {
		checkState();
		
		if(isSyn)
			throw new StreamException(id, StreamErrorCode.PROTOCOL_ERROR, "The SYN stream has been sent");

		try {
			SynStreamFrame synStreamFrame = new SynStreamFrame(version, flags, id, associatedStreamId, flags, slot, fields);
			connection.getSession().encode(synStreamFrame);
			isSyn = true;
			return this;
		} finally {
			if(flags == SynStreamFrame.FLAG_FIN) {
				closeOutbound();
			}
		}
	}
	
	private void checkState() {		
		if(outboundClosed)
			throw new StreamException(id, StreamErrorCode.STREAM_ALREADY_CLOSED, "The stream " + id + " has been closed");
	}
	
	public Stream reply(short version, byte flags, Fields headers) {
		checkState();
		try {
			SynReplyFrame synReplyFrame = new SynReplyFrame(version, flags, id, headers);
			connection.getSession().encode(synReplyFrame);
			return this;
		} finally {
			if(flags == SynReplyFrame.FLAG_FIN) {
				closeOutbound();
			}
		}
	}
	
	public Stream rst(short version, StreamErrorCode statusCode) {
		try {
			RstStreamFrame rst = new RstStreamFrame(version, id, statusCode);
			connection.getSession().encode(rst);
			return this;
		} finally {
			closeInbound();
			closeOutbound();
		}
	}
	
	public Stream sendHeaders(short version, byte flags, Fields headers) {
		checkState();
		try {
			HeadersFrame headersFrame = new HeadersFrame(version, flags, id, headers);
			connection.getSession().encode(headersFrame);
			return this;
		} finally {
			if(flags == HeadersFrame.FLAG_FIN) {
				closeOutbound();
			}
		}
	}
	
	public Stream sendData(byte[] data) {
		return sendData(data, (byte)0);
	}
	
	public Stream sendLastData(byte[] data) {
		return sendData(data, DataFrame.FLAG_FIN);
	}
	
	public synchronized Stream sendData(byte[] data, byte flags) {
		checkState();		
		outboundBuffer.offer(new DataFrame(id, flags, data));
		flush();
		return this;
	}
	
	public synchronized void flush() {
		while(true) {
			DataFrame dataFrame = outboundBuffer.peek();
			if(dataFrame == null)
				break;
			int availableWindowSize = getAvailableWindowSize();
			if(availableWindowSize <= 0)
				break;

			if(dataFrame.getLength() > availableWindowSize) {
				// split into two small data blocks
				List<byte[]> list = ByteArrayUtils.splitData(dataFrame.getData(), availableWindowSize, 2);
				// flush the first data block
				_flushData(new DataFrame(dataFrame.getStreamId(), (byte)0, list.get(0)));
				// remove current data frame
				outboundBuffer.poll();
				// insert the remained data block to the head of queue
				outboundBuffer.offerFirst(new DataFrame(dataFrame.getStreamId(), dataFrame.getFlags(), list.get(1)));
			} else {
				_flushData(dataFrame);
				outboundBuffer.poll();
			}
		}
	}
	
	private void _flushData(DataFrame dataFrame) {
		try {
			connection.getSession().encode(dataFrame);
			connection.getWindowControl().reduceWindowSize(dataFrame.getLength());
			windowControl.reduceWindowSize(dataFrame.getLength());
		} finally {
			if(dataFrame.getFlags() == DataFrame.FLAG_FIN) {
				closeOutbound();
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((connection == null) ? 0 : connection.hashCode());
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Stream other = (Stream) obj;
		if (connection == null) {
			if (other.connection != null)
				return false;
		} else if (!connection.equals(other.connection))
			return false;
		if (id != other.id)
			return false;
		return true;
	}

	public boolean isInboundClosed() {
		return inboundClosed;
	}

	public boolean isOutboundClosed() {
		return outboundClosed;
	}
	
	synchronized void closeInbound() {
		inboundClosed = true;
		if(outboundClosed)
			connection.remove(this);
	}
	
	synchronized void closeOutbound() {
		outboundClosed = true;
		if(inboundClosed)
			connection.remove(this);
	}
	
}

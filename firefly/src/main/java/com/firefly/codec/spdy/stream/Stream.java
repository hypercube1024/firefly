package com.firefly.codec.spdy.stream;

import java.util.LinkedList;

import com.firefly.codec.spdy.frames.DataFrame;
import com.firefly.codec.spdy.frames.control.Fields;
import com.firefly.codec.spdy.frames.control.RstStreamFrame.StreamErrorCode;
import com.firefly.codec.spdy.frames.exception.StreamException;

public class Stream implements Comparable<Stream> {
	private final WindowControl windowControl;
	private final Connection connection;
	private final int id;
	private final LinkedList<DataFrame> outboundBuffer = new LinkedList<>();
	private final byte priority;
	private boolean isSyn;
	private final StreamEventListener streamEventListener;
	
	public Stream(Connection connection, int id, byte priority, boolean isSyn, StreamEventListener streamEventListener) {
		this.connection = connection;
		this.id = id;
		this.priority = priority;
		this.isSyn = isSyn;
		this.streamEventListener = streamEventListener;
		this.windowControl = new WindowControl();
	}
	
	public Stream(Connection connection, int id, byte priority, boolean isSyn, StreamEventListener streamEventListener, int initWindowSize) {
		this(connection, id, priority, isSyn, streamEventListener);
		if(initWindowSize > 0)
			windowControl.setWindowSize(initWindowSize);
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

	public void windowUpdate(int delta) {
		windowControl.addWindowSize(delta);
		flush();
	}
	
	public Stream syn(short version, byte flags, int associatedStreamId, byte slot, Fields headers) {
		// TODO
		return this;
	}
	
	public Stream reply(short version, byte flags, Fields headers) {
		// TODO
		return this;
	}
	
	public Stream sendData(byte[] data) {
		return sendData(data, (byte)0);
	}
	
	public Stream sendLastData(byte[] data) {
		return sendData(data, DataFrame.FLAG_FIN);
	}
	
	public synchronized Stream sendData(byte[] data, byte flags) {
		if(!isSyn)
			throw new StreamException(id, StreamErrorCode.PROTOCOL_ERROR, "The SYN stream has not been sent");
			
		outboundBuffer.offer(new DataFrame(id, flags, data));
		flush();
		return this;
	}
	
	public synchronized void flush() {
		while(true) {
			DataFrame dataFrame = outboundBuffer.peek();
			if(dataFrame == null)
				break;
			if(dataFrame.getLength() > connection.getWindowControl().windowSize())
				break;
			if(dataFrame.getLength() > windowControl.windowSize())
				break;
			
			connection.getSession().encode(dataFrame);
			windowControl.reduceWindowSize(dataFrame.getLength());
			outboundBuffer.poll();
			if(dataFrame.getFlags() == DataFrame.FLAG_FIN) {
				connection.remove(this);
			}
		}
	}

	@Override
	public int compareTo(Stream o) {
		return Byte.compare(priority, o.priority);
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
	
}

package test.codec.http2.decode;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.LinkedList;

import com.firefly.net.ByteBufferArrayOutputEntry;
import com.firefly.net.OutputEntry;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferUtils;

public class HTTP2MockSession implements Session {

	private Object attachment;
	public LinkedList<ByteBuffer> outboundData = new LinkedList<>();
	private boolean isOpen = true;

	@Override
	public void attachObject(Object attachment) {
		this.attachment = attachment;
	}

	@Override
	public Object getAttachment() {
		return attachment;
	}

	@Override
	public void onReceivingMessage(Object message) {
		// TODO Auto-generated method stub

	}

	@Override
	public void encode(Object message) {
		if (message instanceof ByteBufferArrayOutputEntry) {
			ByteBufferArrayOutputEntry outputEntry = (ByteBufferArrayOutputEntry) message;
			write(outputEntry);
		}
	}

	@Override
	public int getSessionId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getOpenTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLastReadTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLastWrittenTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getLastActiveTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getReadBytes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getWrittenBytes() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void close() {
		isOpen = false;
	}

	@Override
	public Session.State getState() {
		// TODO Auto-generated method stub
		return State.OPEN;
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(ByteBuffer byteBuffer, Callback callback) {
		outboundData.offer(byteBuffer);
		byteBuffer.flip();
		callback.succeeded();
	}

	@Override
	public void write(ByteBuffer[] buffers, Callback callback) {
		for(ByteBuffer buffer : buffers) {
			outboundData.offer(buffer);
			buffer.flip();
		}
		callback.succeeded();
		
	}

	@Override
	public void write(Collection<ByteBuffer> buffers, Callback callback) {
		write(buffers.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY), callback);
	}

	@Override
	public void write(FileRegion file, Callback callback) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void write(OutputEntry<?> entry) {
		ByteBufferArrayOutputEntry outputEntry = (ByteBufferArrayOutputEntry)entry;
		write(outputEntry.getData(), outputEntry.getCallback());
	}

	@Override
	public void closeNow() {
		isOpen = false;
	}

	@Override
	public void shutdownOutput() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdownInput() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getCloseTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getDuration() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getIdleTimeout() {
		// TODO Auto-generated method stub
		return 0;
	}

}

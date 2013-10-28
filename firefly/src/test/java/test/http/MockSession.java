package test.http;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.server.http.HttpServletRequestImpl;
import com.firefly.server.http.SessionAttachment;

public class MockSession implements Session {
	
	Object attachment = new SessionAttachment();
	List<HttpServletRequestImpl> request = new ArrayList<HttpServletRequestImpl>();

	@Override
	public void fireReceiveMessage(Object message) {
		request.add((HttpServletRequestImpl)message);
	}

	@Override
	public void encode(Object message) {
		// TODO Auto-generated method stub

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
	public void close(boolean immediately) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getState() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public InetSocketAddress getLocalAddress() {
		return new InetSocketAddress("localhost", 80);
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return new InetSocketAddress("localhost", 9999);
	}

	@Override
	public void write(ByteBuffer byteBuffer) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void write(FileRegion fileRegion) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void attachObject(Object attachment) {
		this.attachment = attachment;
	}

	@Override
	public Object getAttachment() {
		return attachment;
	}

}

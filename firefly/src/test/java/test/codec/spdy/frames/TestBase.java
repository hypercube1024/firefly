package test.codec.spdy.frames;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class TestBase {
	
	public ByteBuffer merge(List<ByteBuffer> list) {
		int size = 0;
		for(ByteBuffer b : list) {
			size += b.remaining();
		}
		ByteBuffer ret = ByteBuffer.allocate(size);
		for(ByteBuffer b : list) {
			ret.put(b);
		}
		ret.flip();
		return ret;
	}
	
	public List<ByteBuffer> split(ByteBuffer buf, int size) {
		List<ByteBuffer> list = new ArrayList<>();
		while(buf.hasRemaining()) {
			byte[] bytes = new byte[Math.min(size, buf.remaining())];
			buf.get(bytes);
			list.add(ByteBuffer.wrap(bytes));
		}
		return list;
	}
}

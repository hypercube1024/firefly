package test.codec.spdy.frames;

import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import com.firefly.codec.spdy.decode.SpdyDecoder;
import com.firefly.codec.spdy.decode.SpdySessionAttachment;
import com.firefly.codec.spdy.frames.Serialization;
import com.firefly.net.Session;

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
	
	protected void testSpdyFrame(SpdyDecoder decoder, Serialization s, Session session) throws Throwable {
		SpdySessionAttachment attachment = (SpdySessionAttachment)session.getAttachment();
		// test sync flush
		List<ByteBuffer> list = new ArrayList<ByteBuffer>();
		for (int i = 0; i < 10; i++) {
			ByteBuffer b1 = s.toByteBuffer();
			System.out.println("b1's size is " + b1.remaining());
			list.add(b1);
		}
		for(ByteBuffer b : list) {
			decoder.decode(b, session);
		}
		Assert.assertThat(attachment.isInitialized(), is(true));
		
		// test merge
		list = new ArrayList<ByteBuffer>();
		for (int i = 0; i < 10; i++) {
			ByteBuffer b1 = s.toByteBuffer();
			System.out.println("b1's size is " + b1.remaining());
			list.add(b1);
		}
		ByteBuffer mergeByteBuffer = merge(list);
		System.out.println("merge byte buffer size is " + mergeByteBuffer.remaining());
		decoder.decode(mergeByteBuffer, session);
		Assert.assertThat(attachment.isInitialized(), is(true));
		
		// test split
		ByteBuffer b2 = s.toByteBuffer();
		System.out.println("b2's size is " + b2.remaining());
		list = split(b2, 7);
		System.out.println("split b2 into a list the size is " + list.size());
		for(ByteBuffer b : list) {
			decoder.decode(b, session);
		}
		Assert.assertThat(attachment.isInitialized(), is(true));
		
		ByteBuffer b3 = s.toByteBuffer();
		System.out.println("b3's size is " + b3.remaining());
		list = split(b3, 10);
		System.out.println("split b3 into a list the size is " + list.size());
		for(ByteBuffer b : list) {
			decoder.decode(b, session);
		}
		Assert.assertThat(attachment.isInitialized(), is(true));
	}
}

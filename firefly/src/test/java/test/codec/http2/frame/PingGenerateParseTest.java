package test.codec.http2.frame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.HeaderGenerator;
import com.firefly.codec.http2.encode.PingGenerator;
import com.firefly.codec.http2.frame.PingFrame;

public class PingGenerateParseTest {

	@Test
	public void testGenerateParse() throws Exception {
		PingGenerator generator = new PingGenerator(new HeaderGenerator());

		final List<PingFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onPing(PingFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		byte[] payload = new byte[8];
		new Random().nextBytes(payload);

		// Iterate a few times to be sure generator and parser are properly
		// reset.
		for (int i = 0; i < 2; ++i) {
			ByteBuffer buffer = generator.generatePing(payload, true);

			frames.clear();

			while (buffer.hasRemaining()) {
				parser.parse(buffer);
			}

		}

		Assert.assertEquals(1, frames.size());
		PingFrame frame = frames.get(0);
		Assert.assertArrayEquals(payload, frame.getPayload());
		Assert.assertTrue(frame.isReply());
	}

	@Test
	public void testGenerateParseOneByteAtATime() throws Exception {
		PingGenerator generator = new PingGenerator(new HeaderGenerator());

		final List<PingFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onPing(PingFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		byte[] payload = new byte[8];
		new Random().nextBytes(payload);

		// Iterate a few times to be sure generator and parser are properly
		// reset.
		for (int i = 0; i < 2; ++i) {
			ByteBuffer buffer = generator.generatePing(payload, true);

			frames.clear();
			while (buffer.hasRemaining()) {
				parser.parse(ByteBuffer.wrap(new byte[] { buffer.get() }));
			}

			Assert.assertEquals(1, frames.size());
			PingFrame frame = frames.get(0);
			Assert.assertArrayEquals(payload, frame.getPayload());
			Assert.assertTrue(frame.isReply());
		}
	}

	@Test
	public void testPayloadAsLong() throws Exception {
		PingGenerator generator = new PingGenerator(new HeaderGenerator());

		final List<PingFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onPing(PingFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		PingFrame ping = new PingFrame(System.nanoTime(), true);
		List<ByteBuffer> list = generator.generate(ping);

		for (ByteBuffer buffer : list) {
			while (buffer.hasRemaining()) {
				parser.parse(buffer);
			}
		}

		Assert.assertEquals(1, frames.size());
		PingFrame pong = frames.get(0);
		Assert.assertEquals(ping.getPayloadAsLong(), pong.getPayloadAsLong());
		Assert.assertTrue(pong.isReply());
	}
}

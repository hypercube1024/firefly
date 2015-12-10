package test.codec.http2.frame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.GoAwayGenerator;
import com.firefly.codec.http2.encode.HeaderGenerator;
import com.firefly.codec.http2.frame.GoAwayFrame;

public class GoAwayGenerateParseTest {

	@Test
	public void testGenerateParse() throws Exception {
		GoAwayGenerator generator = new GoAwayGenerator(new HeaderGenerator());

		final List<GoAwayFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onGoAway(GoAwayFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		int lastStreamId = 13;
		int error = 17;

		// Iterate a few times to be sure generator and parser are properly
		// reset.
		for (int i = 0; i < 2; ++i) {
			ByteBuffer buffer = generator.generateGoAway(lastStreamId, error, null);

			frames.clear();
			while (buffer.hasRemaining()) {
				parser.parse(buffer);
			}
		}

		Assert.assertEquals(1, frames.size());
		GoAwayFrame frame = frames.get(0);
		Assert.assertEquals(lastStreamId, frame.getLastStreamId());
		Assert.assertEquals(error, frame.getError());
		Assert.assertNull(frame.getPayload());
	}

	@Test
	public void testGenerateParseOneByteAtATime() throws Exception {
		GoAwayGenerator generator = new GoAwayGenerator(new HeaderGenerator());

		final List<GoAwayFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onGoAway(GoAwayFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		int lastStreamId = 13;
		int error = 17;
		byte[] payload = new byte[16];
		new Random().nextBytes(payload);

		// Iterate a few times to be sure generator and parser are properly
		// reset.
		for (int i = 0; i < 2; ++i) {
			ByteBuffer buffer = generator.generateGoAway(lastStreamId, error, payload);

			frames.clear();
			while (buffer.hasRemaining()) {
				parser.parse(ByteBuffer.wrap(new byte[] { buffer.get() }));
			}

			Assert.assertEquals(1, frames.size());
			GoAwayFrame frame = frames.get(0);
			Assert.assertEquals(lastStreamId, frame.getLastStreamId());
			Assert.assertEquals(error, frame.getError());
			Assert.assertArrayEquals(payload, frame.getPayload());
		}
	}
}

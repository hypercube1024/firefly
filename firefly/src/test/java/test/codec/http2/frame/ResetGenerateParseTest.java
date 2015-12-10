package test.codec.http2.frame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.HeaderGenerator;
import com.firefly.codec.http2.encode.ResetGenerator;
import com.firefly.codec.http2.frame.ResetFrame;

public class ResetGenerateParseTest {

	@Test
	public void testGenerateParse() throws Exception {
		ResetGenerator generator = new ResetGenerator(new HeaderGenerator());

		final List<ResetFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onReset(ResetFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		int streamId = 13;
		int error = 17;

		// Iterate a few times to be sure generator and parser are properly
		// reset.
		for (int i = 0; i < 2; ++i) {
			ByteBuffer buffer = generator.generateReset(streamId, error);

			frames.clear();

			while (buffer.hasRemaining()) {
				parser.parse(buffer);
			}

		}

		Assert.assertEquals(1, frames.size());
		ResetFrame frame = frames.get(0);
		Assert.assertEquals(streamId, frame.getStreamId());
		Assert.assertEquals(error, frame.getError());
	}

	@Test
	public void testGenerateParseOneByteAtATime() throws Exception {
		ResetGenerator generator = new ResetGenerator(new HeaderGenerator());

		final List<ResetFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onReset(ResetFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		int streamId = 13;
		int error = 17;

		// Iterate a few times to be sure generator and parser are properly
		// reset.
		for (int i = 0; i < 2; ++i) {
			ByteBuffer buffer = generator.generateReset(streamId, error);

			frames.clear();
			while (buffer.hasRemaining()) {
				parser.parse(ByteBuffer.wrap(new byte[] { buffer.get() }));
			}

			Assert.assertEquals(1, frames.size());
			ResetFrame frame = frames.get(0);
			Assert.assertEquals(streamId, frame.getStreamId());
			Assert.assertEquals(error, frame.getError());
		}
	}
}

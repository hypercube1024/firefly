package test.codec.http2.frame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.firefly.codec.http2.frame.Frame;
import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.DataGenerator;
import com.firefly.codec.http2.encode.HeaderGenerator;
import com.firefly.codec.http2.frame.DataFrame;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.lang.Pair;

public class DataGenerateParseTest {
	private final byte[] smallContent = new byte[128];
	private final byte[] largeContent = new byte[128 * 1024];

	public DataGenerateParseTest() {
		Random random = new Random();
		random.nextBytes(smallContent);
		random.nextBytes(largeContent);
	}

	@Test
	public void testGenerateParseNoContentNoPadding() {
		testGenerateParseContent(BufferUtils.EMPTY_BUFFER);
	}

	@Test
	public void testGenerateParseSmallContentNoPadding() {
		testGenerateParseContent(ByteBuffer.wrap(smallContent));
	}

	private void testGenerateParseContent(ByteBuffer content) {
		List<DataFrame> frames = testGenerateParse(content);
		Assert.assertEquals(1, frames.size());
		DataFrame frame = frames.get(0);
		Assert.assertTrue(frame.getStreamId() != 0);
		Assert.assertTrue(frame.isEndStream());
		Assert.assertEquals(content, frame.getData());
	}

	@Test
	public void testGenerateParseLargeContent() {
		ByteBuffer content = ByteBuffer.wrap(largeContent);
		List<DataFrame> frames = testGenerateParse(content);
		Assert.assertEquals(8, frames.size());
		ByteBuffer aggregate = ByteBuffer.allocate(content.remaining());
		for (int i = 1; i <= frames.size(); ++i) {
			DataFrame frame = frames.get(i - 1);
			Assert.assertTrue(frame.getStreamId() != 0);
			Assert.assertEquals(i == frames.size(), frame.isEndStream());
			aggregate.put(frame.getData());
		}
		aggregate.flip();
		Assert.assertEquals(content, aggregate);
	}

	private List<DataFrame> testGenerateParse(ByteBuffer data) {
		DataGenerator generator = new DataGenerator(new HeaderGenerator());

		final List<DataFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onData(DataFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		// Iterate a few times to be sure generator and parser are properly
		// reset.
		for (int i = 0; i < 2; ++i) {
			ByteBuffer slice = data.slice();
			int generated = 0;
			List<ByteBuffer> list = new ArrayList<>();
			encodeDataFrame(generator, data, slice, generated, list);

			frames.clear();
			for (ByteBuffer buffer : list) {
				parser.parse(buffer);
			}
		}

		return frames;
	}

	@Test
	public void testGenerateParseOneByteAtATime() {
		DataGenerator generator = new DataGenerator(new HeaderGenerator());

		final List<DataFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onData(DataFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		// Iterate a few times to be sure generator and parser are properly
		// reset.
		for (int i = 0; i < 2; ++i) {
			ByteBuffer data = ByteBuffer.wrap(largeContent);
			ByteBuffer slice = data.slice();
			int generated = 0;
			List<ByteBuffer> list = new ArrayList<>();
			encodeDataFrame(generator, data, slice, generated, list);

			frames.clear();
			for (ByteBuffer buffer : list) {
				while (buffer.hasRemaining()) {
					parser.parse(ByteBuffer.wrap(new byte[] { buffer.get() }));
				}
			}

			Assert.assertEquals(largeContent.length, frames.size());
		}
	}

	private void encodeDataFrame(DataGenerator generator, ByteBuffer data, ByteBuffer slice, int generated, List<ByteBuffer> list) {
		while (true) {
            Pair<Integer, List<ByteBuffer>> pair = generator.generateData(13, slice, true, slice.remaining());
            generated += pair.first - Frame.HEADER_LENGTH;
            list.addAll(pair.second);
            if (generated == data.remaining())
                break;
        }
	}
}

package test.codec.http2.frame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.encode.HeaderGenerator;
import com.firefly.codec.http2.encode.HeadersGenerator;
import com.firefly.codec.http2.frame.HeadersFrame;
import com.firefly.codec.http2.frame.PriorityFrame;
import com.firefly.codec.http2.hpack.HpackEncoder;
import com.firefly.codec.http2.model.HostPortHttpField;
import com.firefly.codec.http2.model.HttpField;
import com.firefly.codec.http2.model.HttpFields;
import com.firefly.codec.http2.model.HttpScheme;
import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.codec.http2.model.MetaData;

public class HeadersGenerateParseTest {

	@Test
	public void testGenerateParse() throws Exception {
		HeadersGenerator generator = new HeadersGenerator(new HeaderGenerator(), new HpackEncoder());

		int streamId = 13;
		HttpFields fields = new HttpFields();
		fields.put("Accept", "text/html");
		fields.put("User-Agent", "Firefly");
		MetaData.Request metaData = new MetaData.Request("GET", HttpScheme.HTTP,
				new HostPortHttpField("localhost:8080"), "/path", HttpVersion.HTTP_2, fields);

		final List<HeadersFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onHeaders(HeadersFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		// Iterate a few times to be sure generator and parser are properly
		// reset.
		for (int i = 0; i < 2; ++i) {
			PriorityFrame priorityFrame = new PriorityFrame(streamId, 3 * streamId, 200, true);
			List<ByteBuffer> list = generator.generateHeaders(streamId, metaData, priorityFrame, true);

			frames.clear();
			for (ByteBuffer buffer : list) {
				while (buffer.hasRemaining()) {
					parser.parse(buffer);
				}
			}

			Assert.assertEquals(1, frames.size());
			HeadersFrame frame = frames.get(0);
			Assert.assertEquals(streamId, frame.getStreamId());
			Assert.assertTrue(frame.isEndStream());
			MetaData.Request request = (MetaData.Request) frame.getMetaData();
			Assert.assertEquals(metaData.getMethod(), request.getMethod());
			Assert.assertEquals(metaData.getURI(), request.getURI());
			for (int j = 0; j < fields.size(); ++j) {
				HttpField field = fields.getField(j);
				Assert.assertTrue(request.getFields().contains(field));
			}
			PriorityFrame priority = frame.getPriority();
			Assert.assertNotNull(priority);
			Assert.assertEquals(priorityFrame.getStreamId(), priority.getStreamId());
			Assert.assertEquals(priorityFrame.getParentStreamId(), priority.getParentStreamId());
			Assert.assertEquals(priorityFrame.getWeight(), priority.getWeight());
			Assert.assertEquals(priorityFrame.isExclusive(), priority.isExclusive());
		}
	}

	@Test
	public void testGenerateParseOneByteAtATime() throws Exception {
		HeadersGenerator generator = new HeadersGenerator(new HeaderGenerator(), new HpackEncoder());

		final List<HeadersFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onHeaders(HeadersFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		int streamId = 13;
		HttpFields fields = new HttpFields();
		fields.put("Accept", "text/html");
		fields.put("User-Agent", "Firefly");
		MetaData.Request metaData = new MetaData.Request("GET", HttpScheme.HTTP,
				new HostPortHttpField("localhost:8080"), "/path", HttpVersion.HTTP_2, fields);

		PriorityFrame priorityFrame = new PriorityFrame(streamId, 3 * streamId, 200, true);
		List<ByteBuffer> list = generator.generateHeaders(streamId, metaData, priorityFrame, true);

		for (ByteBuffer buffer : list) {
			while (buffer.hasRemaining()) {
				parser.parse(ByteBuffer.wrap(new byte[] { buffer.get() }));
			}
		}

		Assert.assertEquals(1, frames.size());
		HeadersFrame frame = frames.get(0);
		Assert.assertEquals(streamId, frame.getStreamId());
		Assert.assertTrue(frame.isEndStream());
		MetaData.Request request = (MetaData.Request) frame.getMetaData();
		Assert.assertEquals(metaData.getMethod(), request.getMethod());
		Assert.assertEquals(metaData.getURI(), request.getURI());
		for (int j = 0; j < fields.size(); ++j) {
			HttpField field = fields.getField(j);
			Assert.assertTrue(request.getFields().contains(field));
		}
		PriorityFrame priority = frame.getPriority();
		Assert.assertNotNull(priority);
		Assert.assertEquals(priorityFrame.getStreamId(), priority.getStreamId());
		Assert.assertEquals(priorityFrame.getParentStreamId(), priority.getParentStreamId());
		Assert.assertEquals(priorityFrame.getWeight(), priority.getWeight());
		Assert.assertEquals(priorityFrame.isExclusive(), priority.isExclusive());
	}
}

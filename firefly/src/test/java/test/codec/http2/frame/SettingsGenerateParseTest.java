package test.codec.http2.frame;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.codec.http2.decode.Parser;
import com.firefly.codec.http2.decode.SettingsBodyParser;
import com.firefly.codec.http2.encode.Generator;
import com.firefly.codec.http2.encode.HeaderGenerator;
import com.firefly.codec.http2.encode.SettingsGenerator;
import com.firefly.codec.http2.frame.ErrorCode;
import com.firefly.codec.http2.frame.SettingsFrame;
import com.firefly.codec.http2.stream.HTTP2Configuration;
import com.firefly.utils.codec.Base64Utils;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.lang.TypeUtils;

public class SettingsGenerateParseTest {
	
	@Test
	public void testSettingsWithBase64() {
		final HTTP2Configuration http2Configuration = new HTTP2Configuration();
		final Generator http2Generator = new Generator(http2Configuration.getMaxDynamicTableSize(), http2Configuration.getMaxHeaderBlockFragment());
		
		Map<Integer, Integer> settings = new HashMap<>();
		settings.put(SettingsFrame.HEADER_TABLE_SIZE, http2Configuration.getMaxDynamicTableSize());
		settings.put(SettingsFrame.INITIAL_WINDOW_SIZE, http2Configuration.getInitialStreamSendWindow());
		SettingsFrame settingsFrame = new SettingsFrame(settings, false);
		
		List<ByteBuffer> byteBuffers = http2Generator.control(settingsFrame);
		System.out.println("buffer size: " + byteBuffers.size());
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			for (ByteBuffer buffer : byteBuffers) {
				byte[] bufferArray = BufferUtils.toArray(buffer);
				System.out.println("before1:\t" + TypeUtils.toHexString(bufferArray));
				out.write(bufferArray);
			}
			byte[] settingsFrameBytes = out.toByteArray();
			byte[] settingsPayload = new byte[settingsFrameBytes.length - 9];
			System.arraycopy(settingsFrameBytes, 9, settingsPayload, 0, settingsPayload.length);
			
			
			String value = Base64Utils.encodeToUrlSafeString(settingsPayload);
			System.out.println("Settings: " + value);
			byte[] settingsByte = Base64Utils.decodeFromUrlSafeString(value);
			System.out.println("after:\t" + TypeUtils.toHexString(settingsByte));
			Assert.assertArrayEquals(settingsPayload, settingsByte);
			
			SettingsFrame afterSettings = SettingsBodyParser.parseBody(BufferUtils.toBuffer(settingsByte));
			System.out.println(afterSettings);
			Assert.assertEquals(settings, afterSettings.getSettings());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testGenerateParseNoSettings() throws Exception {
		List<SettingsFrame> frames = testGenerateParse(Collections.<Integer, Integer> emptyMap());
		Assert.assertEquals(1, frames.size());
		SettingsFrame frame = frames.get(0);
		Assert.assertEquals(0, frame.getSettings().size());
		Assert.assertTrue(frame.isReply());
	}

	@Test
	public void testGenerateParseSettings() throws Exception {
		Map<Integer, Integer> settings1 = new HashMap<>();
		int key1 = 13;
		Integer value1 = 17;
		settings1.put(key1, value1);
		int key2 = 19;
		Integer value2 = 23;
		settings1.put(key2, value2);
		List<SettingsFrame> frames = testGenerateParse(settings1);
		Assert.assertEquals(1, frames.size());
		SettingsFrame frame = frames.get(0);
		Map<Integer, Integer> settings2 = frame.getSettings();
		Assert.assertEquals(2, settings2.size());
		Assert.assertEquals(value1, settings2.get(key1));
		Assert.assertEquals(value2, settings2.get(key2));
	}

	private List<SettingsFrame> testGenerateParse(Map<Integer, Integer> settings) {
		SettingsGenerator generator = new SettingsGenerator(new HeaderGenerator());

		final List<SettingsFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onSettings(SettingsFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		// Iterate a few times to be sure generator and parser are properly
		// reset.
		for (int i = 0; i < 2; ++i) {
			ByteBuffer buffer = generator.generateSettings(settings, true);

			frames.clear();
			while (buffer.hasRemaining()) {
				parser.parse(buffer);
			}

		}

		return frames;
	}

	@Test
	public void testGenerateParseInvalidSettings() throws Exception {
		SettingsGenerator generator = new SettingsGenerator(new HeaderGenerator());

		final AtomicInteger errorRef = new AtomicInteger();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onConnectionFailure(int error, String reason) {
				errorRef.set(error);
			}
		}, 4096, 8192);

		Map<Integer, Integer> settings1 = new HashMap<>();
		settings1.put(13, 17);
		ByteBuffer buffer = generator.generateSettings(settings1, true);
		// Modify the length of the frame to make it invalid
		ByteBuffer bytes = buffer;
		bytes.putShort(1, (short) (bytes.getShort(1) - 1));

		while (buffer.hasRemaining()) {
			parser.parse(ByteBuffer.wrap(new byte[] { buffer.get() }));
		}

		Assert.assertEquals(ErrorCode.FRAME_SIZE_ERROR.code, errorRef.get());
	}

	@Test
	public void testGenerateParseOneByteAtATime() throws Exception {
		SettingsGenerator generator = new SettingsGenerator(new HeaderGenerator());

		final List<SettingsFrame> frames = new ArrayList<>();
		Parser parser = new Parser(new Parser.Listener.Adapter() {
			@Override
			public void onSettings(SettingsFrame frame) {
				frames.add(frame);
			}
		}, 4096, 8192);

		Map<Integer, Integer> settings1 = new HashMap<>();
		int key = 13;
		Integer value = 17;
		settings1.put(key, value);

		// Iterate a few times to be sure generator and parser are properly
		// reset.
		for (int i = 0; i < 2; ++i) {
			ByteBuffer buffer = generator.generateSettings(settings1, true);

			frames.clear();
			while (buffer.hasRemaining()) {
				parser.parse(ByteBuffer.wrap(new byte[] { buffer.get() }));
			}

			Assert.assertEquals(1, frames.size());
			SettingsFrame frame = frames.get(0);
			Map<Integer, Integer> settings2 = frame.getSettings();
			Assert.assertEquals(1, settings2.size());
			Assert.assertEquals(value, settings2.get(key));
			Assert.assertTrue(frame.isReply());
		}
	}
}

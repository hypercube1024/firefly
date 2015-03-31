package com.firefly.codec.spdy.frames.control;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.firefly.codec.spdy.frames.compression.CompressionDictionary;
import com.firefly.codec.spdy.frames.compression.CompressionFactory;

public class HeadersBlockGenerator {
	private final CompressionFactory.Compressor compressor;

	public HeadersBlockGenerator(CompressionFactory.Compressor compressor) {
		this.compressor = compressor;
		this.compressor.setDictionary(CompressionDictionary.DICTIONARY_V3);
	}

	public ByteBuffer generate(Fields headers) {
		// TODO: ByteArrayOutputStream is quite inefficient, but grows on
		// demand; optimize using ByteBuffer ?
		final Charset charset = StandardCharsets.UTF_8;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(headers.getSize() * 64);
		writeCount(buffer, headers.getSize());
		for (Fields.Field header : headers) {
			String name = header.getName().toLowerCase(Locale.ENGLISH);
			byte[] nameBytes = name.getBytes(charset);
			writeNameLength(buffer, nameBytes.length);
			buffer.write(nameBytes, 0, nameBytes.length);

			// Most common path first
			String value = header.getValue();
			byte[] valueBytes = value.getBytes(charset);
			if (header.hasMultipleValues()) {
				List<String> values = header.getValues();
				for (int i = 1; i < values.size(); ++i) {
					byte[] moreValueBytes = values.get(i).getBytes(charset);
					byte[] newValueBytes = Arrays.copyOf(valueBytes, valueBytes.length + 1 + moreValueBytes.length);
					newValueBytes[valueBytes.length] = 0;
					System.arraycopy(moreValueBytes, 0, newValueBytes, valueBytes.length + 1, moreValueBytes.length);
					valueBytes = newValueBytes;
				}
			}

			writeValueLength(buffer, valueBytes.length);
			buffer.write(valueBytes, 0, valueBytes.length);
		}

		return compressor.compressToByteBuffer(buffer.toByteArray());
	}

	private void writeCount(ByteArrayOutputStream buffer, int value) {
		buffer.write((value & 0xFF_00_00_00) >>> 24);
		buffer.write((value & 0x00_FF_00_00) >>> 16);
		buffer.write((value & 0x00_00_FF_00) >>> 8);
		buffer.write(value & 0x00_00_00_FF);
	}

	private void writeNameLength(ByteArrayOutputStream buffer, int length) {
		writeCount(buffer, length);
	}

	private void writeValueLength(ByteArrayOutputStream buffer, int length) {
		writeCount(buffer, length);
	}
}

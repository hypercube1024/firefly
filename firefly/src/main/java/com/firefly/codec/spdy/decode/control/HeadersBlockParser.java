package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;

import com.firefly.codec.spdy.decode.DecodeStatus;
import com.firefly.codec.spdy.frames.compression.CompressionDictionary;
import com.firefly.codec.spdy.frames.compression.CompressionFactory;
import com.firefly.net.Session;

public class HeadersBlockParser {
	
	private final CompressionFactory.Decompressor decompressor;
	
	public HeadersBlockParser(CompressionFactory.Decompressor decompressor) {
		this.decompressor = decompressor;
		this.decompressor.setDefaultDictionary(CompressionDictionary.DICTIONARY_V3);
	}

	public DecodeStatus parse(int streamId, int length, ByteBuffer buf, Session session) {
		return null;
	}

}

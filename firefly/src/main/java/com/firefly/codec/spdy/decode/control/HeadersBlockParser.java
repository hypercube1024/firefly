package com.firefly.codec.spdy.decode.control;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.zip.ZipException;

import com.firefly.codec.spdy.frames.compression.CompressionDictionary;
import com.firefly.codec.spdy.frames.compression.CompressionFactory;
import com.firefly.codec.spdy.frames.compression.DefaultCompressionFactory;
import com.firefly.codec.spdy.frames.control.Fields;
import com.firefly.codec.spdy.frames.control.Fields.Field;
import com.firefly.codec.spdy.frames.control.RstStreamFrame.StreamErrorCode;
import com.firefly.codec.spdy.frames.exception.StreamException;
import com.firefly.net.Session;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class HeadersBlockParser {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	public static final HeadersBlockParser DEFAULT_PARSER = new HeadersBlockParser(
			DefaultCompressionFactory.getCompressionfactory().newDecompressor());
	
	private final CompressionFactory.Decompressor decompressor;
	
	public HeadersBlockParser(CompressionFactory.Decompressor decompressor) {
		this.decompressor = decompressor;
		this.decompressor.setDefaultDictionary(CompressionDictionary.DICTIONARY_V3);
	}

	public Fields parse(int streamId, int length, ByteBuffer compressedBuffer, Session session) {
		byte[] compressed = new byte[length];
		compressedBuffer.get(compressed);
		ByteBuffer decompressedBuffer = null;
		try {
			decompressedBuffer = decompressor.decompressToByteBuffer(compressed);
		} catch (ZipException e) {
			log.error("header block decompressesing error", e);
		}
		if(decompressedBuffer == null)
			return null;
		
		Charset utf8 = StandardCharsets.UTF_8;
		int count = decompressedBuffer.getInt();
		log.debug("spdy header block count is {}", count);
		
		Fields headers = new Fields(new HashMap<String, Field>());
		for (int i = 0; i < count; i++) {
			int nameLength = decompressedBuffer.getInt();
			if(nameLength == 0) {
				throw new StreamException(streamId, StreamErrorCode.PROTOCOL_ERROR, "Invalid header name length");
			}
			byte[] nameBytes = new byte[nameLength];
			decompressedBuffer.get(nameBytes);
			String name = new String(nameBytes, utf8);
			
			int valueLength = decompressedBuffer.getInt();
			if(valueLength == 0) {
				throw new StreamException(streamId, StreamErrorCode.PROTOCOL_ERROR, "Invalid header value length");
			}
			byte[] valueBytes = new byte[valueLength];
			decompressedBuffer.get(valueBytes);
			String value = new String(valueBytes, utf8);
			String[] values = StringUtils.split(value, "\u0000");
			for(String v : values) {
				if(VerifyUtils.isNotEmpty(v))
					headers.add(name, v);
			}
		}
		return headers;
	}

}

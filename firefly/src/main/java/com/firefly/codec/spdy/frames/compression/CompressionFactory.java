package com.firefly.codec.spdy.frames.compression;

import java.nio.ByteBuffer;
import java.util.zip.ZipException;

public interface CompressionFactory {
	
	public Compressor newCompressor();
	
	public Decompressor newDecompressor();

	public interface Compressor {
		public void setInput(byte[] input);

		public void setDictionary(byte[] dictionary);

		public int compress(byte[] output);
		
		public ByteBuffer compressToByteBuffer(byte[] bytes);
	}

	public interface Decompressor {
		
		public void setDefaultDictionary(byte[] defaultDictionary);
		
		public void setDictionary(byte[] dictionary);

		public void setInput(byte[] input);

		public int decompress(byte[] output) throws ZipException;
		
		public ByteBuffer decompressToByteBuffer(byte[] compressed) throws ZipException;
	}
}

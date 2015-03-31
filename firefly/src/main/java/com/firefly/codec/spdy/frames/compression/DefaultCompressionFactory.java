package com.firefly.codec.spdy.frames.compression;

public abstract class DefaultCompressionFactory {
	
	private static final CompressionFactory compressionFactory = new StandardCompressionFactory();

	public static CompressionFactory getCompressionfactory() {
		return compressionFactory;
	}

}

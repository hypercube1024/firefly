package com.firefly.utils.concurrent;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * MemoryUtils provides an abstraction over memory properties and operations.
 */
public class MemoryUtils {
	private static final int cacheLineBytes;

	static {
		final int defaultValue = 64;
		int value = defaultValue;
		try {
			value = Integer.parseInt(AccessController.doPrivileged(new PrivilegedAction<String>() {
				@Override
				public String run() {
					return System.getProperty("com.fireflysource.utils.cacheLineBytes", String.valueOf(defaultValue));
				}
			}));
		} catch (Exception ignored) {
		}
		cacheLineBytes = value;
	}

	private MemoryUtils() {
	}

	public static int getCacheLineBytes() {
		return cacheLineBytes;
	}

	public static int getIntegersPerCacheLine() {
		return getCacheLineBytes() >> 2;
	}

	public static int getLongsPerCacheLine() {
		return getCacheLineBytes() >> 3;
	}
}

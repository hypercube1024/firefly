package com.firefly.utils.codec;

import java.util.ArrayList;
import java.util.List;

public abstract class ByteArrayUtils {
	
	public static List<byte[]> splitData(byte[] data, int size, int part) {
		if(size > data.length)
			throw new IllegalArgumentException("the part size is greater than data's length");
		
		List<byte[]> list = new ArrayList<>();
		int srcPos = 0;
		int len = size;
		byte[] dest = null;
		int blocks = part - 1;
		for(int i = 0; i < blocks; i++) {
			dest = new byte[len];
			System.arraycopy(data, srcPos, dest, 0, dest.length);
			list.add(dest);
			srcPos = srcPos + len;
			if(srcPos >= data.length)
				return list;
			len = Math.min(data.length - srcPos, size);
		}
		dest = new byte[data.length - srcPos];
		System.arraycopy(data, srcPos, dest, 0, dest.length);
		list.add(dest);
		return list;
	}
	
	public static List<byte[]> splitData(byte[] data, int size) {
		if(size > data.length)
			throw new IllegalArgumentException("the part size is greater than data's length");
		
		List<byte[]> list = new ArrayList<>();
		int srcPos = 0;
		int len = size;
		byte[] dest = null;
		while(true) {
			dest = new byte[len];
			System.arraycopy(data, srcPos, dest, 0, len);
			list.add(dest);
			srcPos = srcPos + len;
			if(srcPos >= data.length)
				break;
			len = Math.min(data.length - srcPos, size);
		}
		return list;
	}
}

package com.firefly.codec.utils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public abstract class ByteArrayUtils {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	public static List<byte[]> splitData(byte[] data, int size, int part) {
		List<byte[]> list = splitData(data, size);
		if(list == null)
			return null;
		
		List<byte[]> ret = new ArrayList<>();
		int index = 0;
		for (int i = 0; i < part - 1; i++) {
			ret.add(list.get(i));
			index++;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(data.length);
		try {
			while(index < list.size()) {
				out.write(list.get(index));
				index++;
			}
		} catch (Throwable t) {
			log.error("merge data error", t);
		}
		ret.add(out.toByteArray());
		return ret;
	}
	
	public static List<byte[]> splitData(byte[] data, int size) {
		if(size > data.length)
			return null;
		
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

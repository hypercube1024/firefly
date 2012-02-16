package com.firefly.utils.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.nio.channels.FileChannel;

abstract public class FileUtils {

	public static void recursiveDelete(File dir) {
		dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File f) {
				if (f.isDirectory())
					recursiveDelete(f);
				else
					f.delete();
				return false;
			}
		});
		dir.delete();
	}

	public static void read(File file, LineReaderHandler handler, String charset)
			throws IOException {
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(
				new FileInputStream(file), charset));
		try {
			for (String line = null; (line = reader.readLine()) != null;) {
				handler.readline(line, reader.getLineNumber());
			}
		} finally {
			if (reader != null)
				reader.close();
		}
	}

	public static long copy(File src, File dest) throws IOException {
		return copy(src, dest, 0, src.length());
	}

	public static long copy(File src, File dest, long position, long count)
			throws IOException {
		FileInputStream in = new FileInputStream(src);
		FileOutputStream out = new FileOutputStream(dest);
		FileChannel inChannel = in.getChannel();
		FileChannel outChannel = out.getChannel();
		try {
			return inChannel.transferTo(position, count, outChannel);
		} finally {
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}
}

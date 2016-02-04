package com.firefly.utils.io;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.CountingCallback;

abstract public class FileUtils {

	public static final long FILE_READER_BUFFER_SIZE = 8 * 1024;

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

	public static void read(File file, LineReaderHandler handler, String charset) throws IOException {
		try (LineNumberReader reader = new LineNumberReader(
				new InputStreamReader(new FileInputStream(file), charset))) {
			for (String line = null; (line = reader.readLine()) != null;) {
				handler.readline(line, reader.getLineNumber());
			}
		}
	}

	public static String readFileToString(File file, String charset) throws IOException {
		StringBuilder s = new StringBuilder();
		try (Reader reader = new InputStreamReader(new FileInputStream(file), charset)) {
			char[] buf = new char[1024];
			for (int length = 0; (length = reader.read(buf)) != -1;) {
				s.append(buf, 0, length);
			}
		}
		return s.length() <= 0 ? null : s.toString();
	}

	public static long copy(File src, File dest) throws IOException {
		return copy(src, dest, 0, src.length());
	}

	public static long copy(File src, File dest, long position, long count) throws IOException {
		try (FileInputStream in = new FileInputStream(src);
				FileOutputStream out = new FileOutputStream(dest);
				FileChannel inChannel = in.getChannel();
				FileChannel outChannel = out.getChannel();) {
			return inChannel.transferTo(position, count, outChannel);
		}
	}

	public static long transferTo(File file, Callback callback, BufferReaderHandler handler) throws IOException {
		try (FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ)) {
			return transferTo(fc, file.length(), callback, handler);
		}
	}

	public static long transferTo(File file, long pos, long len, Callback callback, BufferReaderHandler handler)
			throws IOException {
		try (FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ)) {
			return transferTo(fc, pos, len, callback, handler);
		}
	}

	public static long transferTo(FileChannel fileChannel, long len, Callback callback, BufferReaderHandler handler)
			throws IOException {
		long bufferSize = Math.min(FILE_READER_BUFFER_SIZE, len);
		long count = 0;
		long bufferCount = (len + bufferSize - 1) / bufferSize;
		CountingCallback countingCallback = new CountingCallback(callback, (int) bufferCount);

		try (FileChannel fc = fileChannel) {
			ByteBuffer buf = ByteBuffer.allocate((int) bufferSize);
			int i = 0;
			while ((i = fc.read(buf)) != -1) {
				if (i > 0) {
					count += i;
					buf.flip();
					handler.readBuffer(buf, countingCallback, count);
				}

				if (count >= len) {
					break;
				} else {
					buf = ByteBuffer.allocate((int) bufferSize);
				}
			}
		}
		return count;
	}

	public static long transferTo(FileChannel fileChannel, long pos, long len, Callback callback,
			BufferReaderHandler handler) throws IOException {
		long bufferSize = Math.min(FILE_READER_BUFFER_SIZE, len);
		long count = 0;
		long bufferCount = (len + bufferSize - 1) / bufferSize;
		CountingCallback countingCallback = new CountingCallback(callback, (int) bufferCount);

		try (FileChannel fc = fileChannel) {
			ByteBuffer buf = ByteBuffer.allocate((int) bufferSize);
			int i = 0;
			while ((i = fc.read(buf, pos)) != -1) {
				if (i > 0) {
					count += i;
					pos += i;
					buf.flip();
					handler.readBuffer(buf, countingCallback, count);
				}

				if (count >= len) {
					break;
				} else {
					buf = ByteBuffer.allocate((int) Math.min(FILE_READER_BUFFER_SIZE, len - count));
				}
			}
		}
		return count;
	}
}

package com.firefly.net.buffer;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class FileRegion implements Closeable {

	private final File file;
	private final long position;
	private final long length;
	private final boolean randomAccess;

	private FileInputStream inputStream;
	private RandomAccessFile raf;
	private FileChannel fileChannel;

	public FileRegion(File file) throws FileNotFoundException {
		this.file = file;
		position = 0;
		length = file.length();
		randomAccess = false;

		inputStream = new FileInputStream(file);
		fileChannel = inputStream.getChannel();
	}

	public FileRegion(File file, long position, long length) throws FileNotFoundException {
		this.file = file;
		this.position = position;
		this.length = length;
		randomAccess = true;

		raf = new RandomAccessFile(file, "r");
		fileChannel = raf.getChannel();
	}

	public long getPosition() {
		return position;
	}

	public long getLength() {
		return length;
	}

	public FileChannel getFileChannel() {
		return fileChannel;
	}

	public boolean isRandomAccess() {
		return randomAccess;
	}

	public File getFile() {
		return file;
	}

	@Override
	public void close() throws IOException {
		if (fileChannel != null)
			fileChannel.close();

		if (inputStream != null)
			inputStream.close();

		if (raf != null)
			raf.close();
	}

}

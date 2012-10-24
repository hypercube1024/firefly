package com.firefly.net.buffer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class FileRegion {
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	private final FileChannel file;
	private final RandomAccessFile raf;
	private final long position;
	private final long count;

	public FileRegion(RandomAccessFile raf, long position, long count) {
		this.raf = raf;
		this.file = raf.getChannel();
		this.position = position;
		this.count = count;
	}

	public long getPosition() {
		return position;
	}

	public long getCount() {
		return count;
	}

	public long transferTo(WritableByteChannel target, long position)
			throws IOException {
		long count = this.count - position;
		if (count < 0 || position < 0) {
			throw new IllegalArgumentException("position out of range: "
					+ position + " (expected: 0 - " + (this.count - 1) + ")");
		}
		if (count == 0) {
			return 0L;
		}

		return file.transferTo(this.position + position, count, target);
	}

	public void releaseExternalResources() {
		try {
			log.debug("FileChannel close");
			file.close();
			raf.close();
		} catch (IOException e) {
			log.error("Failed to close a file.", e);
		}
	}
}

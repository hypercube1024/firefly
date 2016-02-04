package test.utils.io;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.CountingCallback;
import com.firefly.utils.io.BufferReaderHandler;
import com.firefly.utils.io.FileUtils;

public class FileUtilsExample {

	public static void main2(String[] args) throws IOException {
		File parent = new File("/Users/qiupengtao/Documents");
		// FileUtils.read(new File(parent, "dev_note"), new LineReaderHandler()
		// {
		// @Override
		// public void readline(String text, int num) {
		// System.out.println(num + "\t" + text);
		// }
		// }, "utf-8");
		//
		// long ret = FileUtils.copy(new File(parent, "dev_note"), new
		// File(parent, "dev_note.bak"));
		// System.out.println("copy length: " + ret);

		System.out.println(FileUtils.readFileToString(new File(parent, "dev_note"), "UTF-8"));
	}

	public static void main(String[] args) throws IOException {
		final File file = new File("D:/test2.sql");

		try (FileChannel fc = FileChannel.open(Paths.get("D:/test3.sql"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

			FileUtils.transferTo(file, 3, 10, Callback.NOOP, new BufferReaderHandler() {

				@Override
				public void readBuffer(ByteBuffer buf, CountingCallback countingCallback, long count)
						throws IOException {
					System.out.println("count: " + count + "| length: " + file.length());
					fc.write(buf);
				}
			});
		}

		try (FileChannel fc = FileChannel.open(Paths.get("D:/test4.sql"), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {

			FileUtils.transferTo(file, Callback.NOOP, new BufferReaderHandler() {

				@Override
				public void readBuffer(ByteBuffer buf, CountingCallback countingCallback, long count)
						throws IOException {
					System.out.println("count: " + count + "| length: " + file.length());
					fc.write(buf);
				}
			});
		}
	}

}

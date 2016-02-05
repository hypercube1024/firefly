package test.utils.io;

import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.Assert;
import org.junit.Test;

import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.CountingCallback;
import com.firefly.utils.io.BufferReaderHandler;
import com.firefly.utils.io.FileUtils;
import com.firefly.utils.io.LineReaderHandler;

public class TestFileUtils {

	@Test
	public void testReadingLine() throws URISyntaxException, IOException {
		File file = new File(TestFileUtils.class.getClassLoader().getResource("testFile1").toURI());
		FileUtils.read(file, new LineReaderHandler() {

			@Override
			public void readline(String text, int num) {
				if (num == 1) {
					Assert.assertThat(text, is("the line 1"));
				} else if (num == 2) {
					Assert.assertThat(text, is("the line 2"));
				} else if (num == 3) {
					Assert.assertThat(text, is("hello the end line"));
				}

			}
		}, "UTF-8");
	}

	@Test
	public void testCopy() throws URISyntaxException, IOException {
		File src = new File(TestFileUtils.class.getClassLoader().getResource("testFile1").toURI());
		File dest = new File(src.getParent(), "testFile2");
		FileUtils.copy(src, dest);
		Assert.assertThat(FileUtils.readFileToString(dest, "UTF-8"), is(FileUtils.readFileToString(src, "UTF-8")));
	}

	@Test
	public void testTransferTo() throws URISyntaxException, IOException {
		File src = new File(TestFileUtils.class.getClassLoader().getResource("testFile1").toURI());
		File dest = new File(src.getParent(), "testFile3");
		try (FileChannel fc = FileChannel.open(Paths.get(dest.toURI()), StandardOpenOption.WRITE,
				StandardOpenOption.CREATE)) {
			FileUtils.transferTo(src, Callback.NOOP, new BufferReaderHandler() {

				@Override
				public void readBuffer(ByteBuffer buf, CountingCallback countingCallback, long count)
						throws IOException {
					fc.write(buf);
				}
			});
		}
		Assert.assertThat(FileUtils.readFileToString(dest, "UTF-8"), is(FileUtils.readFileToString(src, "UTF-8")));
	}

	@Test
	public void testTransferTo2() throws URISyntaxException, IOException {
		File src = new File(TestFileUtils.class.getClassLoader().getResource("testFile1").toURI());
		File dest = new File(src.getParent(), "testFile4");
		try (FileChannel fc = FileChannel.open(Paths.get(dest.toURI()), StandardOpenOption.WRITE,
				StandardOpenOption.CREATE)) {
			FileUtils.transferTo(src, 12, src.length() - 12, Callback.NOOP, new BufferReaderHandler() {

				@Override
				public void readBuffer(ByteBuffer buf, CountingCallback countingCallback, long count)
						throws IOException {
					fc.write(buf);
				}
			});
		}
		Assert.assertThat(FileUtils.readFileToString(dest, "UTF-8"), is("the line 2\r\nhello the end line"));
	}

}

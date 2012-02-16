package test.utils.io;

import java.io.File;
import java.io.IOException;

import com.firefly.utils.io.FileUtils;
import com.firefly.utils.io.LineReaderHandler;

public class FileUtilsExample {

	public static void main(String[] args) throws IOException {
		File parent = new File("/Users/qiupengtao/Documents");
		FileUtils.read(new File(parent, "dev_note"), new LineReaderHandler() {
			@Override
			public void readline(String text, int num) {
				System.out.println(num + "\t" + text);
			}
		}, "utf-8");
		
		long ret = FileUtils.copy(new File(parent, "dev_note"), new File(parent, "dev_note.bak"));
		System.out.println("copy length: " + ret);

	}

}

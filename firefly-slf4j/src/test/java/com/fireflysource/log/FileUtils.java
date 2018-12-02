package com.fireflysource.log;

import java.io.*;

abstract class FileUtils {

    static void read(File file, LineReaderHandler handler, String charset) throws IOException {
        try (LineNumberReader reader = new LineNumberReader(
                new InputStreamReader(new FileInputStream(file), charset))) {
            for (String line; (line = reader.readLine()) != null; ) {
                handler.readline(line, reader.getLineNumber());
            }
        }
    }

}

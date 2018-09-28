package com.firefly.utils.io;

import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.CountingCallback;
import com.firefly.utils.pattern.Pattern;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.Consumer;

abstract public class FileUtils {

    public static final long FILE_READER_BUFFER_SIZE = 8 * 1024;

    public static void delete(Path dir, String fileNamePattern) throws IOException {
        filter(dir, fileNamePattern, path -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void filter(Path dir, String fileNamePattern, Consumer<Path> consumer) throws IOException {
        Pattern pattern = Pattern.compile(fileNamePattern, "*");
        Files.walk(dir).filter(path -> !Files.isDirectory(path))
             .filter(path -> pattern.match(path.getFileName().toString()) != null)
             .forEach(consumer);
    }

    public static void delete(Path dir) throws IOException {
        try {
            Files.deleteIfExists(dir);
        } catch (DirectoryNotEmptyException e) {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return super.postVisitDirectory(dir, exc);
                }
            });
        }
    }

    public static void read(File file, LineReaderHandler handler, String charset) throws IOException {
        try (LineNumberReader reader = new LineNumberReader(
                new InputStreamReader(new FileInputStream(file), charset))) {
            for (String line; (line = reader.readLine()) != null; ) {
                handler.readline(line, reader.getLineNumber());
            }
        }
    }

    public static String readFileToString(File file, String charset) throws IOException {
        StringBuilder s = new StringBuilder();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), charset)) {
            char[] buf = new char[1024];
            for (int length; (length = reader.read(buf)) != -1; ) {
                s.append(buf, 0, length);
            }
        }
        return s.length() <= 0 ? null : s.toString();
    }

    public static long copy(File src, File dest) throws IOException {
        return copy(src, dest, 0, src.length());
    }

    public static long copy(File src, File dest, long position, long length) throws IOException {
        try (FileChannel in = FileChannel.open(Paths.get(src.toURI()), StandardOpenOption.READ);
             FileChannel out = FileChannel.open(Paths.get(dest.toURI()), StandardOpenOption.WRITE, StandardOpenOption.CREATE)) {
            return in.transferTo(position, length, out);
        }
    }

    public static long transferTo(File file, Callback callback, BufferReaderHandler handler) throws IOException {
        try (FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ)) {
            return transferTo(fc, file.length(), callback, handler);
        }
    }

    public static long transferTo(File file, long pos, long len,
                                  Callback callback, BufferReaderHandler handler) throws IOException {
        try (FileChannel fc = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ)) {
            return transferTo(fc, pos, len, callback, handler);
        }
    }

    public static long transferTo(FileChannel fileChannel, long len,
                                  Callback callback, BufferReaderHandler handler) throws IOException {
        long bufferSize = Math.min(FILE_READER_BUFFER_SIZE, len);
        long count = 0;
        long bufferCount = (len + bufferSize - 1) / bufferSize;
        CountingCallback countingCallback = new CountingCallback(callback, (int) bufferCount);

        try (FileChannel fc = fileChannel) {
            ByteBuffer buf = ByteBuffer.allocate((int) bufferSize);
            int i;
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

    public static long transferTo(FileChannel fileChannel, long pos, long len,
                                  Callback callback, BufferReaderHandler handler) throws IOException {
        long bufferSize = Math.min(FILE_READER_BUFFER_SIZE, len);
        long count = 0;
        long bufferCount = (len + bufferSize - 1) / bufferSize;
        CountingCallback countingCallback = new CountingCallback(callback, (int) bufferCount);

        try (FileChannel fc = fileChannel) {
            ByteBuffer buf = ByteBuffer.allocate((int) bufferSize);
            int i;
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

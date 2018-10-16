package com.firefly.net.buffer;

import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.io.BufferReaderHandler;
import com.firefly.utils.io.FileUtils;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class FileRegion implements Closeable {

    private final File file;
    private final long position;
    private final long length;
    private final boolean randomAccess;
    private FileChannel fileChannel;

    public FileRegion(File file) throws FileNotFoundException {
        this.file = file;
        position = 0;
        length = file.length();
        randomAccess = false;
    }

    public FileRegion(File file, long position, long length) throws FileNotFoundException {
        long fileLen = file.length();
        if (position < 0 || position >= fileLen) {
            throw new IndexOutOfBoundsException("the position range is illegal");
        }

        this.file = file;
        this.position = position;
        this.length = length;
        if (position > 0) {
            randomAccess = true;
        } else {
            randomAccess = (length < fileLen);
        }
    }

    public long getPosition() {
        return position;
    }

    public long getLength() {
        return length;
    }

    public FileChannel getFileChannel() throws IOException {
        if (fileChannel != null) {
            return fileChannel;
        } else {
            fileChannel = FileChannel.open(Paths.get(file.toURI()), StandardOpenOption.READ);
            return fileChannel;
        }
    }

    public boolean isRandomAccess() {
        return randomAccess;
    }

    public File getFile() {
        return file;
    }

    public long transferTo(Callback callback, BufferReaderHandler handler) throws IOException {
        long ret;
        if (isRandomAccess()) {
            ret = FileUtils.transferTo(getFileChannel(), getPosition(), getLength(), callback, handler);
        } else {
            ret = FileUtils.transferTo(getFileChannel(), getLength(), callback, handler);
        }
        return ret;
    }

    @Override
    public void close() throws IOException {
        if (fileChannel != null)
            fileChannel.close();
    }

}

package com.firefly.net.tcp;

import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;
import com.firefly.utils.function.Action2;
import com.firefly.utils.io.BufferUtils;
import com.firefly.utils.io.IO;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SecureTcpConnectionImpl extends AbstractTcpConnection {

    protected final SecureSession secureSession;

    public SecureTcpConnectionImpl(Session session, SecureSession secureSession) {
        super(session);
        this.secureSession = secureSession;
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(ByteBuffer byteBuffer) {
        Promise.Completable<Boolean> c = new Promise.Completable<>();
        write(byteBuffer, () -> c.succeeded(true), c::failed);
        return c;
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(ByteBuffer[] byteBuffer) {
        Promise.Completable<Boolean> c = new Promise.Completable<>();
        write(byteBuffer, () -> c.succeeded(true), c::failed);
        return c;
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(Collection<ByteBuffer> byteBuffer) {
        return writeToFuture(byteBuffer.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY));
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(String message) {
        return writeToFuture(message, DEFAULT_CHARSET);
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(String message, String charset) {
        ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
        return writeToFuture(byteBuffer);
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(FileRegion file) {
        Promise.Completable<Boolean> c = new Promise.Completable<>();
        write(file, () -> c.succeeded(true), c::failed);
        return c;
    }

    @Override
    public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
        _write(byteBuffer, this::_write, succeeded, failed);
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
        _write(byteBuffer, this::_write, succeeded, failed);
        return this;
    }

    private <T> void _write(T byteBuffer, Action2<T, Callback> writeAction, Action0 succeeded, Action1<Throwable> failed) {
        Callback callback = new Callback() {
            @Override
            public void succeeded() {
                if (succeeded != null) {
                    succeeded.call();
                }
            }

            @Override
            public void failed(Throwable x) {
                if (failed != null) {
                    failed.call(x);
                }
            }
        };
        writeAction.call(byteBuffer, callback);
    }

    private void _write(ByteBuffer[] byteBuffers, Callback callback) {
        try {
            secureSession.write(byteBuffers, callback);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    private void _write(ByteBuffer byteBuffer, Callback callback) {
        try {
            secureSession.write(byteBuffer, callback);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    private void _write(FileRegion file, Callback callback) {
        try {
            secureSession.transferFileRegion(file, callback);
        } catch (Exception e) {
            callback.failed(e);
        }
    }

    @Override
    public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
        return write(byteBuffer.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY), succeeded, failed);
    }

    @Override
    public TcpConnection write(String message, Action0 succeeded, Action1<Throwable> failed) {
        write(message, DEFAULT_CHARSET, succeeded, failed);
        return this;
    }

    @Override
    public TcpConnection write(String message, String charset, Action0 succeeded, Action1<Throwable> failed) {
        write(BufferUtils.toBuffer(message, Charset.forName(charset)), succeeded, failed);
        return this;
    }

    @Override
    public TcpConnection write(FileRegion file, Action0 succeeded, Action1<Throwable> failed) {
        _write(file, this::_write, succeeded, failed);
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded) {
        return write(byteBuffer, succeeded, null);
    }

    @Override
    public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded) {
        return write(byteBuffer, succeeded, null);
    }

    @Override
    public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded) {
        return write(byteBuffer, succeeded, null);
    }

    @Override
    public TcpConnection write(String message, Action0 succeeded) {
        return write(message, DEFAULT_CHARSET, succeeded, null);
    }

    @Override
    public TcpConnection write(String message, String charset, Action0 succeeded) {
        return write(message, charset, succeeded, null);
    }

    @Override
    public TcpConnection write(FileRegion file, Action0 succeeded) {
        return write(file, succeeded, null);
    }

    @Override
    public TcpConnection write(ByteBuffer byteBuffer) {
        _write(byteBuffer, Callback.NOOP);
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer[] byteBuffer) {
        _write(byteBuffer, Callback.NOOP);
        return this;
    }

    @Override
    public TcpConnection write(Collection<ByteBuffer> byteBuffer) {
        return write(byteBuffer.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY));
    }

    @Override
    public TcpConnection write(String message) {
        return write(message, DEFAULT_CHARSET, null, null);
    }

    @Override
    public TcpConnection write(String message, String charset) {
        return write(message, charset, null, null);
    }

    @Override
    public TcpConnection write(FileRegion file) {
        return write(file, null, null);
    }

    @Override
    public boolean isSecureConnection() {
        return true;
    }

    @Override
    public String getApplicationProtocol() {
        return secureSession.getApplicationProtocol();
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return secureSession.getSupportedApplicationProtocols();
    }

    @Override
    public void close() {
        IO.close(secureSession);
        session.close();
    }
}

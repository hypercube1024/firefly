package com.firefly.net.tcp;

import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;
import com.firefly.utils.io.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TcpConnectionImpl extends AbstractTcpConnection {

    public TcpConnectionImpl(Session session) {
        super(session);
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(ByteBuffer byteBuffer) {
        Promise.Completable<Boolean> c = new Promise.Completable<>();
        session.write(byteBuffer, new Callback() {
            public void succeeded() {
                c.succeeded(true);
            }

            public void failed(Throwable x) {
                c.failed(x);
            }
        });
        return c;
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(ByteBuffer[] byteBuffer) {
        Promise.Completable<Boolean> c = new Promise.Completable<>();
        session.write(byteBuffer, new Callback() {
            public void succeeded() {
                c.succeeded(true);
            }

            public void failed(Throwable x) {
                c.failed(x);
            }
        });
        return c;
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(Collection<ByteBuffer> byteBuffer) {
        Promise.Completable<Boolean> c = new Promise.Completable<>();
        session.write(byteBuffer, new Callback() {
            public void succeeded() {
                c.succeeded(true);
            }

            public void failed(Throwable x) {
                c.failed(x);
            }
        });
        return c;
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(String message) {
        return writeToFuture(message, DEFAULT_CHARSET);
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(String message, String charset) {
        return writeToFuture(BufferUtils.toBuffer(message, Charset.forName(charset)));
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(FileRegion file) {
        Promise.Completable<Boolean> c = new Promise.Completable<>();
        session.write(file, new Callback() {
            public void succeeded() {
                c.succeeded(true);
            }

            public void failed(Throwable x) {
                c.failed(x);
            }
        });
        return c;
    }

    @Override
    public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
        session.write(byteBuffer, new Callback() {
            public void succeeded() {
                succeeded.call();
            }

            public void failed(Throwable x) {
                failed.call(x);
            }
        });
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
        session.write(byteBuffer, new Callback() {
            public void succeeded() {
                succeeded.call();
            }

            public void failed(Throwable x) {
                failed.call(x);
            }
        });
        return this;
    }

    @Override
    public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
        session.write(byteBuffer, new Callback() {
            public void succeeded() {
                succeeded.call();
            }

            public void failed(Throwable x) {
                failed.call(x);
            }
        });
        return this;
    }

    @Override
    public TcpConnection write(String message, Action0 succeeded, Action1<Throwable> failed) {
        return write(message, DEFAULT_CHARSET, succeeded, failed);
    }

    @Override
    public TcpConnection write(String message, String charset, Action0 succeeded, Action1<Throwable> failed) {
        ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
        session.write(byteBuffer, new Callback() {
            public void succeeded() {
                succeeded.call();
            }

            public void failed(Throwable x) {
                failed.call(x);
            }
        });
        return this;
    }

    @Override
    public TcpConnection write(FileRegion file, Action0 succeeded, Action1<Throwable> failed) {
        session.write(file, new Callback() {
            public void succeeded() {
                succeeded.call();
            }

            public void failed(Throwable x) {
                failed.call(x);
            }
        });
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded) {
        session.write(byteBuffer, new Callback() {
            public void succeeded() {
                succeeded.call();
            }
        });
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded) {
        session.write(byteBuffer, new Callback() {
            public void succeeded() {
                succeeded.call();
            }
        });
        return this;
    }

    @Override
    public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded) {
        session.write(byteBuffer, new Callback() {
            public void succeeded() {
                succeeded.call();
            }
        });
        return this;
    }

    @Override
    public TcpConnection write(String message, Action0 succeeded) {
        return write(message, DEFAULT_CHARSET, succeeded);
    }

    @Override
    public TcpConnection write(String message, String charset, Action0 succeeded) {
        ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
        session.write(byteBuffer, new Callback() {
            public void succeeded() {
                succeeded.call();
            }
        });
        return this;
    }

    @Override
    public TcpConnection write(FileRegion file, Action0 succeeded) {
        session.write(file, new Callback() {
            public void succeeded() {
                succeeded.call();
            }
        });
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer byteBuffer) {
        session.write(byteBuffer, Callback.NOOP);
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer[] byteBuffer) {
        session.write(byteBuffer, Callback.NOOP);
        return this;
    }

    @Override
    public TcpConnection write(Collection<ByteBuffer> byteBuffer) {
        session.write(byteBuffer, Callback.NOOP);
        return this;
    }

    @Override
    public TcpConnection write(String message) {
        return write(message, DEFAULT_CHARSET);
    }

    @Override
    public TcpConnection write(String message, String charset) {
        ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
        session.write(byteBuffer, Callback.NOOP);
        return this;
    }

    @Override
    public TcpConnection write(FileRegion file) {
        session.write(file, Callback.NOOP);
        return this;
    }

    @Override
    public boolean isSecureConnection() {
        return false;
    }

    @Override
    public String getApplicationProtocol() {
        return null;
    }

    @Override
    public List<String> getSupportedApplicationProtocols() {
        return null;
    }
}

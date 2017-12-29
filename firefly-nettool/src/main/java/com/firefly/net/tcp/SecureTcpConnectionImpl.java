package com.firefly.net.tcp;

import com.firefly.net.SecureSession;
import com.firefly.net.Session;
import com.firefly.net.buffer.FileRegion;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.concurrent.Promise;
import com.firefly.utils.function.Action0;
import com.firefly.utils.function.Action1;
import com.firefly.utils.io.BufferUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SecureTcpConnectionImpl extends AbstractTcpConnection {

    SecureSession secureSession;

    public SecureTcpConnectionImpl(Session session, SecureSession secureSession) {
        super(session);
        this.secureSession = secureSession;
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(ByteBuffer byteBuffer) {
        Promise.Completable<Boolean> c = new Promise.Completable<>();
        try {
            secureSession.write(byteBuffer, new Callback() {
                public void succeeded() {
                    c.succeeded(true);
                }

                public void failed(Throwable x) {
                    c.failed(x);
                }
            });
        } catch (IOException e) {
            c.failed(e);
        }
        return c;
    }

    @Override
    public CompletableFuture<Boolean> writeToFuture(ByteBuffer[] byteBuffer) {
        Promise.Completable<Boolean> c = new Promise.Completable<>();
        try {
            secureSession.write(byteBuffer, new Callback() {
                public void succeeded() {
                    c.succeeded(true);
                }

                public void failed(Throwable x) {
                    c.failed(x);
                }
            });
        } catch (IOException e) {
            c.failed(e);
        }
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
        try {
            secureSession.transferFileRegion(file, new Callback() {
                public void succeeded() {
                    c.succeeded(true);
                }

                public void failed(Throwable x) {
                    c.failed(x);
                }
            });
        } catch (Throwable e) {
            c.failed(e);
        }
        return c;
    }

    @Override
    public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
        try {
            secureSession.write(byteBuffer, new Callback() {
                public void succeeded() {
                    succeeded.call();
                }

                public void failed(Throwable x) {
                    failed.call(x);
                }
            });
        } catch (Throwable e) {
            failed.call(e);
        }
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
        try {
            secureSession.write(byteBuffer, new Callback() {
                public void succeeded() {
                    succeeded.call();
                }

                public void failed(Throwable x) {
                    failed.call(x);
                }
            });
        } catch (Throwable e) {
            failed.call(e);
        }
        return this;
    }

    @Override
    public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded, Action1<Throwable> failed) {
        try {
            secureSession.write(byteBuffer.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY), new Callback() {
                public void succeeded() {
                    succeeded.call();
                }

                public void failed(Throwable x) {
                    failed.call(x);
                }
            });
        } catch (Throwable e) {
            failed.call(e);
        }
        return this;
    }

    @Override
    public TcpConnection write(String message, Action0 succeeded, Action1<Throwable> failed) {
        write(message, DEFAULT_CHARSET, succeeded, failed);
        return this;
    }

    @Override
    public TcpConnection write(String message, String charset, Action0 succeeded, Action1<Throwable> failed) {
        ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
        write(byteBuffer, succeeded, failed);
        return this;
    }

    @Override
    public TcpConnection write(FileRegion file, Action0 succeeded, Action1<Throwable> failed) {
        try {
            secureSession.transferFileRegion(file, new Callback() {
                public void succeeded() {
                    succeeded.call();
                }

                public void failed(Throwable x) {
                    failed.call(x);
                }
            });
        } catch (Throwable e) {
            failed.call(e);
        }
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer byteBuffer, Action0 succeeded) {
        try {
            secureSession.write(byteBuffer, new Callback() {
                public void succeeded() {
                    succeeded.call();
                }
            });
        } catch (Throwable ignored) {
        }
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer[] byteBuffer, Action0 succeeded) {
        try {
            secureSession.write(byteBuffer, new Callback() {
                public void succeeded() {
                    succeeded.call();
                }
            });
        } catch (Throwable ignored) {
        }
        return this;
    }

    @Override
    public TcpConnection write(Collection<ByteBuffer> byteBuffer, Action0 succeeded) {
        try {
            secureSession.write(byteBuffer.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY), new Callback() {
                public void succeeded() {
                    succeeded.call();
                }
            });
        } catch (Throwable ignored) {
        }
        return this;
    }

    @Override
    public TcpConnection write(String message, Action0 succeeded) {
        return write(message, DEFAULT_CHARSET, succeeded);
    }

    @Override
    public TcpConnection write(String message, String charset, Action0 succeeded) {
        ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
        try {
            secureSession.write(byteBuffer, new Callback() {
                public void succeeded() {
                    succeeded.call();
                }
            });
        } catch (Throwable ignored) {
        }
        return this;
    }

    @Override
    public TcpConnection write(FileRegion file, Action0 succeeded) {
        try {
            secureSession.transferFileRegion(file, new Callback() {
                public void succeeded() {
                    succeeded.call();
                }
            });
        } catch (Throwable ignored) {
        }
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer byteBuffer) {
        try {
            secureSession.write(byteBuffer, Callback.NOOP);
        } catch (Throwable ignored) {
        }
        return this;
    }

    @Override
    public TcpConnection write(ByteBuffer[] byteBuffer) {
        try {
            secureSession.write(byteBuffer, Callback.NOOP);
        } catch (Throwable ignored) {
        }
        return this;
    }

    @Override
    public TcpConnection write(Collection<ByteBuffer> byteBuffer) {
        return write(byteBuffer.toArray(BufferUtils.EMPTY_BYTE_BUFFER_ARRAY));
    }

    @Override
    public TcpConnection write(String message) {
        return write(message, DEFAULT_CHARSET);
    }

    @Override
    public TcpConnection write(String message, String charset) {
        ByteBuffer byteBuffer = BufferUtils.toBuffer(message, Charset.forName(charset));
        try {
            secureSession.write(byteBuffer, Callback.NOOP);
        } catch (Throwable ignored) {
        }
        return this;
    }

    @Override
    public TcpConnection write(FileRegion file) {
        try {
            secureSession.transferFileRegion(file, Callback.NOOP);
        } catch (Throwable ignored) {
        }
        return this;
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
}

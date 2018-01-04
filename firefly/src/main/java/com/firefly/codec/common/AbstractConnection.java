package com.firefly.codec.common;

import com.firefly.net.*;
import com.firefly.net.exception.SecureNetException;
import com.firefly.utils.concurrent.Callback;
import com.firefly.utils.function.Action2;
import com.firefly.utils.io.IO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * @author Pengtao Qiu
 */
abstract public class AbstractConnection implements Connection, ConnectionExtInfo {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    protected final SecureSession secureSession;
    protected final Session tcpSession;
    protected volatile Object attachment;

    public AbstractConnection(SecureSession secureSession, Session tcpSession) {
        this.secureSession = secureSession;
        this.tcpSession = tcpSession;
    }

    @Override
    public int getSessionId() {
        return tcpSession.getSessionId();
    }

    @Override
    public long getOpenTime() {
        return tcpSession.getOpenTime();
    }

    @Override
    public long getCloseTime() {
        return tcpSession.getCloseTime();
    }

    @Override
    public long getDuration() {
        return tcpSession.getDuration();
    }

    @Override
    public long getLastReadTime() {
        return tcpSession.getLastReadTime();
    }

    @Override
    public long getLastWrittenTime() {
        return tcpSession.getLastWrittenTime();
    }

    @Override
    public long getLastActiveTime() {
        return tcpSession.getLastActiveTime();
    }

    @Override
    public long getReadBytes() {
        return tcpSession.getReadBytes();
    }

    @Override
    public long getWrittenBytes() {
        return tcpSession.getWrittenBytes();
    }

    @Override
    public boolean isOpen() {
        return tcpSession.isOpen();
    }

    @Override
    public boolean isClosed() {
        return tcpSession.isClosed();
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return tcpSession.getLocalAddress();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return tcpSession.getRemoteAddress();
    }

    @Override
    public long getIdleTimeout() {
        return tcpSession.getIdleTimeout();
    }

    @Override
    public long getMaxIdleTimeout() {
        return tcpSession.getMaxIdleTimeout();
    }

    @Override
    public Object getAttachment() {
        return attachment;
    }

    @Override
    public void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    @Override
    public void close() {
        Optional.ofNullable(secureSession)
                .filter(SecureSession::isOpen)
                .ifPresent(IO::close);

        Optional.ofNullable(tcpSession)
                .filter(Session::isOpen)
                .ifPresent(Session::close);

        attachment = null;
    }

    public SecureSession getSecureSession() {
        return secureSession;
    }

    public Session getTcpSession() {
        return tcpSession;
    }

    @Override
    public boolean isEncrypted() {
        return secureSession != null;
    }

    public ByteBuffer decrypt(ByteBuffer buffer) {
        if (isEncrypted()) {
            try {
                return secureSession.read(buffer);
            } catch (IOException e) {
                throw new SecureNetException("decrypt exception", e);
            }
        } else {
            return null;
        }
    }

    public void encrypt(ByteBufferOutputEntry entry) {
        encrypt(entry, (buffers, callback) -> {
            try {
                secureSession.write(buffers, callback);
            } catch (IOException e) {
                throw new SecureNetException("encrypt exception", e);
            }
        });
    }

    public void encrypt(ByteBufferArrayOutputEntry entry) {
        encrypt(entry, (buffers, callback) -> {
            try {
                secureSession.write(buffers, callback);
            } catch (IOException e) {
                throw new SecureNetException("encrypt exception", e);
            }
        });
    }

    public void encrypt(ByteBuffer buffer) {
        try {
            secureSession.write(buffer, Callback.NOOP);
        } catch (IOException e) {
            throw new SecureNetException("encrypt exception", e);
        }
    }

    public void encrypt(ByteBuffer[] buffers) {
        try {
            secureSession.write(buffers, Callback.NOOP);
        } catch (IOException e) {
            throw new SecureNetException("encrypt exception", e);
        }
    }

    private <T> void encrypt(OutputEntry<T> entry, Action2<T, Callback> encrypt) {
        if (isEncrypted()) {
            encrypt.call(entry.getData(), entry.getCallback());
        }
    }

}

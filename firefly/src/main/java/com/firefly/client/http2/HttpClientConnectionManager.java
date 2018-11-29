package com.firefly.client.http2;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.utils.StringUtils;
import com.firefly.utils.io.IO;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.lang.pool.Pool;
import com.firefly.utils.lang.pool.PooledObject;
import com.firefly.utils.lang.pool.UnboundPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class HttpClientConnectionManager extends AbstractLifeCycle {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    private final HTTP2Client client;
    private final String host;
    private final int port;
    private final int maxSize;
    private final long timeout; // unit second
    private final long leakDetectorInterval; // unit second
    private final int maxGettingThreadNum;
    private final int maxReleaseThreadNum;
    private UnboundPool<HTTPClientConnection> pool;
    private HttpVersion httpVersion;

    public HttpClientConnectionManager(HTTP2Client client,
                                       String host, int port,
                                       int maxSize, long timeout,
                                       long leakDetectorInterval,
                                       int maxGettingThreadNum, int maxReleaseThreadNum) {
        this.client = client;
        this.host = host;
        this.port = port;
        this.maxSize = maxSize;
        this.timeout = timeout;
        this.leakDetectorInterval = leakDetectorInterval;
        this.maxGettingThreadNum = maxGettingThreadNum;
        this.maxReleaseThreadNum = maxReleaseThreadNum;
        start();
    }

    public HttpVersion getHttpVersion() {
        return httpVersion;
    }

    public CompletableFuture<PooledObject<HTTPClientConnection>> asyncGet() {
        return pool.asyncGet();
    }

    public void release(PooledObject<HTTPClientConnection> pooledObject) {
        pool.release(pooledObject);
    }

    public int size() {
        return pool.size();
    }

    @Override
    protected void init() {
        Pool.Validator<HTTPClientConnection> validator = conn -> conn.getObject().isOpen();
        Pool.Dispose<HTTPClientConnection> dispose = conn -> IO.close(conn.getObject());
        Pool.ObjectFactory<HTTPClientConnection> factory = pool -> {
            CompletableFuture<PooledObject<HTTPClientConnection>> future = new CompletableFuture<>();
            client.connect(host, port).thenAccept(conn -> {
                String leakMessage = StringUtils.replace(
                        "The Firefly HTTP client connection leaked. id -> {}, host -> {}:{}",
                        conn.getSessionId(), host, port);
                PooledObject<HTTPClientConnection> pooledObject = new PooledObject<>(conn, pool, pooledObj -> { // connection leak callback
                    log.warn(leakMessage);
                    IO.close(pooledObj.getObject());
                });
                httpVersion = conn.getHttpVersion();
                conn.onClose(c -> pooledObject.release())
                    .onException((c, exception) -> pooledObject.release());
            }).exceptionally(ex -> {
                future.completeExceptionally(ex);
                return null;
            });
            return future;
        };
        pool = new UnboundPool<>(maxSize, timeout, leakDetectorInterval,
                maxGettingThreadNum, maxReleaseThreadNum,
                factory, validator, dispose,
                p -> log.info("The Firefly HTTP client has not any connections leaked. host -> {}:{}", host, port));
    }

    @Override
    protected void destroy() {
        pool.stop();
    }
}

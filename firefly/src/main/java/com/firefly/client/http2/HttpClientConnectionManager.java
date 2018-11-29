package com.firefly.client.http2;

import com.firefly.codec.http2.model.HttpVersion;
import com.firefly.utils.Assert;
import com.firefly.utils.StringUtils;
import com.firefly.utils.io.IO;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.lang.pool.Pool;
import com.firefly.utils.lang.pool.PooledObject;
import com.firefly.utils.lang.pool.BoundObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

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
    private volatile CompletableFuture<PooledObject<HTTPClientConnection>> currentConnReq;
    private volatile HTTPClientConnection connection;
    private BoundObjectPool<HTTPClientConnection> pool;
    private final ExecutorService gettingService;

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
        this.currentConnReq = client.connect(host, port).thenApply(FakePooledObject::new);
        this.gettingService = Executors.newSingleThreadExecutor(r -> new Thread(r, "firefly-http-connection-manager-thread"));
    }

    private class FakePooledObject extends PooledObject<HTTPClientConnection> {

        FakePooledObject(HTTPClientConnection connection) {
            super(connection, null, null);
        }

        public void release() {
            if (object.getHttpVersion() != HttpVersion.HTTP_2) {
                IO.close(object);
            }
        }

        public void clear() {
        }

        public void register() {
        }
    }

    public CompletableFuture<PooledObject<HTTPClientConnection>> asyncGet() {
        if (currentConnReq.isDone()) {
            return _asyncGet();
        } else {
            CompletableFuture<PooledObject<HTTPClientConnection>> future = new CompletableFuture<>();
            gettingService.submit(() -> {
                try {
                    future.complete(_asyncGet().get());
                } catch (InterruptedException | ExecutionException e) {
                    future.completeExceptionally(e);
                }
            });
            return future;
        }
    }

    public CompletableFuture<PooledObject<HTTPClientConnection>> _asyncGet() {
        if (connection == null) {
            try {
                connection = currentConnReq.get(timeout, TimeUnit.SECONDS).getObject();
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                IO.close(connection);
            }
        }
        Assert.state(connection != null, "Get the HTTP connection exception");

        if (connection.getHttpVersion() == HttpVersion.HTTP_2) {
            return asyncGetHTTP2Conn();
        } else {
            return asyncGetHTTP1Conn();
        }
    }

    private CompletableFuture<PooledObject<HTTPClientConnection>> asyncGetHTTP1Conn() {
        start();
        return pool.asyncGet();
    }

    private CompletableFuture<PooledObject<HTTPClientConnection>> asyncGetHTTP2Conn() {
        if (connection.isOpen()) {
            CompletableFuture<PooledObject<HTTPClientConnection>> ret = new CompletableFuture<>();
            ret.complete(new FakePooledObject(connection));
            return ret;
        } else {
            currentConnReq = client.connect(host, port).thenApply(FakePooledObject::new);
            return currentConnReq;
        }
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
                conn.onClose(c -> pooledObject.release())
                    .onException((c, exception) -> pooledObject.release());
                future.complete(pooledObject);
            }).exceptionally(ex -> {
                future.completeExceptionally(ex);
                return null;
            });
            return future;
        };

        pool = new BoundObjectPool<>(maxSize, timeout, leakDetectorInterval,
                maxGettingThreadNum, maxReleaseThreadNum,
                factory, validator, dispose,
                () -> log.info("The Firefly HTTP client has not any connections leaked. host -> {}:{}", host, port));
    }

    @Override
    protected void destroy() {
        if (pool != null) {
            pool.stop();
        }
        gettingService.shutdown();
    }
}

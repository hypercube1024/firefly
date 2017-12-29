package com.firefly.net.tcp.flex.client;

import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.flex.exception.ConnectionException;
import com.firefly.utils.Assert;
import com.firefly.utils.CollectionUtils;
import com.firefly.utils.concurrent.Scheduler;
import com.firefly.utils.concurrent.Schedulers;
import com.firefly.utils.lang.AbstractLifeCycle;
import com.firefly.utils.lang.HostPort;
import com.firefly.utils.retry.RetryTaskBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.firefly.utils.retry.RetryStrategies.ifException;
import static com.firefly.utils.retry.RetryStrategies.ifResult;
import static com.firefly.utils.retry.StopStrategies.afterExecute;
import static com.firefly.utils.retry.WaitStrategies.exponentialWait;

/**
 * @author Pengtao Qiu
 */
public class FlexConnectionManager extends AbstractLifeCycle {

    protected static final Logger log = LoggerFactory.getLogger("firefly-system");

    private ConcurrentMap<HostPort, FlexConnection> concurrentMap = new ConcurrentHashMap<>();
    private final MultiplexingClient client;
    private final AtomicInteger index = new AtomicInteger(0);
    private final Scheduler scheduler = Schedulers.createScheduler();
    private final AddressProvider addressProvider;
    private volatile List<HostPort> activatedList;

    public FlexConnectionManager(MultiplexingClient client, AddressProvider addressProvider) {
        Assert.notNull(addressProvider);
        Assert.notNull(client);

        this.client = client;
        this.addressProvider = addressProvider;
        this.activatedList = convert(addressProvider.getAddressList());
        Assert.notEmpty(activatedList, "The address list is empty");
        start();
    }

    public List<HostPort> getActivatedList() {
        return activatedList;
    }

    public void updateActivatedList(Set<String> activatedList) {
        this.activatedList = convert(activatedList);
    }

    public FlexConnection getConnection() {
        FlexConnection ret = RetryTaskBuilder.<FlexConnection>newTask()
                .retry(ifResult(Objects::isNull))
                .stop(afterExecute(activatedList.size()))
                .wait(exponentialWait(10, TimeUnit.MILLISECONDS))
                .task(() -> {
                    int i = Math.abs(index.getAndAdd(1)) % activatedList.size();
                    return getConnection(activatedList.get(i));
                })
                .call();
        if (ret == null) {
            throw new ConnectionException("Can not get connection");
        }
        return ret;
    }

    private synchronized FlexConnection getConnection(HostPort hostPort) {
        try {
            FlexConnection connection = concurrentMap.get(hostPort);
            if (connection != null) {
                if (connection.isOpen()) {
                    return connection;
                } else {
                    FlexConnection newConnection = createConnection(hostPort);
                    if (newConnection != null) {
                        concurrentMap.put(hostPort, newConnection);
                        return newConnection;
                    } else {
                        concurrentMap.remove(hostPort);
                        updateActivatedList();
                        return null;
                    }
                }
            } else {
                FlexConnection newConnection = createConnection(hostPort);
                if (newConnection != null) {
                    concurrentMap.put(hostPort, newConnection);
                    updateActivatedList();
                    return newConnection;
                } else {
                    concurrentMap.remove(hostPort);
                    updateActivatedList();
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("get connection exception", e);
            updateActivatedList();
            return null;
        }
    }

    private FlexConnection createConnection(HostPort hostPort) {
        return RetryTaskBuilder.<FlexConnection>newTask()
                .retry(ifException(ex -> ex != null && ex.getCause() instanceof TimeoutException))
                .stop(afterExecute(10))
                .wait(exponentialWait(10, TimeUnit.MILLISECONDS))
                .task(() -> this.connect(hostPort))
                .call();
    }

    private FlexConnection connect(HostPort hostPort) {
        try {
            return client.connect(hostPort.getHost(), hostPort.getPort()).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Connect " + hostPort + " exception", e);
            throw new ConnectionException("connection exception", e);
        }
    }

    private List<HostPort> convert(Set<String> urls) {
        return Collections.unmodifiableList(
                urls.stream().map(HostPort::new)
                    .sorted(Comparator.comparing(HostPort::toString))
                    .collect(Collectors.toList()));
    }

    private void updateActivatedList() {
        activatedList = Collections.unmodifiableList(new ArrayList<>(concurrentMap.keySet()));
    }

    @Override
    protected void init() {
        if (!CollectionUtils.isEmpty(activatedList)) {
            activatedList.forEach(hostPort -> {
                try {
                    FlexConnection connection = createConnection(hostPort);
                    if (connection != null) {
                        concurrentMap.put(hostPort, connection);
                    }
                } catch (Exception e) {
                    log.error("Connect " + hostPort + " exception", e);
                }
            });
            updateActivatedList();
        }
        scheduler.scheduleWithFixedDelay(() -> {
            log.info("Client current activated address list: {}", activatedList);
            activatedList = convert(addressProvider.getAddressList());
        }, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void destroy() {
        scheduler.stop();
    }
}

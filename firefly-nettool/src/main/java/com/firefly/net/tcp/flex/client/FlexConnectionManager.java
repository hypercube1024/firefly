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
import java.util.function.Supplier;
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
    private final List<HostPort> hostPorts;
    private final AtomicInteger index = new AtomicInteger(0);
    private final Scheduler scheduler = Schedulers.createScheduler();
    private volatile List<HostPort> activatedList;
    private volatile Supplier<List<HostPort>> resetActivatedList;

    public FlexConnectionManager(MultiplexingClient client, Set<String> hostPorts) {
        Assert.notEmpty(hostPorts);
        this.client = client;
        this.hostPorts = convert(hostPorts);
        start();
    }

    public List<HostPort> getActivatedList() {
        return activatedList;
    }

    public void setActivatedList(List<HostPort> activatedList) {
        this.activatedList = activatedList;
    }

    public Supplier<List<HostPort>> getResetActivatedList() {
        return resetActivatedList;
    }

    public void setResetActivatedList(Supplier<List<HostPort>> resetActivatedList) {
        this.resetActivatedList = resetActivatedList;
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

    private FlexConnection getConnection(HostPort hostPort) {
        try {
            FlexConnection connection = concurrentMap.computeIfAbsent(hostPort, this::createConnection);
            if (connection == null) {
                concurrentMap.remove(hostPort);
                return null;
            }
            if (connection.isOpen()) {
                return connection;
            } else {
                FlexConnection newConnection = createConnection(hostPort);
                if (newConnection != null) {
                    concurrentMap.put(hostPort, newConnection);
                    return newConnection;
                } else {
                    concurrentMap.remove(hostPort);
                    return null;
                }
            }
        } catch (Exception e) {
            log.error("get connection exception", e);
            return null;
        } finally {
            activatedList = new ArrayList<>(concurrentMap.keySet());
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

    public List<HostPort> getHostPorts() {
        return hostPorts;
    }

    private List<HostPort> convert(Set<String> urls) {
        return Collections.unmodifiableList(
                urls.stream().map(HostPort::new)
                    .sorted(Comparator.comparing(HostPort::toString))
                    .collect(Collectors.toList()));
    }

    @Override
    protected void init() {
        if (!CollectionUtils.isEmpty(hostPorts)) {
            hostPorts.forEach(hostPort -> {
                try {
                    FlexConnection connection = createConnection(hostPort);
                    if (connection != null) {
                        concurrentMap.put(hostPort, connection);
                    }
                } catch (Exception e) {
                    log.error("Connect " + hostPort + " exception", e);
                }
            });
            activatedList = new ArrayList<>(concurrentMap.keySet());
        }
        scheduler.scheduleWithFixedDelay(
                () -> activatedList = Optional.ofNullable(resetActivatedList).map(Supplier::get)
                                              .orElseGet(() -> new ArrayList<>(hostPorts)),
                5, 5, TimeUnit.SECONDS);
    }

    @Override
    protected void destroy() {

    }
}

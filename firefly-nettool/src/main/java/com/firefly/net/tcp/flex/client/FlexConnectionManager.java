package com.firefly.net.tcp.flex.client;

import com.firefly.net.tcp.codec.flex.stream.FlexConnection;
import com.firefly.net.tcp.flex.exception.ConnectionException;
import com.firefly.utils.Assert;
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

    public FlexConnectionManager(MultiplexingClient client, Set<String> hostPorts) {
        Assert.notEmpty(hostPorts);
        this.client = client;
        this.hostPorts = convert(hostPorts);
        start();
    }

    public FlexConnection removeConnection(HostPort hostPort) {
        return concurrentMap.remove(hostPort);
    }

    public FlexConnection putConnection(HostPort hostPort, FlexConnection connection) {
        return concurrentMap.put(hostPort, connection);
    }

    public FlexConnection getConnection() {
        List<HostPort> list = concurrentMap.keySet().stream()
                                           .sorted(Comparator.comparing(HostPort::toString))
                                           .collect(Collectors.toList());
        int i = Math.abs(index.getAndAdd(1)) % list.size();
        return Optional.ofNullable(list.get(i)).map(this::getConnection)
                       .orElseThrow(() -> new ConnectionException("Can not get the connection"));
    }

    public FlexConnection getConnection(HostPort hostPort) {
        FlexConnection connection = concurrentMap.computeIfAbsent(hostPort, this::createConnection);
        if (connection == null) {
            throw new ConnectionException("Can not get the connection: " + hostPort);
        }
        if (connection.isOpen()) {
            return connection;
        } else {
            FlexConnection newConnection = createConnection(hostPort);
            concurrentMap.put(hostPort, newConnection);
            return newConnection;
        }
    }

    public FlexConnection createConnection(HostPort hostPort) {
        return RetryTaskBuilder.<FlexConnection>newTask()
                .retry(ifException(ex -> ex != null && ex.getCause() instanceof TimeoutException))
                .stop(afterExecute(5))
                .wait(exponentialWait(10, TimeUnit.MILLISECONDS))
                .task(() -> this.connect(hostPort))
                .call();
    }

    private FlexConnection connect(HostPort hostPort) {
        try {
            return client.connect(hostPort.getHost(), hostPort.getPort()).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new ConnectionException("connection exception", e);
        }
    }

    public List<HostPort> getHostPorts() {
        return Collections.unmodifiableList(hostPorts);
    }

    private List<HostPort> convert(Set<String> urls) {
        return urls.stream().map(HostPort::new).sorted(Comparator.comparing(HostPort::toString)).collect(Collectors.toList());
    }

    @Override
    protected void init() {
        Optional.ofNullable(hostPorts).ifPresent(hostPorts ->
                hostPorts.forEach(hostPort -> concurrentMap.put(hostPort, createConnection(hostPort))));
    }

    @Override
    protected void destroy() {

    }
}

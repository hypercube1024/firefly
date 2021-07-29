package com.fireflysource.common.actor;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.SystemLogger;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

abstract public class AbstractActor<T> implements Runnable, Actor<T> {

    private static final LazyLogger log = SystemLogger.create(AbstractActor.class);

    private final String id;
    private final Executor executor;
    private final Queue<T> userMailbox;
    private final Queue<SystemMessage> systemMailbox;
    private final AtomicReference<TaskState> taskState = new AtomicReference<>(TaskState.IDLE);
    private final AtomicInteger unhandledUserMessageCount = new AtomicInteger(0);
    private final AtomicInteger unhandledSystemMessageCount = new AtomicInteger(0);
    private ActorState actorState = ActorState.RUNNING;

    public AbstractActor() {
        this(UUID.randomUUID().toString(), ForkJoinPool.commonPool(), new ConcurrentLinkedQueue<>(), new ConcurrentLinkedQueue<>());
    }

    public AbstractActor(String id, Executor executor) {
        this(id, executor, new ConcurrentLinkedQueue<>(), new ConcurrentLinkedQueue<>());
    }

    public AbstractActor(String id, Executor executor, Queue<T> userMailbox, Queue<SystemMessage> systemMailbox) {
        this.id = id;
        this.executor = executor;
        this.userMailbox = userMailbox;
        this.systemMailbox = systemMailbox;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean send(T message) {
        if (userMailbox.offer(message)) {
            unhandledUserMessageCount.incrementAndGet();
            dispatch();
            return true;
        } else {
            return false;
        }
    }

    public void pause() {
        sendSystemMessage(SystemMessage.PAUSE);
    }

    public void resume() {
        sendSystemMessage(SystemMessage.RESUME);
    }

    public void shutdown() {
        sendSystemMessage(SystemMessage.SHUTDOWN);
    }

    protected ActorState getActorState() {
        return actorState;
    }

    @Override
    public void run() {
        while (true) {
            handleSystemMessages();

            if (actorState == ActorState.PAUSE) {
                break;
            }

            boolean empty = handleUserMessages();
            if (empty) {
                break;
            }
        }

        dispatchNext();
    }

    private void dispatchNext() {
        taskState.set(TaskState.IDLE);
        switch (actorState) {
            case SHUTDOWN:
            case RUNNING:
                if (unhandledSystemMessageCount.get() > 0 || unhandledUserMessageCount.get() > 0) {
                    dispatch();
                }
                break;
            case PAUSE:
                if (unhandledSystemMessageCount.get() > 0) {
                    dispatch();
                }
                break;
        }
    }

    private boolean handleUserMessages() {
        boolean empty;
        T message = userMailbox.poll();
        if (message != null) {
            unhandledUserMessageCount.decrementAndGet();
            switch (actorState) {
                case RUNNING:
                    handleMessage(message);
                    break;
                case SHUTDOWN:
                    handleDiscardMessage(message);
                    break;
            }
            empty = false;
        } else {
            empty = true;
        }
        return empty;
    }

    private void handleSystemMessages() {
        SystemMessage systemMessage = systemMailbox.poll();
        if (systemMessage != null) {
            unhandledSystemMessageCount.decrementAndGet();
            switch (systemMessage) {
                case PAUSE:
                    if (actorState == ActorState.RUNNING) {
                        actorState = ActorState.PAUSE;
                    }
                    break;
                case RESUME:
                    if (actorState == ActorState.PAUSE) {
                        actorState = ActorState.RUNNING;
                    }
                    break;
                case SHUTDOWN:
                    actorState = ActorState.SHUTDOWN;
                    break;
            }
        }
    }

    private void sendSystemMessage(SystemMessage message) {
        if (systemMailbox.offer(message)) {
            unhandledSystemMessageCount.incrementAndGet();
            dispatch();
        }
    }

    private void dispatch() {
        if (taskState.compareAndSet(TaskState.IDLE, TaskState.BUSY)) {
            executor.execute(this);
        }
    }

    private void handleMessage(T message) {
        try {
            onReceive(message);
        } catch (Exception e) {
            log.error("on receive exception. id: " + getId(), e);
        }
    }

    private void handleDiscardMessage(T message) {
        try {
            onDiscard(message);
        } catch (Exception e) {
            log.error("on discard exception. id: " + getId(), e);
        }
    }

    abstract public void onReceive(T message);

    public void onDiscard(T message) {

    }

    enum TaskState {
        IDLE, BUSY
    }

    enum ActorState {
        PAUSE, RUNNING, SHUTDOWN
    }

    public enum SystemMessage {
        PAUSE, RESUME, SHUTDOWN
    }
}

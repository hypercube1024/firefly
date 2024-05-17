package com.fireflysource.common.actor;

import com.fireflysource.common.slf4j.LazyLogger;
import com.fireflysource.common.sys.SystemLogger;

import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

abstract public class AbstractActor<T> implements Runnable, Actor<T>, ActorInternalApi {

    private static final LazyLogger log = SystemLogger.create(AbstractActor.class);

    private final String address;
    private final Dispatcher dispatcher;
    private final Mailbox<T, SystemMessage> mailbox;
    private final AtomicReference<TaskState> taskState = new AtomicReference<>(TaskState.IDLE);
    private ActorState actorState = ActorState.RUNNING;

    public AbstractActor() {
        this(UUID.randomUUID().toString(), DispatcherFactory.createDispatcher(), MailboxFactory.createMailbox());
    }

    public AbstractActor(String address, Dispatcher dispatcher, Mailbox<T, SystemMessage> mailbox) {
        this.address = address;
        this.dispatcher = dispatcher;
        this.mailbox = mailbox;
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public boolean offer(T message) {
        if (mailbox.offerUserMessage(message)) {
            dispatch();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void pause() {
        sendSystemMessage(SystemMessage.PAUSE);
    }

    @Override
    public void resume() {
        sendSystemMessage(SystemMessage.RESUME);
    }

    @Override
    public void shutdown() {
        sendSystemMessage(SystemMessage.SHUTDOWN);
    }

    @Override
    public void restart() {
        sendSystemMessage(SystemMessage.RESTART);
    }

    @Override
    public ActorState getActorState() {
        return actorState;
    }

    @Override
    public void run() {
        while (true) {
            boolean systemMailboxEmpty = handleSystemMessages();

            if (actorState == ActorState.PAUSE) {
                break;
            }

            boolean userMailboxEmpty = handleUserMessages();

            if (systemMailboxEmpty && userMailboxEmpty) {
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
                if (mailbox.hasSystemMessage() || mailbox.hasUserMessage()) {
                    dispatch();
                }
                break;
            case PAUSE:
                if (mailbox.hasSystemMessage()) {
                    dispatch();
                }
                break;
        }
    }

    private boolean handleUserMessages() {
        boolean empty;
        T message = mailbox.pollUserMessage();
        if (message != null) {
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

    protected void pauseInMessageProcessThread() {
        if (actorState == ActorState.RUNNING) {
            actorState = ActorState.PAUSE;
        }
    }

    private boolean handleSystemMessages() {
        boolean empty;
        SystemMessage systemMessage = mailbox.pollSystemMessage();
        if (systemMessage != null) {
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
                case RESTART:
                    if (actorState == ActorState.SHUTDOWN) {
                        actorState = ActorState.RUNNING;
                    }
                    break;
            }
            empty = false;
        } else {
            empty = true;
        }
        return empty;
    }

    private void sendSystemMessage(SystemMessage message) {
        if (mailbox.offerSystemMessage(message)) {
            dispatch();
        }
    }

    private void dispatch() {
        if (taskState.compareAndSet(TaskState.IDLE, TaskState.BUSY)) {
            dispatcher.dispatch(this);
        }
    }

    private void handleMessage(T message) {
        try {
            onReceive(message);
        } catch (Exception e) {
            log.error("on receive exception. address: " + getAddress(), e);
        }
    }

    private void handleDiscardMessage(T message) {
        try {
            onDiscard(message);
        } catch (Exception e) {
            log.error("on discard exception. address: " + getAddress(), e);
        }
    }

    abstract public void onReceive(T message);

    public void onDiscard(T message) {

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractActor<?> that = (AbstractActor<?>) o;
        return address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }

    enum TaskState {
        IDLE, BUSY
    }

    public enum SystemMessage {
        PAUSE, RESUME, SHUTDOWN, RESTART
    }

    public static class DispatcherImpl implements Dispatcher {
        private final Executor executor;

        public DispatcherImpl(Executor executor) {
            this.executor = executor;
        }

        @Override
        public void dispatch(Runnable runnable) {
            executor.execute(runnable);
        }
    }

    public static class MailboxImpl<T> implements Mailbox<T, AbstractActor.SystemMessage> {
        private final Queue<T> userMessageQueue;
        private final Queue<AbstractActor.SystemMessage> systemMessageQueue;
        private final AtomicInteger unhandledUserMessageCount = new AtomicInteger(0);
        private final AtomicInteger unhandledSystemMessageCount = new AtomicInteger(0);

        public MailboxImpl(Queue<T> userMessageQueue, Queue<SystemMessage> systemMessageQueue) {
            this.userMessageQueue = userMessageQueue;
            this.systemMessageQueue = systemMessageQueue;
        }

        @Override
        public AbstractActor.SystemMessage pollSystemMessage() {
            AbstractActor.SystemMessage systemMessage = systemMessageQueue.poll();
            if (systemMessage != null) {
                unhandledSystemMessageCount.decrementAndGet();
            }
            return systemMessage;
        }

        @Override
        public boolean offerSystemMessage(AbstractActor.SystemMessage systemMessage) {
            boolean success = systemMessageQueue.offer(systemMessage);
            if (success) {
                unhandledSystemMessageCount.incrementAndGet();
            }
            return success;
        }

        @Override
        public boolean hasSystemMessage() {
            return unhandledSystemMessageCount.get() > 0;
        }

        @Override
        public T pollUserMessage() {
            T message = userMessageQueue.poll();
            if (message != null) {
                unhandledUserMessageCount.decrementAndGet();
            }
            return message;
        }

        @Override
        public boolean offerUserMessage(T userMessage) {
            boolean success = userMessageQueue.offer(userMessage);
            if (success) {
                unhandledUserMessageCount.incrementAndGet();
            }
            return success;
        }

        @Override
        public boolean hasUserMessage() {
            return unhandledUserMessageCount.get() > 0;
        }
    }
}

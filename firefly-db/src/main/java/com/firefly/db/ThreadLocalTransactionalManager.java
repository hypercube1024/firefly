package com.firefly.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Pengtao Qiu
 */
public class ThreadLocalTransactionalManager implements TransactionalManager {

    private final static Logger log = LoggerFactory.getLogger("firefly-system");

    private final ThreadLocal<Transaction> transaction = new ThreadLocal<>();
    private final DataSource dataSource;
    private AtomicLong idGenerator = new AtomicLong();

    public ThreadLocalTransactionalManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void beginTransaction() {
        getTransaction().beginTransaction();
    }

    @Override
    public Connection getConnection() {
        if (isTransactionBegin()) {
            return getTransaction().getConnection();
        } else {
            return getConnectionFromDataSource();
        }
    }

    @Override
    public void commit() {
        checkTransactionBegin();
        getTransaction().commit();
    }

    @Override
    public void rollback() {
        checkTransactionBegin();
        getTransaction().rollback();
    }

    @Override
    public void endTransaction() {
        checkTransactionBegin();
        if (getTransaction().endTransaction()) {
            transaction.set(null);
        }
    }

    @Override
    public boolean isTransactionBegin() {
        return transaction.get() != null;
    }

    @Override
    public long getCurrentTransactionId() {
        Transaction t = transaction.get();
        if (t != null) {
            return t.id;
        } else {
            return -1;
        }
    }

    protected void checkTransactionBegin() {
        if (!isTransactionBegin()) {
            throw new DBException("the transaction is not begin");
        }
    }

    protected Transaction getTransaction() {
        Transaction t = transaction.get();
        if (t == null) {
            t = new Transaction(dataSource, idGenerator.incrementAndGet());
            transaction.set(t);
        }
        return t;
    }

    protected Connection getConnectionFromDataSource() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.error("get connection exception", e);
            throw new DBException(e);
        }
    }

}

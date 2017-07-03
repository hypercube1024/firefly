package com.firefly.db;

import java.sql.Connection;

/**
 * @author Pengtao Qiu
 */
public interface TransactionalManager {
    void beginTransaction();

    Connection getConnection();

    void commit();

    void rollback();

    void endTransaction();
}

package com.firefly.db;

/**
 * @author Pengtao Qiu
 */
public enum TransactionIsolation {
    NONE,
    READ_UNCOMMITTED,
    READ_COMMITTED,
    REPEATABLE_READ,
    SERIALIZABLE,
    ;
}

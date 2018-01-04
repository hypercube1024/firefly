package com.firefly.codec.common;

/**
 * @author Pengtao Qiu
 */
public interface ConnectionExtInfo {

    ConnectionType getConnectionType();

    boolean isEncrypted();

}

package com.firefly.db;

import com.firefly.utils.concurrent.Promise;

/**
 * @author Pengtao Qiu
 */
public interface SQLClient {

    Promise.Completable<SQLConnection> getConnection();

}

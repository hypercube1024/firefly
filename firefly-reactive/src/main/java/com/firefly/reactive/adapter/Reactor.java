package com.firefly.reactive.adapter;

import com.firefly.db.SQLClient;
import com.firefly.db.SQLConnection;
import com.firefly.reactive.adapter.db.ReactiveSQLClient;
import com.firefly.reactive.adapter.db.ReactiveSQLClientAdapter;
import com.firefly.reactive.adapter.db.ReactiveSQLConnection;
import com.firefly.reactive.adapter.db.ReactiveSQLConnectionAdapter;
import com.firefly.reactive.adapter.http.ReactiveHTTPClient;
import com.firefly.reactive.adapter.http.ReactiveHTTPClientSingleton;

/**
 * Hello world!
 */
public interface Reactor {

    interface db {
        static ReactiveSQLClient fromSQLClient(SQLClient sqlClient) {
            return new ReactiveSQLClientAdapter(sqlClient);
        }

        static ReactiveSQLConnection fromSQLConnection(SQLConnection sqlConnection) {
            return new ReactiveSQLConnectionAdapter(sqlConnection);
        }
    }

    interface http {

        static ReactiveHTTPClient httpClient() {
            return ReactiveHTTPClientSingleton.getInstance().httpClient();
        }

    }
}

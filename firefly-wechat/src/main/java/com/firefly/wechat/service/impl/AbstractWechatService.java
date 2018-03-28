package com.firefly.wechat.service.impl;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleResponse;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.wechat.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

/**
 * @author Pengtao Qiu
 */
public class AbstractWechatService {

    protected static Logger log = LoggerFactory.getLogger("firefly-system");

    protected SimpleHTTPClient client;

    public AbstractWechatService() {
    }

    public AbstractWechatService(SimpleHTTPClient client) {
        this.client = client;
    }

    public SimpleHTTPClient getClient() {
        return client;
    }

    public void setClient(SimpleHTTPClient client) {
        this.client = client;
    }

    protected <T> CompletableFuture<T> callWechatService(String url, String param, Class<T> clazz) {
        CompletableFuture<T> ret = new CompletableFuture<>();
        client.get(url + "?" + param).submit()
              .thenAccept(res -> {
                  log.info("call wechat service -> {}, {}, {}, {}", url, param, res.getStatus(), res.getStringBody());
                  complete(ret, res, clazz);
              });
        return ret;
    }

    protected <T> void complete(CompletableFuture<T> ret, SimpleResponse res, Class<T> clazz) {
        if (res.getStatus() == HttpStatus.OK_200) {
            if (res.getJsonObjectBody().getInteger("errcode") != 0) {
                ErrorResponse errorResponse = res.getJsonBody(ErrorResponse.class);
                ret.completeExceptionally(errorResponse);
            } else {
                T response = res.getJsonBody(clazz);
                ret.complete(response);
            }
        } else {
            ErrorResponse errorResponse = res.getJsonBody(ErrorResponse.class);
            ret.completeExceptionally(errorResponse);
        }
    }
}

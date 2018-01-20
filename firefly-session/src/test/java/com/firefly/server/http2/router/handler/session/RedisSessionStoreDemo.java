package com.firefly.server.http2.router.handler.session;

import com.firefly.$;
import com.firefly.server.http2.router.HTTPSession;
import org.redisson.Redisson;
import org.redisson.api.RedissonReactiveClient;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class RedisSessionStoreDemo {

    public static void main(String[] args) {
        RedisSessionStore store = new RedisSessionStore();
        RedissonReactiveClient client = Redisson.createReactive();
        store.setClient(client);
        store.setKeyPrefix("com:fireflysource");
        store.setSessionKey("test_session");
        store.start();

        String id = UUID.randomUUID().toString().replace("-", "");
        HTTPSession session = HTTPSession.create(id, 3);
        session.getAttributes().put("myTest", "hello ok!~");
        store.put(id, session)
             .thenCompose(ret -> {
                 System.out.println("put session: " + ret);
                 return store.get(id);
             })
             .thenApply(s -> s.getAttributes().get("myTest"))
             .thenAccept(System.out::println);

        $.thread.sleep(5, TimeUnit.SECONDS);
        store.get(id)
             .thenApply(s -> s.getAttributes().get("myTest"))
             .thenAccept(System.out::println)
             .exceptionally(t -> {
                 t.printStackTrace();
                 return null;
             });
    }
}

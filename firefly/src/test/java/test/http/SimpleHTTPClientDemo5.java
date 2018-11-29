package test.http;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.net.tcp.secure.conscrypt.ConscryptSecureSessionFactory;
import com.firefly.net.tcp.secure.jdk.JdkSecureSessionFactory;
import com.firefly.utils.concurrent.ThreadUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author Pengtao Qiu
 */
public class SimpleHTTPClientDemo5 {
    public static void main(String[] args) {
//        SimpleHTTPClient client = $.createHTTPsClient(new ConscryptSecureSessionFactory());
        SimpleHTTPClient client = $.createHTTPsClient(new JdkSecureSessionFactory());
        // https://login.taobao.com
        for (int i = 0; i < 30; i++) {
            final int j = i;
            client.get("https://login.taobao.com").submit()
                  .thenAccept(resp -> System.out.println("size: " + resp.getStringBody().length() + ", " + j));
            ThreadUtils.sleep(1, TimeUnit.SECONDS);
        }

    }
}

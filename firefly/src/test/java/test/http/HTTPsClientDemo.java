package test.http;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.codec.http2.model.HttpStatus;
import com.firefly.net.tcp.secure.conscrypt.ConscryptSecureSessionFactory;
import com.firefly.net.tcp.secure.jdk.JdkSecureSessionFactory;
import com.firefly.net.tcp.secure.openssl.DefaultOpenSSLSecureSessionFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author Pengtao Qiu
 */
public class HTTPsClientDemo {
    public static final List<String> urlList = Arrays.asList(
            "https://www.jd.com",
            "https://segmentfault.com",
            "https://github.com",
            "https://www.taobao.com",
            "https://www.baidu.com");

    public static void main(String[] args) throws InterruptedException {
        SimpleHTTPClient client = $.createHTTPsClient(new ConscryptSecureSessionFactory());
        for (int i = 0; i < 3; i++) {
            CountDownLatch latch = new CountDownLatch(urlList.size());
            urlList.forEach(url -> {
                long start = System.currentTimeMillis();
                client.get(url).submit().thenAccept(resp -> {
                    long end = System.currentTimeMillis();
                    if (resp.getStatus() == HttpStatus.OK_200) {
                        System.out.println("The " + url + " is OK. " +
                                "Size: " + resp.getStringBody().length() + ". " +
                                "Time: " + (end - start) + ". " +
                                "Version: " + resp.getHttpVersion());
                    } else {
                        System.out.println("The " + url + " is failed. " +
                                "Status: " + resp.getStatus() + ". " +
                                "Time: " + (end - start) + ". " +
                                "Version: " + resp.getHttpVersion());
                    }
                    latch.countDown();
                });
            });
            latch.await();
            System.out.println("test " + i + " completion. ");
            client.stop();
        }
    }
}

package test.http;

import com.firefly.$;
import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.client.http2.SimpleHTTPClientConfiguration;
import com.firefly.codec.http2.model.HttpVersion;

/**
 * @author Pengtao Qiu
 */
public class DirectHTTP2ClientDemo {
    public static void main(String[] args) {
        SimpleHTTPClientConfiguration configuration = new SimpleHTTPClientConfiguration();
        configuration.setProtocol(HttpVersion.HTTP_2.asString());
        SimpleHTTPClient httpClient = $.createHTTPClient(configuration);
        httpClient.get("http://localhost:2242/test").submit().thenAccept(res -> {
            System.out.println(res.getFields());
            System.out.println(res.getStringBody());
        });
    }
}

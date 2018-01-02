package test.http;

import com.firefly.$;

/**
 * @author Pengtao Qiu
 */
public class HTTP2PingDemo {
    public static void main(String[] args) {
        $.httpsClient().get("https://www.jd.com")
         .submit()
         .thenAccept(resp -> {
             System.out.println(resp.getStringBody());
             System.out.println(resp.getHttpVersion());
         });
    }
}

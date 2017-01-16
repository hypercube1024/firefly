package test.http;

import com.firefly.client.http2.SimpleHTTPClient;
import com.firefly.server.http2.SimpleHTTPServer;
import com.firefly.server.http2.SimpleHTTPServerConfiguration;
import com.firefly.server.http2.SimpleResponse;

import java.io.PrintWriter;

/**
 * @author Pengtao Qiu
 */
public class ProxyDemo {
    public static void main(String[] args) {
        SimpleHTTPClient client = new SimpleHTTPClient();
        SimpleHTTPServerConfiguration configuration = new SimpleHTTPServerConfiguration();
//        configuration.setSecureConnectionEnabled(true);
        SimpleHTTPServer server = new SimpleHTTPServer(configuration);
        server.headerComplete(request -> request.messageComplete(req -> {
            SimpleResponse response = req.getResponse();

            System.out.println(req.getRequest().toString());
            System.out.println(req.getRequest().getFields());
            System.out.println(req.getStringBody());

            long start = System.currentTimeMillis();
            client.get(req.getRequest().getURI().toString())
                  .submit()
                  .thenApply(res -> {
                      response.getResponse().getFields().addAll(res.getResponse().getFields());
                      return res.getStringBody("UTF-8");
                  })
                  .thenAccept(v -> {
                      try (PrintWriter writer = response.getPrintWriter()) {
                          writer.print(v);
                      }
                      System.out.println("time: " + (System.currentTimeMillis() - start));
                  });


        })).listen("localhost", 3344);
    }
}

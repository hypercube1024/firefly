package com.firefly.example.http.hello;

import com.firefly.$;
import com.firefly.codec.http2.model.InputStreamContentProvider;
import com.firefly.codec.http2.model.StringContentProvider;
import com.firefly.server.http2.HTTP2ServerBuilder;

import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Phaser;

/**
 * @author Pengtao Qiu
 */
public class MultipartDemo {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;
        String uri = "http://" + host + ":" + port;
        Phaser phaser = new Phaser(3);

        HTTP2ServerBuilder httpServer = $.httpServer();
        httpServer.router().post("/upload/string").handler(ctx -> {
            // small multi part data test case
            Part test1 = ctx.getPart("test1");
            Part test2 = ctx.getPart("test2");
            try (InputStream input1 = test1.getInputStream();
                 InputStream input2 = test2.getInputStream()) {
                String value = $.io.toString(input1);
                System.out.println(value);

                String value2 = $.io.toString(input2);
                System.out.println(value2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ctx.end("server received multi part data");
        }).router().post("/upload/poetry").handler(ctx -> {
            // upload poetry
            Part poetry = ctx.getPart("poetry");
            System.out.println(poetry.getSubmittedFileName());
            try (InputStream inputStream = $.class.getResourceAsStream("/static/poem.txt")) {
                String poem = $.io.toString(inputStream);
                System.out.println(poem);
            } catch (IOException e) {
                e.printStackTrace();
            }
            ctx.end("server received poetry");
        }).listen(host, port);

        $.httpClient().post(uri + "/upload/string")
         .addFieldPart("test1", new StringContentProvider("hello multi part1"), null)
         .addFieldPart("test2", new StringContentProvider("hello multi part2"), null)
         .submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             phaser.arrive();
         });

        InputStream inputStream = $.class.getResourceAsStream("/static/poem.txt");
        InputStreamContentProvider inputStreamContentProvider = new InputStreamContentProvider(inputStream);
        $.httpClient().post(uri + "/upload/poetry")
         .addFilePart("poetry", "poem.txt", inputStreamContentProvider, null)
         .submit()
         .thenAccept(res -> {
             System.out.println(res.getStringBody());
             $.io.close(inputStreamContentProvider);
             $.io.close(inputStream);
             phaser.arrive();
         });

        phaser.arriveAndAwaitAdvance();
        httpServer.stop();
        $.httpClient().stop();
    }
}

package test.http;

import com.firefly.server.http2.servlet.ServerBootstrap;

public class ServerDemo2 {


    public static void main(String[] args) throws Throwable {
        ServerBootstrap bootstrap = new ServerBootstrap("firefly-server2.xml");
        bootstrap.start();

//		Thread.sleep(2000L);
//		bootstrap.stop();
    }

}

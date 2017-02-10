# What is Firefly?

Firefly framework is a asynchronous java web framework. It helps you create a web application ***Easy*** and ***Quickly***. It provides MVC framework, asynchronous HTTP Server/Client, asynchronous TCP Server/Client and many other useful components for developing web applications, protocol servers, etc. That means you can easy deploy your web without any other java web containers, in short , it's containerless. It taps into the fullest potential of hardware using ***SEDA*** architecture, a highly customizable thread model.  

Firefly core provides functionality for things like:
- Writing TCP clients and servers
- Writing HTTP clients and servers
- Writing web application with MVC framework and template engine
- Database access

# Event driven

The Firefly APIs are largely event driven. This means that when things happen in Firefly that you are interested in, Firefly will call you by sending you events.

Some example events are:
- some data has arrived on a socket
- an HTTP server has received a request

Firefly handles a lot of concurrency using just a small number of threads, so ***don't block Firefly thread***, you must manage blocking call in the standalone thread pool.  

With a conventional blocking API the calling thread might block when:
- Thread.sleep()
- Waiting on a lock
- Waiting on a mutex or monitor
- Doing a long lived database operation and waiting for a result
- Call blocking I/O APIs

In all the above cases, when your thread is waiting for a result it can’t do anything else - it’s effectively useless.

This means that if you want a lot of concurrency using blocking APIs then you need a lot of threads to prevent your application grinding to a halt.

Threads have overhead in terms of the memory they require (e.g. for their stack) and in context switching.

For the levels of concurrency required in many modern applications, a blocking approach just doesn’t scale.


# Writing TCP servers and clients

Add maven dependency in your pom.xml.
```xml
<dependency>
    <groupId>com.fireflysource</groupId>
    <artifactId>firefly</artifactId>
    <version>4.0.21</version>
</dependency>

<dependency>
    <groupId>com.fireflysource</groupId>
    <artifactId>firefly-slf4j</artifactId>
    <version>4.0.21</version>
</dependency>
```

Write the test case as follows:
```java
@RunWith(Parameterized.class)
public class TestSimpleTcpServerAndClient {

    @Parameter
    public Run r;

    static class Run {
        TcpConfiguration clientConfig;
        TcpServerConfiguration serverConfig;
        int port;
        int maxMsg;
        String testName;

        @Override
        public String toString() {
            return testName;
        }
    }

    @Parameters(name = "{0}")
    public static Collection<Run> data() {
        List<Run> data = new ArrayList<>();
        Run run = new Run();
        run.clientConfig = new TcpConfiguration();
        run.serverConfig = new TcpServerConfiguration();
        run.port = 1212;
        run.maxMsg = 5;
        run.testName = "Test TCP server and client";
        data.add(run);

        run = new Run();
        run.clientConfig = new TcpConfiguration();
        run.clientConfig.setSecureConnectionEnabled(true); // enable TLS
        run.serverConfig = new TcpServerConfiguration();
        run.serverConfig.setSecureConnectionEnabled(true);
        run.port = 1213;
        run.maxMsg = 20;
        run.testName = "Test TCP server and client with TLS";
        data.add(run);

        return data;
    }


    @Test
    public void test() {
        SimpleTcpClient client = new SimpleTcpClient(r.clientConfig);
        SimpleTcpServer server = new SimpleTcpServer(r.serverConfig);
        int port = r.port;
        int maxMsg = r.maxMsg;
        Phaser phaser = new Phaser(3);

        server.accept(connection -> { // accept a client connection
            StringParser parser = new StringParser();
            AtomicInteger msgCount = new AtomicInteger();
            parser.complete(message -> { // parse string message using delimiter '\n'
                String s = message.trim();
                System.out.println("server receives message -> " + s);
                switch (s) {
                    case "quit":
                        connection.write("bye!\r\n");
                        IO.close(connection);
                        Assert.assertThat(msgCount.get(), is(maxMsg));
                        phaser.arrive();
                        break;
                    default:
                        msgCount.incrementAndGet();
                        connection.write("response message [" + s + "]\r\n");
                        break;
                }
            });
            connection.receive(parser::receive);
        }).listen("localhost", port);


        client.connect("localhost", port)
              .thenAccept(c -> {
                  StringParser parser = new StringParser();
                  AtomicInteger msgCount = new AtomicInteger();
                  parser.complete(message -> {
                      String s = message.trim();
                      System.out.println("client receives message -> " + s);
                      switch (s) {
                          case "bye!":
                              Assert.assertThat(msgCount.get(), is(maxMsg));
                              phaser.arrive();
                              break;
                          default:
                              msgCount.incrementAndGet();
                              break;
                      }
                  });
                  c.receive(parser::receive);
                  for (int i = 0; i < maxMsg; i++) {
                      c.write("hello world" + i + "!\r\n");
                  }
                  c.write("quit\r\n");
              });

        phaser.arriveAndAwaitAdvance();
        client.stop();
        server.stop();
    }
}
```
Firefly TCP server/client can guarantee the message order in the same connection if you write (or receive) message in the Firefly network event thread.

TCP client sends message order
```
"hello world0!", "hello world1!", "hello world2!" , "hello world3!" ......
```

TCP server receives message order
```
"hello world0!", "hello world1!", "hello world2!" , "hello world3!" ......
```


Run JUnit result:
```
server receives message -> hello world0!
server receives message -> hello world1!
server receives message -> hello world2!
server receives message -> hello world3!
client receives message -> response message [hello world0!]
server receives message -> hello world4!
server receives message -> quit
client receives message -> response message [hello world1!]
client receives message -> response message [hello world2!]
client receives message -> response message [hello world3!]
client receives message -> response message [hello world4!]
client receives message -> bye!

......
```

# Writing HTTP servers and clients
```java
public class TestHTTPServerAndClient {

    @Parameter
    public Run r;

    static class Run {
        SimpleHTTPClientConfiguration clientConfig;
        SimpleHTTPServerConfiguration serverConfig;
        String requestURL;
        String quitURL;
        int port;
        int maxMsg;
        String testName;

        @Override
        public String toString() {
            return testName;
        }
    }

    @Parameters(name = "{0}")
    public static Collection<Run> data() {
        List<Run> data = new ArrayList<>();
        Run run = new Run();
        run.clientConfig = new SimpleHTTPClientConfiguration();
        run.serverConfig = new SimpleHTTPServerConfiguration();
        run.port = 1332;
        run.maxMsg = 5;
        run.requestURL = "http://localhost:" + run.port + "/";
        run.quitURL = "http://localhost:" + run.port + "/quit";
        run.testName = "Test HTTP server and client";
        data.add(run);

        run = new Run();
        run.clientConfig = new SimpleHTTPClientConfiguration();
        run.clientConfig.setSecureConnectionEnabled(true); // enable HTTPs
        run.serverConfig = new SimpleHTTPServerConfiguration();
        run.serverConfig.setSecureConnectionEnabled(true);
        run.port = 1333;
        run.maxMsg = 15;
        run.requestURL = "https://localhost:" + run.port + "/";
        run.quitURL = "https://localhost:" + run.port + "/quit";
        run.testName = "Test HTTP server and client with TLS";
        data.add(run);

        return data;
    }

    @Test
    public void test() {
        SimpleHTTPServer server = new SimpleHTTPServer(r.serverConfig);
        SimpleHTTPClient client = new SimpleHTTPClient(r.clientConfig);
        int port = r.port;
        int maxMsg = r.maxMsg;
        Phaser phaser = new Phaser(maxMsg + 2);

        AtomicInteger msgCount = new AtomicInteger();
        server.headerComplete(r -> r.messageComplete(request -> {
            SimpleResponse response = request.getResponse();
            String path = request.getURI().getPath();

            System.out.println("server receives message -> " + request.getStringBody());
            response.getFields().put(HttpHeader.CONTENT_TYPE, MimeTypes.Type.TEXT_PLAIN.asString());
            switch (path) {
                case "/": {
                    msgCount.incrementAndGet();
                    try (PrintWriter writer = response.getPrintWriter()) {
                        writer.print("response message [" + request.getStringBody() + "]");
                    }
                }
                break;
                case "/quit": {
                    try (PrintWriter writer = response.getPrintWriter()) {
                        writer.print("bye!");
                    }
                }
                break;
            }
            phaser.arrive();
        })).listen("localhost", port);

        for (int i = 0; i < maxMsg; i++) {
            client.post(r.requestURL)
                  .body("hello world" + i + "!")
                  .submit()
                  .thenAcceptAsync(r -> System.out.println("client receives message -> " + r.getStringBody()));
        }
        client.post(r.quitURL)
              .body("quit test")
              .submit()
              .thenAcceptAsync(r -> System.out.println("client receives message -> " + r.getStringBody()));

        phaser.arriveAndAwaitAdvance();
        Assert.assertThat(msgCount.get(), is(maxMsg));
        client.stop();
        server.stop();
    }
}
```

Firefly HTTP client/server supports both HTTP1 and HTTP2 protocol, when you enable the TLS configuration, Firefly will negotiate HTTP version using ALPN automatically.  

The HTTP client uses a bounded connection pool to send requests. In this example, the client sends request in different TCP connections, so the HTTP server received messages are unordered.

Run JUnit result:
```
server receives message -> hello world0!
server receives message -> hello world3!
server receives message -> hello world2!
server receives message -> hello world4!
client receives message -> response message [hello world3!]
client receives message -> response message [hello world0!]
server receives message -> hello world1!
client receives message -> response message [hello world2!]
client receives message -> response message [hello world4!]
client receives message -> response message [hello world1!]
server receives message -> quit test
client receives message -> bye!

......
```

More detailed information, please refer to the [full document](http://www.fireflysource.com)

#Contact information
E-mail: qptkk@163.com  
QQ Group: 126079579

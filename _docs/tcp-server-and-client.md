---

category : docs
layout: document
title: TCP server and client

---
**Table of Contents**

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [AIO or NIO](#aio-or-nio)
- [Writing TCP server and client](#writing-tcp-server-and-client)

<!-- /TOC -->

# AIO or NIO

NIO bases on the Reactor pattern. When the socket flows readable or writable, the operating system will notify reference procedure, application of the current read into the buffer or write the operating system.

Unlike NIO, when the read and write operation, can only directly call API read or write method. These two methods are asynchronous.  For a read operation, when the stream can be read,  the operating system will spread into the read buffer method and readable, and notify the application;  for a write operation, when the operating system will write transmission stream to write is completed, the operating system to inform application program.
That can be understood as, read/write methods are asynchronous. After completion will take the initiative to call the callback function.

Here is the table showing the different of NIO, AIO, and BIO.
<table class="table table-striped table-hover ">
<thead>
<tr>
  <th>#</th>
  <th>NIO</th>
  <th>AIO</th>
  <th>BIO</th>
</tr>
</thead>
<tbody>
  <tr>
    <td>Performance</td>
    <td>good</td>
    <td>good</td>
    <td>poor scalability</td>
  </tr>
  <tr>
    <td>Easy to use</td>
    <td>complex</td>
    <td>good</td>
    <td>very good</td>
  </tr>
  <tr>
    <td>Zero copy APIs</td>
    <td>yes</td>
    <td>no</td>
    <td>no</td>
  </tr>
</tbody>
</table>

The AIO provides good performance and scalability. Also, it's easy to use, so Firefly TCP framework just wraps AIO APIs simply and using a writing queue to avoid some common exceptions, such as "java.nio.channels.WritePendingException",  "java.nio.channels.AsynchronousCloseException" and so on.

When the network card buffer is full, the user data will not send entirely. Firefly will retry to send data until the data is sent completely.


# Writing TCP server and client

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

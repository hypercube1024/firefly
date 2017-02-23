---

category : docs
layout: document
title: TCP server and client

---
**Table of Contents**
<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->

- [AIO or NIO](#aio-or-nio)
- [Writing TCP servers and clients](#writing-tcp-servers-and-clients)

<!-- /TOC -->

# AIO or NIO

# Writing TCP servers and clients

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

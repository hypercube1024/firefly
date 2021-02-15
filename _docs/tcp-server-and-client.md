---

category : docs
title: TCP server and client

---
**Table of Contents**

<!-- TOC depthFrom:1 depthTo:6 withLinks:1 updateOnSave:1 orderedList:0 -->
- [TCP server and client](#tcp-server-and-client)

<!-- /TOC -->

# TCP server and client
```kotlin
fun main() {
    `$`.tcpServer().onAcceptAsync { connection -> // (1)
        connection.coroutineScope.launch { writeLoop("Server", connection) }
        connection.coroutineScope.launch { readLoop(connection) }
    }.listen("localhost", 8090)

    `$`.tcpClient().connectAsync("localhost", 8090) { connection -> // (2)
        connection.coroutineScope.launch { writeLoop("Client", connection) } // (3)
        connection.coroutineScope.launch { readLoop(connection) }
    }
}

private suspend fun readLoop(connection: TcpConnection) {
    while (true) {
        try {
            val buffer = connection.read().await()
            println(BufferUtils.toString(buffer))
        } catch (e: Exception) {
            println("Connection closed.")
            break
        }
    }
}

private suspend fun writeLoop(data: String, connection: TcpConnection) {
    (1..10).forEach {
        connection.write(toBuffer("${data}. count: $it, time: ${Date()}"))
        delay(1000)
    }
    connection.closeAsync().await()
}
```
1. Use the `onAcceptAsync` method to accept tcp connection.
2. Use the `connectAsync` method to establish a tcp connection to the server.
3. Lauch a coroutine and write data in a loop.

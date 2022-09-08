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
        launch { writeLoop("Server", connection) } 
        launch { readLoop(connection) }
    }.listen("localhost", 8090)

    `$`.tcpClient().connectAsync("localhost", 8090) { connection -> // (2)
        launch { writeLoop("Client", connection) } 
        launch { readLoop(connection) }
    }
}

private suspend fun readLoop(connection: TcpConnection) = connection.useAwait { // (3)
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

private suspend fun writeLoop(data: String, connection: TcpConnection) = connection.useAwait {
    (1..10).forEach { // (4)
        connection.write(toBuffer("TCP ${data}. count: $it, time: ${Date()}"))
        delay(1000)
    }
}
```
1. Use the `onAcceptAsync` method to accept tcp connection.
2. Use the `connectAsync` method to establish a tcp connection to the server.
3. Lauch a coroutine and read data in a loop. The function `connection.useAwait` closes connection automatically when the `readLoop` exits.
4. Write data in a loop.
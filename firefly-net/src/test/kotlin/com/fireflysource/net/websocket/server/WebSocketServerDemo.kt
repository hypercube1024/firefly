package com.fireflysource.net.websocket.server

import com.fireflysource.common.sys.Result
import com.fireflysource.net.http.client.HttpClientFactory
import com.fireflysource.net.http.server.HttpServerFactory
import com.fireflysource.net.websocket.common.frame.Frame
import com.fireflysource.net.websocket.common.frame.TextFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * @author Pengtao Qiu
 */

const val html = """
<!DOCTYPE html>
<html>
<head>
    <title>Hello WebSocket</title>
</head>
<body>

<div>Hello WebSocket</div>
<div id="content">

</div>

<script
  src="https://code.jquery.com/jquery-3.5.1.min.js"
  integrity="sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0="
  crossorigin="anonymous"></script>
<script>
    var ws = new WebSocket('ws://localhost:8999/helloWebSocket');
    var f;
    ws.onopen = function () {
        f = setInterval(function () {
            ws.send('Hello Server. time: ' + new Date())
        }, 2000)
    };

    ws.onclose = function () {
        clearInterval(f);
    };

    ws.onmessage = function (event) {
        console.log(event);
        ${'$'}("#content").append("<p>" + event.data + "</p>");
    };
</script>
</body>
</html>
"""

fun main() = runBlocking {
    val server = HttpServerFactory.create()
    server
        .router().get("/websocket/test")
        .handler { ctx -> ctx.end(html) }
        .websocket("/helloWebSocket")
        .onMessage { frame, _ ->
            if (frame.type == Frame.Type.TEXT && frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .onAccept { connection ->
            connection.coroutineScope.launch {
                while (true) {
                    connection.sendText("Server time: ${Date()}")
                    delay(1000)
                }
            }
            Result.DONE
        }
        .listen("localhost", 8999)

    val client = HttpClientFactory.create()
    val webSocketConnection = client.websocket("ws://localhost:8999/helloWebSocket")
        .onMessage { frame, _ ->
            if (frame.type == Frame.Type.TEXT && frame is TextFrame) {
                println(frame.payloadAsUTF8)
            }
            Result.DONE
        }
        .connect()
        .await()
    launch {
        while (true) {
            delay(2000)
            webSocketConnection.sendText("Client time: ${Date()}")
        }
    }
    Unit
}
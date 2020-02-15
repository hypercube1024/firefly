package com.fireflysource.net.http.server.impl.content.handler

import com.fireflysource.net.http.common.content.handler.AbstractFileContentHandler
import com.fireflysource.net.http.server.HttpServerContentHandler
import com.fireflysource.net.http.server.RoutingContext
import java.nio.file.OpenOption
import java.nio.file.Path

class FileContentHandler(path: Path, vararg options: OpenOption) :
    AbstractFileContentHandler<RoutingContext>(path, *options), HttpServerContentHandler
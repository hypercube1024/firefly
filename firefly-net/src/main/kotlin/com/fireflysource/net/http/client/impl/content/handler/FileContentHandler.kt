package com.fireflysource.net.http.client.impl.content.handler

import com.fireflysource.net.http.client.HttpClientContentHandler
import com.fireflysource.net.http.client.HttpClientResponse
import com.fireflysource.net.http.common.content.handler.AbstractFileContentHandler
import java.nio.file.OpenOption
import java.nio.file.Path

class FileContentHandler(path: Path, vararg options: OpenOption) :
    AbstractFileContentHandler<HttpClientResponse>(path, *options), HttpClientContentHandler
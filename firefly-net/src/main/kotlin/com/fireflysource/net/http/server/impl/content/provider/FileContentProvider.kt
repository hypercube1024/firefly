package com.fireflysource.net.http.server.impl.content.provider

import com.fireflysource.net.http.common.content.provider.AbstractFileContentProvider
import com.fireflysource.net.http.server.HttpServerContentProvider
import java.nio.file.OpenOption
import java.nio.file.Path

class FileContentProvider(path: Path, vararg options: OpenOption) :
    AbstractFileContentProvider(path, *options), HttpServerContentProvider
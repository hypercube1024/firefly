package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.net.http.client.HttpClientContentProvider
import com.fireflysource.net.http.common.content.provider.AbstractFileContentProvider
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path

class FileContentProvider(
    path: Path,
    options: Set<OpenOption>,
    position: Long,
    length: Long
) : AbstractFileContentProvider(path, options, position, length), HttpClientContentProvider {

    constructor(path: Path, vararg options: OpenOption) : this(path, options.toSet(), 0, Files.size(path))

}
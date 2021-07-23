package com.fireflysource.net.http.server.impl.content.provider

import com.fireflysource.net.http.common.content.provider.AbstractFileContentProvider
import com.fireflysource.net.http.server.HttpServerContentProvider
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path

class FileContentProvider(
    path: Path,
    options: Set<OpenOption>,
    position: Long,
    length: Long,
    scope: CoroutineScope = CoroutineScope(CoroutineName("Firefly-file-content-provider"))
) : AbstractFileContentProvider(path, options, position, length, scope), HttpServerContentProvider {

    constructor(path: Path, vararg options: OpenOption) : this(path, options.toSet(), 0, Files.size(path))

    constructor(path: Path, scope: CoroutineScope, vararg options: OpenOption) : this(
        path,
        options.toSet(),
        0,
        Files.size(path),
        scope
    )

    constructor(
        path: Path,
        options: Set<OpenOption>,
        position: Long,
        length: Long
    ) : this(path, options, position, length, CoroutineScope(CoroutineName("Firefly-file-content-provider")))
}
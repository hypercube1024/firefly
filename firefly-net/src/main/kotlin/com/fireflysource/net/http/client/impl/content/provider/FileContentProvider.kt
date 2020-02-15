package com.fireflysource.net.http.client.impl.content.provider

import com.fireflysource.net.http.client.HttpClientContentProvider
import com.fireflysource.net.http.common.content.provider.AbstractFileContentProvider
import java.nio.file.OpenOption
import java.nio.file.Path

class FileContentProvider(path: Path, vararg options: OpenOption) :
    AbstractFileContentProvider(path, *options), HttpClientContentProvider
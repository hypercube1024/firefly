package com.fireflysource.net.http.common.v1.encoder

import com.fireflysource.common.`object`.Assert

fun HttpGenerator.Result.assert(expectResult: HttpGenerator.Result) {
    Assert.state(
        this == expectResult,
        "The HTTP generator result is $this, but expect $expectResult"
    )
}

fun HttpGenerator.Result.assert(expectResults: Set<HttpGenerator.Result>) {
    Assert.state(
        expectResults.contains(this),
        "The HTTP generator result is $this, but expect $expectResults"
    )
}
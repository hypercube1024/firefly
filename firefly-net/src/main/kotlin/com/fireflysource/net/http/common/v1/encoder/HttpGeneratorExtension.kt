package com.fireflysource.net.http.common.v1.encoder

import com.fireflysource.net.http.common.exception.Http1GeneratingResultException

fun HttpGenerator.Result.assert(expectResult: HttpGenerator.Result) {
    if (this != expectResult) {
        throw Http1GeneratingResultException("The HTTP generator result is $this, but expect $expectResult")
    }
}

fun HttpGenerator.Result.assert(expectResults: Set<HttpGenerator.Result>) {
    if (!expectResults.contains(this)) {
        throw Http1GeneratingResultException("The HTTP generator result is $this, but expect $expectResults")
    }
}
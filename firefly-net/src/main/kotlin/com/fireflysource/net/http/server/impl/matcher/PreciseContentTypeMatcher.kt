package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.common.string.StringUtils
import com.fireflysource.net.http.common.model.MimeTypes
import com.fireflysource.net.http.server.Matcher

class PreciseContentTypeMatcher : AbstractPreciseMatcher() {

    override fun match(value: String): Matcher.MatchResult? {
        val mimeType = MimeTypes.getContentTypeMIMEType(value)
        return if (StringUtils.hasText(mimeType)) super.match(mimeType) else null
    }

    override fun getMatchType(): Matcher.MatchType {
        return Matcher.MatchType.CONTENT_TYPE
    }

}
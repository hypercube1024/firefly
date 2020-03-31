package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.common.string.StringUtils
import com.fireflysource.net.http.common.model.MimeTypes
import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Matcher.MatchType


class ContentTypePatternMatcher : AbstractPatternMatcher() {

    override fun getMatchType(): MatchType {
        return MatchType.CONTENT_TYPE
    }

    override fun match(value: String): Matcher.MatchResult? {
        val mimeType = MimeTypes.getContentTypeMIMEType(value)
        return if (StringUtils.hasText(mimeType)) super.match(mimeType) else null
    }
}
package com.fireflysource.net.http.server.impl.matcher

import com.fireflysource.common.string.StringUtils
import com.fireflysource.net.http.common.model.MimeTypes
import com.fireflysource.net.http.server.Matcher
import com.fireflysource.net.http.server.Matcher.MatchType


class PatternedContentTypeMatcher : AbstractPatternMatcher() {

    override fun match(value: String): Matcher.MatchResult? {
        if (routersMap.isEmpty()) return null

        val mimeType = MimeTypes.getContentTypeMIMEType(value)
        return if (StringUtils.hasText(mimeType)) super.match(mimeType) else null
    }

    override fun getMatchType(): MatchType {
        return MatchType.CONTENT_TYPE
    }
}
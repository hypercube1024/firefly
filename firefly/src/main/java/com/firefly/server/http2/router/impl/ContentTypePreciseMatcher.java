package com.firefly.server.http2.router.impl;

import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.utils.StringUtils;

/**
 * @author Pengtao Qiu
 */
public class ContentTypePreciseMatcher extends AbstractPreciseMatcher {

    @Override
    public MatchType getMatchType() {
        return MatchType.CONTENT_TYPE;
    }

    @Override
    public MatchResult match(String value) {
        String mimeType = MimeTypes.getContentTypeMIMEType(value);
        if (StringUtils.hasText(mimeType)) {
            return super.match(mimeType);
        } else {
            return null;
        }
    }
}

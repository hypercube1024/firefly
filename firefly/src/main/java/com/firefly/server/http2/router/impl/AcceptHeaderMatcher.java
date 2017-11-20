package com.firefly.server.http2.router.impl;

import com.firefly.codec.http2.model.AcceptMIMEType;
import com.firefly.server.http2.router.Router;
import com.firefly.utils.CollectionUtils;
import com.firefly.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import static com.firefly.codec.http2.model.MimeTypes.parseAcceptMIMETypes;

/**
 * @author Pengtao Qiu
 */
public class AcceptHeaderMatcher extends AbstractPreciseMatcher {

    @Override
    public MatchType getMatchType() {
        return MatchType.ACCEPT;
    }

    @Override
    public MatchResult match(String value) {
        if (map == null) {
            return null;
        }

        List<AcceptMIMEType> acceptMIMETypes = parseAcceptMIMETypes(value);
        if (CollectionUtils.isEmpty(acceptMIMETypes)) {
            return null;
        }
        
        for (AcceptMIMEType type : acceptMIMETypes) {
            Set<Router> set = map.entrySet().parallelStream().filter(e -> {
                String[] t = StringUtils.split(e.getKey(), '/');
                String p = t[0].trim();
                String c = t[1].trim();
                switch (type.getMatchType()) {
                    case EXACT:
                        return p.equals(type.getParentType()) && c.equals(type.getChildType());
                    case CHILD:
                        return c.equals(type.getChildType());
                    case PARENT:
                        return p.equals(type.getParentType());
                    case ALL:
                        return true;
                    default:
                        return false;
                }
            }).map(Map.Entry::getValue).flatMap(Collection::stream).collect(Collectors.toSet());
            if (!CollectionUtils.isEmpty(set)) {
                return new MatchResult(set, Collections.emptyMap(), getMatchType());
            }
        }
        return null;
    }
}

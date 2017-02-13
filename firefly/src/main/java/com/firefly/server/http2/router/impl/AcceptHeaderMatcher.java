package com.firefly.server.http2.router.impl;

import com.firefly.codec.http2.model.MimeTypes;
import com.firefly.server.http2.router.Router;
import com.firefly.utils.StringUtils;

import java.util.*;

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

        List<String> acceptList = MimeTypes.getAcceptMIMETypes(value);
        if (acceptList == null || acceptList.isEmpty()) {
            return null;
        } else {
            List<MatchResult> retList = new ArrayList<>();
            acceptList.forEach(s -> {
                String[] t = StringUtils.split(s, '/');
                String parentType = t[0].trim();
                String childType = t[1].trim();
                if (parentType.equals("*")) {
                    if (!childType.equals("*")) {
                        Set<Router> set = new HashSet<>();
                        map.entrySet()
                           .stream()
                           .filter(e -> StringUtils.split(e.getKey(), '/')[1].trim().equals(childType))
                           .map(Map.Entry::getValue)
                           .forEach(set::addAll);
                        if (!set.isEmpty()) {
                            retList.add(new MatchResult(set, Collections.emptyMap(), getMatchType()));
                        }
                    }
                } else {
                    if (!childType.equals("*")) {
                        MatchResult r = super.match(s);
                        if (r != null) {
                            retList.add(r);
                        }
                    } else {
                        Set<Router> set = new HashSet<>();
                        map.entrySet()
                           .stream()
                           .filter(e -> StringUtils.split(e.getKey(), '/')[0].trim().equals(parentType))
                           .map(Map.Entry::getValue)
                           .forEach(set::addAll);
                        if (!set.isEmpty()) {
                            retList.add(new MatchResult(set, Collections.emptyMap(), getMatchType()));
                        }
                    }
                }
            });
            if (retList.isEmpty()) {
                return null;
            } else {
                Set<Router> routers = new HashSet<>();
                retList.forEach(e -> routers.addAll(e.getRouters()));
                return new MatchResult(routers, Collections.emptyMap(), getMatchType());
            }
        }
    }
}

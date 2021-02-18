package com.fireflysource.net.websocket.common.extension.compress;

/**
 * Implementation of the <a href="https://tools.ietf.org/id/draft-tyoshino-hybi-websocket-perframe-deflate-05.txt">x-webkit-deflate-frame</a> extension seen out
 * in the wild. Using the alternate extension identification
 */
public class XWebkitDeflateFrameExtension extends DeflateFrameExtension {
    @Override
    public String getName() {
        return "x-webkit-deflate-frame";
    }
}

package com.fireflysource.net.http.common.v2.frame;

import java.util.HashMap;
import java.util.Map;

public class SettingsFrame extends Frame {

    public static final int DEFAULT_MAX_KEYS = 64;

    public static final int HEADER_TABLE_SIZE = 1;
    public static final int ENABLE_PUSH = 2;
    public static final int MAX_CONCURRENT_STREAMS = 3;
    public static final int INITIAL_WINDOW_SIZE = 4;
    public static final int MAX_FRAME_SIZE = 5;
    public static final int MAX_HEADER_LIST_SIZE = 6;

    public static final SettingsFrame DEFAULT_SETTINGS_FRAME;

    static {
        Map<Integer, Integer> settings = new HashMap<>();
        settings.put(HEADER_TABLE_SIZE, 4096);
        settings.put(ENABLE_PUSH, 1);
        settings.put(INITIAL_WINDOW_SIZE, 65535);
        settings.put(MAX_FRAME_SIZE, 16384);
        DEFAULT_SETTINGS_FRAME = new SettingsFrame(settings, false);
    }

    private final Map<Integer, Integer> settings;
    private final boolean reply;

    public SettingsFrame(Map<Integer, Integer> settings, boolean reply) {
        super(FrameType.SETTINGS);
        this.settings = settings;
        this.reply = reply;
    }

    public Map<Integer, Integer> getSettings() {
        return settings;
    }

    public boolean isReply() {
        return reply;
    }

    @Override
    public String toString() {
        return String.format("%s,reply=%b:%s", super.toString(), reply, settings);
    }
}

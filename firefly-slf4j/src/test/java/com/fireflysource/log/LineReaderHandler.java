package com.fireflysource.log;

@FunctionalInterface
public interface LineReaderHandler {
    void readline(String text, int num);
}

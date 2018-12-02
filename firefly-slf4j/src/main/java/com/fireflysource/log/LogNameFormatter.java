package com.fireflysource.log;

import java.time.LocalDateTime;

/**
 * @author Pengtao Qiu
 */
public interface LogNameFormatter {

    String format(String name, LocalDateTime localDateTime);

    String formatBak(String name, LocalDateTime localDateTime, int index);

}

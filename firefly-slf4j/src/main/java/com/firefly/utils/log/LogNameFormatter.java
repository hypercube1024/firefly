package com.firefly.utils.log;

import java.time.LocalDate;

/**
 * @author Pengtao Qiu
 */
public interface LogNameFormatter {

    String format(String name, LocalDate localDate);

    String formatBak(String name, LocalDate localDate, int index);

}

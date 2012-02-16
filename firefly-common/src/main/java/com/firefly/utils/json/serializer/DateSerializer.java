package com.firefly.utils.json.serializer;

import static com.firefly.utils.json.JsonStringSymbol.QUOTE;

import java.util.Date;

import com.firefly.utils.json.Serializer;
import com.firefly.utils.json.support.JsonStringWriter;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class DateSerializer implements Serializer {

	@Override
	public void convertTo(JsonStringWriter writer, Object obj) {
		writer.write(QUOTE + SafeSimpleDateFormat.defaultDateFormat.format((Date) obj) + QUOTE);
	}

}

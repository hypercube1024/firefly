package com.firefly.utils.json.serializer;

import static com.firefly.utils.json.JsonStringSymbol.QUOTE;

import java.io.IOException;
import java.util.Date;

import com.firefly.utils.json.JsonWriter;
import com.firefly.utils.json.Serializer;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class DateSerializer implements Serializer {

	@Override
	public void convertTo(JsonWriter writer, Object obj) throws IOException {
		writer.write(QUOTE + SafeSimpleDateFormat.defaultDateFormat.format((Date) obj) + QUOTE);
	}

}

package com.firefly.template.function;

import java.io.OutputStream;
import java.util.Date;

import com.firefly.template.Function;
import com.firefly.template.Model;
import com.firefly.utils.time.SafeSimpleDateFormat;

public class DateFormatFunction implements Function {
	
	private String charset;

	public DateFormatFunction(String charset) {
		this.charset = charset;
	}

	@Override
	public void render(Model model, OutputStream out, Object... obj) throws Throwable{
		Date date = (Date)obj[0];
		if(date != null)
			out.write(SafeSimpleDateFormat.defaultDateFormat.format(date).getBytes(charset));
	}

}

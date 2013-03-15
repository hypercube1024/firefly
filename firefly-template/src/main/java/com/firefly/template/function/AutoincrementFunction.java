package com.firefly.template.function;

import java.io.OutputStream;

import com.firefly.template.Function;
import com.firefly.template.Model;

public class AutoincrementFunction implements Function {
	
	private String charset;
	
	public AutoincrementFunction(String charset) {
		this.charset = charset;
	}

	@Override
	public void render(Model model, OutputStream out, Object... obj) throws Throwable {
		String key = (String)obj[0];
		Integer i = (Integer)model.get(key);
		i = (i == null ? 0 : i + 1);
		model.put(key, i);
		
		if(obj.length == 2) {
			boolean print = (Boolean)obj[1];
			if(print)
				out.write(String.valueOf(i).getBytes(charset));
		}
	}

}

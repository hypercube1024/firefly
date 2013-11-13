package com.firefly.template.function;

import java.io.OutputStream;

import com.firefly.template.Function;
import com.firefly.template.Model;

public class ModuloOutputFunction implements Function {
	
	private String charset;

	public ModuloOutputFunction(String charset) {
		super();
		this.charset = charset;
	}

	@Override
	public void render(Model model, OutputStream out, Object... obj) throws Throwable {
		String key = (String)obj[0];
		String output = (String)obj[1];
		Integer mod = (Integer)obj[2];

		Integer i = (Integer)model.get(key);
		i = (i == null ? 0 : i + 1);
		model.put(key, i);
		
		if( (i % mod == 0) == (obj.length > 3 && (Boolean)obj[3]) ) {
			out.write(output.getBytes(charset));
		}
	}

}

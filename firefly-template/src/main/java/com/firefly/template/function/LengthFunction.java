package com.firefly.template.function;

import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Collection;

import com.firefly.template.Function;
import com.firefly.template.Model;

public class LengthFunction implements Function {

	private String charset;
	
	public LengthFunction(String charset) {
		this.charset = charset;
	}

	@Override
	public void render(Model model, OutputStream out, Object... obj)
			throws Throwable {
		Object o = obj[0];
		if(o != null) {
			if(o instanceof String)
				out.write(String.valueOf(((String)o).length()).getBytes(charset));
			else if(o instanceof Collection)
				out.write(String.valueOf(((Collection<?>)o).size()).getBytes(charset));
			else if(o.getClass().isArray())
				out.write(String.valueOf(Array.getLength(o)).getBytes(charset));
		}
	}

}

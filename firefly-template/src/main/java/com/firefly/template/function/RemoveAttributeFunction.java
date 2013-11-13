package com.firefly.template.function;

import java.io.OutputStream;

import com.firefly.template.Function;
import com.firefly.template.Model;

public class RemoveAttributeFunction implements Function {

	@Override
	public void render(Model model, OutputStream out, Object... obj) throws Throwable {
		if(obj != null)
			for(Object key : obj)
				model.remove((String)key);
	}

}

package com.firefly.template;

import java.io.OutputStream;

public interface Function {
	void render(Model model, OutputStream out, Object... obj) throws Throwable;
}

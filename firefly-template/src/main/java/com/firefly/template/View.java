package com.firefly.template;

import java.io.OutputStream;

public interface View {
	void render(Model model, OutputStream out);
}

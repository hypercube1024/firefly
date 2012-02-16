package com.firefly.template.view;

import java.io.OutputStream;

import com.firefly.template.Config;
import com.firefly.template.Model;
import com.firefly.template.TemplateFactory;
import com.firefly.template.View;

public abstract class AbstractView implements View {
	
	protected TemplateFactory templateFactory;

	@Override
	public void render(Model model, OutputStream out) {
		try {
			main(model, out);
		} catch (Throwable t) {
			Config.LOG.error("view render error", t);
		}
	}

	abstract protected void main(Model model, OutputStream out) throws Throwable;

}

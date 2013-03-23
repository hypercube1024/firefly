package com.firefly.template.function;

import java.io.OutputStream;

import com.firefly.template.Function;
import com.firefly.template.Model;

public class CutStringFunction implements Function {
	
	private String charset;
	
	public CutStringFunction(String charset) {
		this.charset = charset;
	}

	@Override
	public void render(Model model, OutputStream out, Object... obj) throws Throwable {
		String str = (String)obj[0];
		
		switch (obj.length) {
		case 2:
			Integer len2 = (Integer)obj[1];
			out.write((str.substring(0, len2)).getBytes(charset));
			break;
		
		case 3:
			if(obj[2] instanceof String) {
				Integer len3 = (Integer)obj[1];
				String padding3 = (String)obj[2];
				out.write((str.substring(0, len3) + padding3).getBytes(charset));
			} else if(obj[2] instanceof Integer) {
				Integer start3 = (Integer)obj[1];
				Integer len3 = (Integer)obj[2];
				out.write((str.substring(start3, start3 + len3)).getBytes(charset));
			}
			break;

		case 4:
			Integer start4 = (Integer)obj[1];
			Integer len4 = (Integer)obj[2];
			String padding4 = (String)obj[3];
			out.write((str.substring(start4, start4 + len4) + padding4).getBytes(charset));
			break;

		default:
			break;
		}

	}

}

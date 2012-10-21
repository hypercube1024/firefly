package com.firefly.mvc.web.support;

import java.util.ArrayList;
import java.util.List;

public abstract class URLParser {
	
	public static List<String> parse(String uri) {
		List<String> ret = new ArrayList<String>();
		int start = 1;
		int max = uri.length() - 1;
		
		for (int i = 1; i <= max; i++) {
			if(uri.charAt(i) == '/') {
				ret.add(uri.substring(start, i));
				start = i + 1;
			}
		}
		
		if(uri.charAt(max) != '/') 
			ret.add(uri.substring(start));
		return ret;
	}
}

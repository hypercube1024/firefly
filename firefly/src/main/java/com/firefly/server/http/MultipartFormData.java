package com.firefly.server.http;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Part;

public class MultipartFormData {

	private final Collection<Part> parts;
	private Map<String, Part> partMap;
	
	public MultipartFormData(Collection<Part> parts) {
		this.parts = parts;
	}

	public Collection<Part> getParts() throws IOException, ServletException {
		return parts;
	}

	public Part getPart(String name) throws IOException, ServletException {
		if(partMap != null)
			return partMap.get(name);
		
		partMap = new HashMap<String, Part>();
		for(Part part : parts)
			partMap.put(part.getName(), part);
		
		return partMap.get(name);
	}
}

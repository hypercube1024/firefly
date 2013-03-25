package com.firefly.server.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.Part;

import com.firefly.server.io.PipedStream;
import com.firefly.utils.StringUtils;

public class PartImpl implements Part {
	
	private PipedStream pipedStream;
	private String name;
	private boolean parseName = false;
	final Map<String, String> headMap = new HashMap<String, String>();
	int size = 0;

	public PartImpl(PipedStream pipedStream) {
		this.pipedStream = pipedStream;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return pipedStream.getInputStream();
	}

	@Override
	public String getContentType() {
		return headMap.get("content-type");
	}

	@Override
	public String getName() {
		if(!parseName) {
			String contentDisposition = headMap.get("content-disposition");
			String[] t = StringUtils.split(contentDisposition, ';');
			if(t.length < 2)
				return null;
			
			String _name = t[1].trim();
			if(_name.length() < 6)
				return null;

			_name = _name.substring(6, _name.length() - 1);
			name = _name;
		}
		return name;
	}

	@Override
	public long getSize() {
		return size;
	}

	@Override
	public void write(String fileName) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void delete() throws IOException {
		pipedStream.close();
	}

	@Override
	public String getHeader(String name) {
		return headMap.get(name);
	}

	@Override
	public Collection<String> getHeaders(String name) {
		String value = getHeader(name);
		String[] values = StringUtils.split(value, ',');
		return Arrays.asList(values);
	}

	@Override
	public Collection<String> getHeaderNames() {
		return headMap.keySet();
	}
	
	OutputStream getOutputStream() throws IOException {
		return pipedStream.getOutputStream();
	}

}

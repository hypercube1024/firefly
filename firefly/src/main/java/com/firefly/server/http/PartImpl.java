package com.firefly.server.http;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.Part;

import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.io.ByteArrayPipedStream;
import com.firefly.utils.io.FilePipedStream;
import com.firefly.utils.io.FileUtils;
import com.firefly.utils.io.PipedStream;

public class PartImpl implements Part {
	
	public static String tempdir;
	
	private PipedStream pipedStream;
	private String name, fileName;
	private boolean parseName = false;
	private File temp;
	final Map<String, String> headMap = new HashMap<String, String>();
	int size = 0;

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
		parseName();
		return name;
	}
	
	public String getFileName() {
		parseName();
		return fileName;
	}
	
	private void parseName() {
		if(!parseName) {
			String contentDisposition = headMap.get("content-disposition");
			String[] t = StringUtils.split(contentDisposition, ';');
			
			String _name = t[1].trim();
			name = _name.substring(6, _name.length() - 1);
			
			if(t.length == 3) {
				String _filename = t[2].trim();
				fileName = _filename.substring(10, _filename.length() - 1);
			}
			parseName = true;
		}
	}

	@Override
	public long getSize() {
		return size;
	}

	@Override
	public void write(String fileName) throws IOException {
		if(size <= 0)
			return;
		
		if(temp != null) {
			FileUtils.copy(temp, new File(fileName));
		} else {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(fileName)));
			InputStream input = getInputStream();
			
			try {
				byte[] buf = new byte[512];
				for (int len = 0; ((len = input.read(buf) ) != -1); ) {
					out.write(buf, 0, len);
					buf = new byte[512];
				}
			} finally {
				input.close();
				out.close();
			}
			
		}
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
		if(pipedStream != null)
			return pipedStream.getOutputStream();
		
		parseName();
		if(VerifyUtils.isNotEmpty(fileName)) {
			temp = new File(tempdir, "part-" + UUID.randomUUID().toString());
			pipedStream = new FilePipedStream(temp);
		} else {
			pipedStream = new ByteArrayPipedStream(512);
		}
		
		return pipedStream.getOutputStream();
	}

	@Override
	public String getSubmittedFileName() {
		// TODO Auto-generated method stub
		return null;
	}

}

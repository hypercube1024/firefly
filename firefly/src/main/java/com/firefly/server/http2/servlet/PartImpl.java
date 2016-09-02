package com.firefly.server.http2.servlet;

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

import com.firefly.server.exception.HttpServerException;
import com.firefly.utils.StringUtils;
import com.firefly.utils.VerifyUtils;
import com.firefly.utils.io.ByteArrayPipedStream;
import com.firefly.utils.io.FilePipedStream;
import com.firefly.utils.io.FileUtils;
import com.firefly.utils.io.PipedStream;

public class PartImpl implements Part {

	private final ServerHTTP2Configuration http2Configuration;

	private PipedStream pipedStream;
	private String name, fileName;
	private boolean parseName = false;
	private File temp;
	final Map<String, String> headMap = new HashMap<String, String>();
	int size = 0;

	public PartImpl(ServerHTTP2Configuration http2Configuration) {
		this.http2Configuration = http2Configuration;
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
		parseName();
		return name;
	}

	@Override
	public String getSubmittedFileName() {
		parseName();
		return fileName;
	}

	private void parseName() {
		if (!parseName) {
			String contentDisposition = headMap.get("content-disposition");
			String[] arr = StringUtils.split(contentDisposition, ';');

			if (arr == null)
				throw new HttpServerException("the Content-Disposition format exception");

			for (String s : arr) {
				if ("form-data".equalsIgnoreCase(s.trim())) {
					continue;
				} else {
					String[] arr2 = StringUtils.split(s, '=');
					String key = arr2[0].trim();
					String value = arr2[1].trim();
					if (value.length() > 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
						value = value.substring(1, value.length() - 1);
					}
					if (arr2 != null && arr2.length > 1) {
						if ("name".equalsIgnoreCase(key)) {
							name = value;
						} else if ("filename".equalsIgnoreCase(key)) {
							fileName = value;
						}
					}
				}
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
		if (size <= 0)
			return;
		
		if(fileName.contains("../")) {
			throw new IOException("the file name is illegal, it must not contain ../");
		}

		if (temp != null) {
			FileUtils.copy(temp, new File(fileName));
		} else {
			BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(new File(fileName)));
			InputStream input = getInputStream();

			try {
				byte[] buf = new byte[512];
				for (int len = 0; ((len = input.read(buf)) != -1);) {
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
		if (pipedStream != null)
			return pipedStream.getOutputStream();

		parseName();
		if (VerifyUtils.isNotEmpty(fileName)) {
			temp = new File(http2Configuration.getTemporaryDirectory(), "part-" + UUID.randomUUID().toString());
			pipedStream = new FilePipedStream(temp);
		} else {
			pipedStream = new ByteArrayPipedStream(512);
		}

		return pipedStream.getOutputStream();
	}

}

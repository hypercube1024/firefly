package com.firefly.server.io;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;

public class FilePipedStream implements PipedStream {
	
	private static Log log = LogFactory.getInstance().getLog("firefly-system");
	
	private OutputStream out;
	private InputStream in;
	private File temp;
	
	public FilePipedStream(String tempdir) {
		temp = new File(tempdir, UUID.randomUUID().toString());
	}
	
	public FilePipedStream(File file) {
		temp = file;
	}

	@Override
	public void close() throws IOException {
		if(temp == null)
			return;
		
		try {
			temp.delete();
		} finally {
			if(in != null)
				in.close();
			
			if(out != null)
				out.close();
		}
		log.info("temp file [{}] piped stream close!", temp.getName());
		in = null;
		out = null;
		temp = null;
	}

	@Override
	public InputStream getInputStream() throws IOException{
		if(in == null) {
			in = new BufferedInputStream(new FileInputStream(temp));
		}
		return in;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		if(out == null) {
			out = new BufferedOutputStream(new FileOutputStream(temp));
		}
		return out;
	}
}

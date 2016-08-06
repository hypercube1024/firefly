package com.firefly.net.tcp.ssl;

import java.io.File;
import java.io.FileInputStream;

import javax.net.ssl.SSLContext;

import com.firefly.net.tcp.ssl.AbstractSSLContextFactory;

public class FileSSLContextFactory extends AbstractSSLContextFactory {

	private File file;
	private String keystorePassword;
	private String keyPassword;

	public FileSSLContextFactory(String path, String keystorePassword, String keyPassword) {
		file = new File(path);
		this.keystorePassword = keystorePassword;
		this.keyPassword = keyPassword;
	}

	@Override
	public SSLContext getSSLContext() {
		SSLContext ret = null;
		try (FileInputStream in = new FileInputStream(file)) {
			ret = getSSLContext(in, keystorePassword, keyPassword);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

}

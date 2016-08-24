package com.firefly.net.tcp.ssl;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.firefly.net.SSLContextFactory;
import com.firefly.utils.log.Log;
import com.firefly.utils.log.LogFactory;
import com.firefly.utils.time.Millisecond100Clock;

public abstract class AbstractSSLContextFactory implements SSLContextFactory {

	protected static final Log log = LogFactory.getInstance().getLog("firefly-system");

	public SSLContext getSSLContextWithManager(KeyManager[] km, TrustManager[] tm, SecureRandom random)
			throws NoSuchAlgorithmException, KeyManagementException {
		long start = Millisecond100Clock.currentTimeMillis();
		final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
		sslContext.init(km, tm, random);
		_handle_BUG_JDK_8022063(sslContext);
		long end = Millisecond100Clock.currentTimeMillis();
		log.info("creating SSL context spends {} ms", (end - start));
		return sslContext;
	}

	public SSLContext getSSLContext(InputStream in, String keystorePassword, String keyPassword)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
			UnrecoverableKeyException, KeyManagementException {
		return getSSLContext(in, keystorePassword, keyPassword, null, null, null);
	}

	public SSLContext getSSLContext(InputStream in, String keystorePassword, String keyPassword,
			String keyManagerFactoryType, String trustManagerFactoryType, String sslProtocol)
			throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
			UnrecoverableKeyException, KeyManagementException {
		long start = Millisecond100Clock.currentTimeMillis();
		final SSLContext sslContext;

		KeyStore ks = KeyStore.getInstance("JKS");
		ks.load(in, keystorePassword != null ? keystorePassword.toCharArray() : null);

		// PKIX,SunX509
		KeyManagerFactory kmf = KeyManagerFactory
				.getInstance(keyManagerFactoryType == null ? "SunX509" : keyManagerFactoryType);
		kmf.init(ks, keyPassword != null ? keyPassword.toCharArray() : null);

		TrustManagerFactory tmf = TrustManagerFactory
				.getInstance(trustManagerFactoryType == null ? "SunX509" : trustManagerFactoryType);
		tmf.init(ks);

		// TLSv1 TLSv1.2
		sslContext = SSLContext.getInstance(sslProtocol == null ? "TLSv1.2" : sslProtocol);
		sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		_handle_BUG_JDK_8022063(sslContext);
		long end = Millisecond100Clock.currentTimeMillis();
		log.info("creating SSL context spends time in {} ms", (end - start));
		return sslContext;
	}

	protected void _handle_BUG_JDK_8022063(SSLContext sslContext) {
		// TODO The bug JDK-8022063, the first createSSLEngine takes 5+ seconds
		// to complete in OS X. Once createSSLEngine is invoked one time, the
		// next invocation will be fast.
		SSLEngine sslEngine = sslContext.createSSLEngine();
		sslEngine.closeOutbound();
	}

}

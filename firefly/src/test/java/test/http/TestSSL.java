package test.http;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import com.firefly.net.tcp.ssl.DefaultSSLContextFactory;
import com.firefly.utils.json.Json;

public class TestSSL {

	public static void main(String[] args) {
		DefaultSSLContextFactory contextFactory = new DefaultSSLContextFactory();
		SSLContext context = contextFactory.getSSLContext();
		SSLParameters enabled = context.getDefaultSSLParameters();
        SSLParameters supported = context.getSupportedSSLParameters();
        System.out.println(Json.toJson(enabled.getCipherSuites()));
        System.out.println(Json.toJson(enabled.getProtocols()));
        System.out.println(Json.toJson(supported.getCipherSuites()));
        System.out.println(Json.toJson(supported.getProtocols()));
	}

}

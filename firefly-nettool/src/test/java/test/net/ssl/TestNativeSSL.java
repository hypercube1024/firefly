package test.net.ssl;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLException;
import java.security.cert.CertificateException;

/**
 * @author Pengtao Qiu
 */
public class TestNativeSSL {

    public static void main(String[] args) throws CertificateException, SSLException {
//        SslContext sslCtx = SslContext.newServerContext(SslProvider.OPENSSL);
        SelfSignedCertificate ssc = new SelfSignedCertificate();
        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                                             .build();
        System.out.println(SslContext.defaultServerProvider());
        sslCtx.newEngine(PooledByteBufAllocator.DEFAULT);
        sslCtx.newHandler(PooledByteBufAllocator.DEFAULT);

    }
}

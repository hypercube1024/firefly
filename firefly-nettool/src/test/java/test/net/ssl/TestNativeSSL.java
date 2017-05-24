package test.net.ssl;

import com.firefly.utils.io.FileUtils;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.security.cert.CertificateException;

/**
 * @author Pengtao Qiu
 */
public class TestNativeSSL {

    public static void main(String[] args) throws CertificateException, IOException {
//        SslContext sslCtx = SslContext.newServerContext(SslProvider.OPENSSL);
        SelfSignedCertificate ssc = new SelfSignedCertificate("www.fireflysource.com");
        System.out.println(ssc.certificate().getAbsolutePath());
        System.out.println(FileUtils.readFileToString(ssc.certificate(), "UTF-8"));
        System.out.println();
        System.out.println(ssc.privateKey().getAbsolutePath());
        System.out.println(FileUtils.readFileToString(ssc.privateKey(), "UTF-8"));

        SslContext sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey())
                                             .build();
        System.out.println(SslContext.defaultServerProvider());

        SSLEngine sslEngine = sslCtx.newEngine(PooledByteBufAllocator.DEFAULT);

        sslCtx.newHandler(PooledByteBufAllocator.DEFAULT);

    }
}

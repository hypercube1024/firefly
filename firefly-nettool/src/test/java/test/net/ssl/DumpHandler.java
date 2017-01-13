package test.net.ssl;

import com.firefly.net.Handler;
import com.firefly.net.SSLContextFactory;
import com.firefly.net.Session;
import com.firefly.net.tcp.ssl.DefaultCredentialSSLContextFactory;
import com.firefly.net.tcp.ssl.SSLSession;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.RandomAccessFile;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Arrays;

public class DumpHandler implements Handler {

    private SSLContextFactory sslContextFactory = new DefaultCredentialSSLContextFactory();


    public DumpHandler() throws Throwable {
    }

    @Override
    public void sessionOpened(Session session) throws Throwable {
        SessionInfo info = new SessionInfo();
        info.sslSession = new SSLSession(sslContextFactory, false, session, session1 -> {
        });
        session.attachObject(info);
    }

    @Override
    public void sessionClosed(Session session) throws Throwable {
        System.out.println("session close: " + session.getSessionId());
    }

    @Override
    public void messageReceived(Session session, Object message)
            throws Throwable {
//		StringBuilder s = new StringBuilder(20000);
//		s.append("hello world!\r\n ----");
//		for (int i = 0; i < 20000; i++) {
//			s.append("c");
//		}
//		s.append("----");
//		session.encode(s.toString());


        File file = new File(DumpHandler.class.getResource("/index.html").toURI());
        SessionInfo info = (SessionInfo) session.getAttachment();
        info.length = file.length();
        @SuppressWarnings("resource")
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        session.encode(raf.getChannel());
    }

    @Override
    public void exceptionCaught(Session session, Throwable t) throws Throwable {
        t.printStackTrace();
    }

    public static void main(String[] args) throws Throwable {
        Certificate[] certificates = getCertificates("fireflySSLkeys.jks", "fireflySSLkeys");
        for (Certificate certificate : certificates) {
            System.out.println(certificate);
        }
        certificates = getCertificates("fireflykeys", "fireflysource");
        for (Certificate certificate : certificates) {
            System.out.println(certificate);
        }
    }

    public static Certificate[] getCertificates(String jks, String alias) throws Throwable {
        try (FileInputStream in = new FileInputStream(new File("/Users/qiupengtao", jks))) {
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(in, "ptmima1234".toCharArray());
            return keyStore.getCertificateChain(alias);
        }
    }

    public static void main2(String[] args) throws Throwable {
        try (FileInputStream in = new FileInputStream(new File("/Users/qiupengtao", "fireflykeys"));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[1024];

            for (int i = 0; (i = in.read(buf)) != -1; ) {
                byte[] temp = new byte[i];
                System.arraycopy(buf, 0, temp, 0, i);
                out.write(temp);
            }

            byte[] ret = out.toByteArray();
//			System.out.println(ret.length);
            System.out.println(Arrays.toString(ret));
        }
    }

}

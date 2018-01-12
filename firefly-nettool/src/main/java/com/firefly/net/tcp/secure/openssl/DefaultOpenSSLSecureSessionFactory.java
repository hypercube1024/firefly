package com.firefly.net.tcp.secure.openssl;

import com.firefly.net.tcp.secure.openssl.nativelib.*;
import com.firefly.utils.exception.CommonRuntimeException;

import javax.net.ssl.SSLException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;

/**
 * @author Pengtao Qiu
 */
public class DefaultOpenSSLSecureSessionFactory extends AbstractOpenSSLSecureSessionFactory {

    private static final byte[] privateKey = ("-----BEGIN PRIVATE KEY-----\r\n" +
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDnP/lbvPGx7uRS\r\n" +
            "3AaHeVyyl68qb48AKQ4EN40FrE6Y6iBlKATVMp5dS17B6+WC+05ICdFMxcW2LFgA\r\n" +
            "z7WznBvGxr0/Tv7ngbjP2z90Pv/Xkwo6klnpY07zp297pm9pTJvysK2L3CVwlc4C\r\n" +
            "1vxsisluJLZnYn++cZGF3Hug+90e/rwJrgp5itppD3WyMCN242NnxtRBYliAsRKd\r\n" +
            "j2SypLdhWR1/OLlzqap2ztaaBhKLn5c3VH+UgxwWxkZwauDQGIoKQIgejMSxTL2i\r\n" +
            "RFtAflDpl0npSABZ0ExBPJGoH/kmF8cO3fm+4SiyJcLan2dF7ubZGEDexf6zO6pg\r\n" +
            "qVmr4vVrAgMBAAECggEAECoLnxr89ggR06znk+6qyR0LNHcp0sQL48WSSPQ7Zjrv\r\n" +
            "WsLKW7C3GyRakkmP+HDijuyIwcoNQOemmx/pvo1J78ISlmtKLBqINZdIvzJsJcB0\r\n" +
            "dZWnTUYQzb1FcKo4nW6qc/NfnigcQdtm1BH9AQVOgTF1wpJDBafgmS/JQH56fWEE\r\n" +
            "8GmLjxT57ActAJHex24I38xkEJouAXSdP5bra7y9op6XOIVkCIq21bioSW9q60cj\r\n" +
            "rmwArpW/gnYu/JWPrz9PyJMawRLnDRaUusamHwFhzdvf68N1NzFD4tkigNTDIxjh\r\n" +
            "F7Hn2SzKmBV5NzHvBhuYEaIVglJW77j3XaxrbKFueQKBgQD8Kk8F0bapg5ieLs42\r\n" +
            "DY0Ur7pDP7FAfcFZ5I2i2hfcBId/yuXi3YmCz7ungS2MXaxJbsoFUfNXB5JVtArQ\r\n" +
            "il66iJ3mxoEi/hEfgXmcgNIoz7QCrDbYUJNubRXzuxx3oL+OEir9iS8wHSq418tj\r\n" +
            "+x0ehjGNeuA7b5SKeD/0WFXinwKBgQDqxD2oW5MWS7mCMbBPzf4pu9WV0BwhsxnR\r\n" +
            "aV08siZKcBX4SNOPyVQa4xDWIEt0pjUUlWz3Lnm/H8Lrfu3hLHeIGCJqkZF25O70\r\n" +
            "1quDJBDsoM6rdPwW9JcOqgOPAcmOmvhttWR+v3eHwttWMrYou0cD4eWv2NAtcEC9\r\n" +
            "9xCl7J9ltQKBgAJsjbHqoCzk/PSYUpcmbjLCtbGrzRJnoxIwJkRogE43JO5PjwIt\r\n" +
            "SwMc6BrmHP/6bouOKxb+AK7XfcqhHnF+wwZcBdiQE/G4K4X4AqlEe8C0rnBQI37U\r\n" +
            "13X6Snm7VLy5qN2+wvcI+JEn1p9MQ8JESbVfaN6JTdqEahMisMqyhrDVAoGBAOCt\r\n" +
            "2RiEM5u18x9f8I4f5ewJARs0pYe3AbGB9iJ9mNnKtav9/sNwedaVk2pqU+yamNhP\r\n" +
            "PY6+4vOAMSryOjUzOl58QovdLusrpGmb/V8vqCbeq4w6FHzfrqXxHiDCJCUfuTw9\r\n" +
            "yqTnFz1Nxl6Tf/Qxb4COtCR8CphCAyQLDNrPP6m1AoGASYDnzafO3+sWjuHygGc+\r\n" +
            "QThsNe6FC3kKotcsMM0cZwDmJLJZbn5I//bXN7MM82xnGvcds8xBWWE1NGkjlFFE\r\n" +
            "SMHInhXT4ousxaWxz5I8x6MuG5x2gLcuuV3/oEnBjJN7OEPGQUJwfB32vT0Ary2d\r\n" +
            "RSfzyjsQnjRgT0rAskXvpA8=\r\n" +
            "-----END PRIVATE KEY-----").getBytes(StandardCharsets.US_ASCII);

    private static final byte[] cert = ("-----BEGIN CERTIFICATE-----\r\n" +
            "MIIDsjCCApoCCQC1VNfRtuYWMTANBgkqhkiG9w0BAQsFADCBmTELMAkGA1UEBhMC\r\n" +
            "Q04xDjAMBgNVBAgMBUh1YmVpMQ4wDAYDVQQHDAVXdWhhbjEWMBQGA1UECgwNZmly\r\n" +
            "ZWZseXNvdXJjZTEUMBIGA1UECwwLZGV2ZWxvcG1lbnQxHjAcBgNVBAMMFXd3dy5m\r\n" +
            "aXJlZmx5c291cmNlLmNvbTEcMBoGCSqGSIb3DQEJARYNcXB0a2tAMTYzLmNvbTAg\r\n" +
            "Fw0xNzExMTExNDU5NDBaGA8yMTE3MTAxODE0NTk0MFowgZkxCzAJBgNVBAYTAkNO\r\n" +
            "MQ4wDAYDVQQIDAVIdWJlaTEOMAwGA1UEBwwFV3VoYW4xFjAUBgNVBAoMDWZpcmVm\r\n" +
            "bHlzb3VyY2UxFDASBgNVBAsMC2RldmVsb3BtZW50MR4wHAYDVQQDDBV3d3cuZmly\r\n" +
            "ZWZseXNvdXJjZS5jb20xHDAaBgkqhkiG9w0BCQEWDXFwdGtrQDE2My5jb20wggEi\r\n" +
            "MA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDnP/lbvPGx7uRS3AaHeVyyl68q\r\n" +
            "b48AKQ4EN40FrE6Y6iBlKATVMp5dS17B6+WC+05ICdFMxcW2LFgAz7WznBvGxr0/\r\n" +
            "Tv7ngbjP2z90Pv/Xkwo6klnpY07zp297pm9pTJvysK2L3CVwlc4C1vxsisluJLZn\r\n" +
            "Yn++cZGF3Hug+90e/rwJrgp5itppD3WyMCN242NnxtRBYliAsRKdj2SypLdhWR1/\r\n" +
            "OLlzqap2ztaaBhKLn5c3VH+UgxwWxkZwauDQGIoKQIgejMSxTL2iRFtAflDpl0np\r\n" +
            "SABZ0ExBPJGoH/kmF8cO3fm+4SiyJcLan2dF7ubZGEDexf6zO6pgqVmr4vVrAgMB\r\n" +
            "AAEwDQYJKoZIhvcNAQELBQADggEBAEAVWv+uZ9MjdKR/0zPJ9OnReYyPTHG+WyuK\r\n" +
            "Ep8k0FT0SZSwcRk5phOC/eykqdf5VXIAqj4/N+m9UHyJsog+k0J5Jv04LvOhMLfG\r\n" +
            "a8Jm/gG0vzMxGcL1BiFr6yftYyQVgI9vqgEvySojMGwKHgkUcpEiaAD3AWdc7Io/\r\n" +
            "sbgAt1y1y47y7+NWJToNShYPCl6nVVz7AISQ32bW1Ph5u11c0dUEVb5qlxqCw1Ai\r\n" +
            "tkrGzhmDyHglAPspUeF2WXcXRi6i23nF0NictSv3PrRdDa7X7K/UBtIgoUaSrssf\r\n" +
            "U8x8YQXwX+Q8SfGpcmeh2LfC2iwYxV/NPr5stNAxrnpivrsBB88=\r\n" +
            "-----END CERTIFICATE-----").getBytes(StandardCharsets.US_ASCII);

    @Override
    public SslContext createSSLContext(boolean clientMode) {
        SslContextBuilder sslContextBuilder = clientMode
                ? SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)
                : SslContextBuilder.forServer(new ByteArrayInputStream(cert), new ByteArrayInputStream(privateKey));

        try {
            return sslContextBuilder.ciphers(SecurityUtils.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
                                    .applicationProtocolConfig(new ApplicationProtocolConfig(ApplicationProtocolConfig.Protocol.ALPN,
                                            ApplicationProtocolConfig.SelectorFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.CHOOSE_MY_LAST_PROTOCOL,
                                            supportedProtocols)).build();
        } catch (SSLException e) {
            log.error("create ssl context exception", e);
            throw new CommonRuntimeException(e);
        }
    }

    @Override
    public File getCertificate() {
        return null;
    }

    @Override
    public File getPrivateKey() {
        return null;
    }
}

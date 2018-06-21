package com.firefly.codec.oauth2.as.issuer;

import com.firefly.codec.oauth2.exception.OAuthSystemException;

import java.security.MessageDigest;
import java.util.UUID;


/**
 * Exemplar OAuth Token Generator
 */
public class MD5Generator implements ValueGenerator {

    @Override
    public String generateValue() throws OAuthSystemException {
        return generateValue(UUID.randomUUID().toString());
    }

    private static final char[] hexCode = "0123456789abcdef".toCharArray();

    public static String toHexString(byte[] data) {
        if (data == null) {
            return null;
        }
        StringBuilder r = new StringBuilder(data.length * 2);
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }

    @Override
    public String generateValue(String param) throws OAuthSystemException {
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            algorithm.update(param.getBytes());
            byte[] messageDigest = algorithm.digest();
            return toHexString(messageDigest);
        } catch (Exception e) {
            throw new OAuthSystemException("OAuth Token cannot be generated.", e);
        }
    }
}

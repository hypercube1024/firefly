package com.firefly.net.tcp.secure.openssl;

import io.netty.internal.tcnative.SessionTicketKey;

/**
 * Session Ticket Key
 */
public final class OpenSslSessionTicketKey {

    /**
     * Size of session ticket key name
     */
    public static final int NAME_SIZE = SessionTicketKey.NAME_SIZE;
    /**
     * Size of session ticket key HMAC key
     */
    public static final int HMAC_KEY_SIZE = SessionTicketKey.HMAC_KEY_SIZE;
    /**
     * Size of session ticket key AES key
     */
    public static final int AES_KEY_SIZE = SessionTicketKey.AES_KEY_SIZE;
    /**
     * Size of session ticker key
     */
    public static final int TICKET_KEY_SIZE = SessionTicketKey.TICKET_KEY_SIZE;

    final SessionTicketKey key;

    /**
     * Construct a OpenSslSessionTicketKey.
     *
     * @param name the name of the session ticket key
     * @param hmacKey the HMAC key of the session ticket key
     * @param aesKey the AES key of the session ticket key
     */
    public OpenSslSessionTicketKey(byte[] name, byte[] hmacKey, byte[] aesKey) {
        key = new SessionTicketKey(name.clone(), hmacKey.clone(), aesKey.clone());
    }

    /**
     * Get name.
     * @return the name of the session ticket key
     */
    public byte[] name() {
        return key.getName().clone();
    }

    /**
     * Get HMAC key.
     * @return the HMAC key of the session ticket key
     */
    public byte[] hmacKey() {
        return key.getHmacKey().clone();
    }

    /**
     * Get AES Key.
     * @return the AES key of the session ticket key
     */
    public byte[] aesKey() {
        return key.getAesKey().clone();
    }
}

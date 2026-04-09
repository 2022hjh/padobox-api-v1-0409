package com.pience.padobox.utility;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtil {
	
    private final static String HEX = "0123456789abcdef";
   
    
    public static String getHashStringTo(String value, String algorithm) throws Exception {
	    if (value == null || value.trim().equals(""))
	        throw new Exception("value is null");
	    MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(value.getBytes());
        byte[] hash = md.digest();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            appendHex(sb, hash[i]);
        }
        return sb.toString();
    }

    public static String encrypt(String seed, String value) throws Exception {
        return encrypt(seed.getBytes(), value);
    }

    public static String encrypt(byte[] key, String value) throws Exception {
        byte[] encrypted = encrypt(key, value.getBytes());
        return getHexStringTo(encrypted);
    }

    public static byte[] encrypt(byte[] key, byte[] value) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(value);
        return encrypted;
    }

    public static String decrypt(String seed, String encrypted) throws Exception {
        return decrypt(seed.getBytes(), encrypted);
    }

    public static String decrypt(byte[] key, String encrypted) throws Exception {
        byte[] bytes = getBytesTo(encrypted);
        byte[] decrypted = decrypt(key, bytes);
        return new String(decrypted);
    }

    public static byte[] decrypt(byte[] key, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }

    public static String getStringTo(String hexString) {
        return new String(getBytesTo(hexString));
    }

    public static byte[] getBytesTo(String hexString) {
        int len = hexString.length() / 2;
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
            bytes[i] = Integer.valueOf(hexString.substring(2 * i, 2 * i + 2), 16).byteValue();
        }
        return bytes;
    }

    public static String getHexStringTo(String value) {
        return getHexStringTo(value.getBytes());
    }

    public static String getHexStringTo(byte[] bytes) {
        if (bytes == null)
            return "";
        StringBuffer sb = new StringBuffer(2 * bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            appendHex(sb, bytes[i]);
        }
        return sb.toString();
    }

    public static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b & 0xF0) >> 4)).append(HEX.charAt(b & 0x0F));
    }

    public static byte[] getRandomKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(seed);
        kgen.init(128, sr);
        SecretKey skey = kgen.generateKey();
        byte[] key = skey.getEncoded();
        return key;
    }

	public static String makeUUID(){
		UUID uuid = UUID.randomUUID();
        String trimmed = uuid.toString().trim().replace("-", "");
		return trimmed;
	};
	
    public static byte[] asByteArray(UUID uuid) {
    	long msb = uuid.getMostSignificantBits();
    	long lsb = uuid.getLeastSignificantBits();
    	byte[] buffer = new byte[16];
    	for (int i = 0; i < 8; i++) {
    		buffer[i] = (byte) (msb >>> 8 * (7 - i));
    	}
    	for (int i = 8; i < 16; i++) {
    		buffer[i] = (byte) (lsb >>> 8 * (7 - i));
    	}
    	return buffer;
    }
    

}

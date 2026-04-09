package com.pience.padobox.utility;

import java.security.MessageDigest;
import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.Base64.Decoder;


public class PwUtil {
	
	public static String enc(String plainTxt) {
		String encTxt = null;
		try {
			MessageDigest mDigest = MessageDigest.getInstance("MD5");
			mDigest.update(plainTxt.getBytes());
			byte[] msgStr = mDigest.digest() ;
			Encoder enc = Base64.getEncoder();
			byte[] encodedBytes = enc.encode(msgStr);
			encTxt = new String(encodedBytes);
		}catch(Exception ex) {
			System.out.println("ex.getMessage():"+ex.getMessage());
		}
		return encTxt;
	}
	
	public static String IdPwDec(String txt_val) {
		String txt_dec_val = "";
		byte[] txt_bytes = txt_val.getBytes();
		Decoder decoder = Base64.getDecoder();
        byte[] dec_bytes = decoder.decode(txt_bytes);
        txt_dec_val = new String(dec_bytes);
		return txt_dec_val;
	}
	
}
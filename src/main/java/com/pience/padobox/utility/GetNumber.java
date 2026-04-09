package com.pience.padobox.utility;

import java.util.Random;

public class GetNumber {
	
	public static int generateNumber(int length) {
	    String numStr = "1";
	    String plusNumStr = "1";
	    for (int i = 0; i < length; i++) {
	        numStr += "0";
	 
	        if (i != length - 1) {
	            plusNumStr += "0";
	        }
	    }
	    Random random = new Random();
	    int result = random.nextInt(Integer.parseInt(numStr)) + Integer.parseInt(plusNumStr);
	    if (result > Integer.parseInt(numStr)) {
	        result = result - Integer.parseInt(plusNumStr);
	    }
	    return result;
	}
	
	public static String generateRandomKey(int length) {
		String return_key = "";
		Random rnd =new Random();
		StringBuffer buf =new StringBuffer();
		for (int i = 0; i < length; i++) {
			if(rnd.nextBoolean()){
				buf.append((char)((int)(rnd.nextInt(26))+97));
			}else {
				buf.append((rnd.nextInt(10)));
			}
		}
		return_key = buf.toString();
		return return_key;
	}
	
	public static String getRandomKey(int key_len) {
	    Random rnd=new Random();
	    StringBuffer buf=new StringBuffer();
	    for(int i=1;i<=key_len;i++) {
	        if(rnd.nextBoolean())
	            buf.append((char)(rnd.nextInt(26)+65));
	        else
	            buf.append(rnd.nextInt(10));
	    }
	    return buf.toString();
	}

}

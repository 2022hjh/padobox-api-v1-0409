package com.pience.padobox.utility;

import java.nio.charset.StandardCharsets;
//import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.pience.padobox.model.DefaultDomain.SellerCheck;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
//import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Service
public class JwtUtil {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
	private SecretKey getSignKey(String secret) {
	    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * @desc   토큰 변환
	 */
	public String createToken(SellerCheck seller_check, String secret) {
		Map<String, Object> claims = new HashMap<String, Object>();
		claims.put("sellerId", seller_check.getSellerid());
		logger.info("jwt createToken seller_id:"+seller_check.getSellerid());
		logger.info("jwt createToken secret:"+secret);
		
		Map<String, Object> headers = new HashMap<>();
	    headers.put("alg", "HS256");
	    headers.put("typ", "JWT");
		
        return Jwts.builder()
        		.header() 
                .add("alg", "HS256")  
                .add("typ", "JWT") 
                .and() 
        		.claims(claims)
        		.signWith(getSignKey(secret))
        		.compact();
    }
	
	/**
	 * @desc   토큰 역변
	 */
	public String paserToken(String token, String secret) {
		logger.info("jwt createToken secret:"+secret);
		String seller_id = "";
		try {
			Claims claims = Jwts.parser()
			        .verifyWith(getSignKey(secret))
			        .build()
			        .parseSignedClaims(token)
			        .getPayload();
			seller_id = claims.get("sellerId", String.class);
			logger.info("jwt paserToken seller_id get payload :"+seller_id);
		} catch (Exception e) {
			if (e instanceof SignatureException) {
				logger.info("jwt paserToken SignatureException : "+e);
			} else if (e instanceof ExpiredJwtException) {
				logger.info("jwt paserToken ExpiredJwtException : "+e);
			} else {
				logger.info("jwt paserToken error : "+e);
			}
		}
	    return seller_id;
	}

}

package com.pience.padobox.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.pience.padobox.config.DefaultConfig;
import com.pience.padobox.model.DefaultDomain;
import com.pience.padobox.service.DefaultService;
import com.pience.padobox.utility.JwtUtil;
import com.pience.padobox.utility.CryptoUtil;
import com.pience.padobox.utility.EmptyUtils;
import com.pience.padobox.utility.Json;

@RestController
public class DefaultController {
	
	private final DefaultConfig defaultConfig;
	
    public DefaultController(DefaultConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }
	
	@Autowired
	DefaultService defaultService;
	
	@Autowired
	private JwtUtil jwtUtil;
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	/**
	 * @method GET
	 * @param NO DATA
	 * 	@return Json{message}
	 * @desc  root 오류 리턴
	 */
	@GetMapping("/")
	public @ResponseBody Json  main(Model model) {
		
		return new Json("{\"message\":\"Invalid path\"}");
	}

	/**
	 * @method GET
	 * @param NO DATA
	 * 	@return Json{}
	 * @desc  idx 암호화 체크
	 */
	@GetMapping("/idx")
	public String LastIdxTest(Model model) {
		String header_seed = "padobox2025api00";
		try {
			String last_idx_enc = CryptoUtil.encrypt(header_seed, "24"); 
			logger.info("last_idx_enc: "+last_idx_enc);
			
			String last_idx_val = CryptoUtil.decrypt(header_seed, last_idx_enc);
			logger.info("last_idx_val: "+last_idx_val);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}
	
	/**
	 * @method GET
	 * @param NO DATA
	 * 	@return Json{""}
	 * @desc  sellerId > token 
	 */
	@GetMapping("/token")
	public String token(@RequestParam Map<String, String> paramMap) {
		
		String seller_id = "";
		String token = "";
		String connect_type = "";
		String secret = "";
		
		if(EmptyUtils.isEmpty(paramMap)==false) {
			
			if(EmptyUtils.isEmpty(paramMap.get("connect_type"))==false) {
				connect_type = paramMap.get("connect_type");
				if(connect_type.equals("local")==true) {
					secret = defaultConfig.getPadoboxJwtSecretDev();
				}else if(connect_type.equals("dev")==true) {
					secret = defaultConfig.getPadoboxJwtSecretDev();
				}else if(connect_type.equals("prod")==true) {
					secret = defaultConfig.getPadoboxJwtSecretProd();
				}
			}
			if(EmptyUtils.isEmpty(secret)==false) {
				if(EmptyUtils.isEmpty(paramMap.get("seller_id"))==false) {
					seller_id =  paramMap.get("seller_id");
					DefaultDomain.SellerCheck domainval = new DefaultDomain.SellerCheck();
					domainval.setSellerid(seller_id);
					try {
						token = jwtUtil.createToken(domainval, secret);
						logger.info("token:"+token);
					} catch (Exception e) {
						logger.info("token error e:"+e);
					}
				}
				if(EmptyUtils.isEmpty(paramMap.get("token"))==false) {
					token =  paramMap.get("token");
					try {
						logger.info("token:"+token);
						seller_id = jwtUtil.paserToken(token, secret);
						logger.info("paserToken seller_id:"+seller_id);
					} catch (Exception e) {
						logger.info("seller_id error e:"+e);
					}
				}
			}
			logger.info("paramMap:"+paramMap.toString());
		}
		return "connect_type : "+connect_type+"// sellerId:"+seller_id+"// token:"+token;
	}
	

}

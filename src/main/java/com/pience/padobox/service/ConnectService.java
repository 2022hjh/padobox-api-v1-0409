package com.pience.padobox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.google.gson.Gson;
import com.pience.padobox.model.ErrorReturnDomain;
import com.pience.padobox.utility.EmptyUtils;

@Service
public class ConnectService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());	
	
	Gson gson = new Gson();
	
	@Autowired
	DefaultService defaultService;
	
	/**
	 * @desc  오류 로그 저장, 오류 리턴 메세지 세팅 
	 */
	public ErrorReturnDomain ErrorCheck(String token_type, Integer error_code
			, String param, String seller_id, String post_body){
		
		ErrorReturnDomain errorReturnDomain = new ErrorReturnDomain();
		ErrorReturnDomain errorReturn = new ErrorReturnDomain();
		
		Model model_error = new ExtendedModelMap();
		model_error.addAttribute("error_code", error_code);
		
		try {
			errorReturnDomain = defaultService.getRestErrorSingle(model_error);
			logger.info("ErrorCheck error_code:"+errorReturnDomain.getError_code());
			logger.info("ErrorCheck error_value:"+errorReturnDomain.getError_value());
			
			if(EmptyUtils.isEmpty(errorReturnDomain)==false) {
				
				model_error.addAttribute("rest_type", errorReturnDomain.getRest_type());
				model_error.addAttribute("seller_id", seller_id);
				model_error.addAttribute("error_value", errorReturnDomain.getError_value());
				model_error.addAttribute("rest_path", errorReturnDomain.getRest_path_refer());
				model_error.addAttribute("rest_refer", param);
				model_error.addAttribute("post_body", post_body);
				
				int iRow_log = 0;
				try {
					iRow_log = defaultService.insertErrorLog(model_error);
					logger.info("ErrorCheck iRow_log idx :"+iRow_log);
				} catch (Exception e1) {
					logger.info("ConnectService ErrorCheck insertErrorLog error e : "+e1);
					errorReturnDomain.setError_code(100);
					errorReturnDomain.setError_value("관리자에게 문의 바랍니다.");
				}
			}else {
				errorReturnDomain.setError_code(100);
				errorReturnDomain.setError_value("관리자에게 문의 바랍니다.");
			}
		} catch (Exception e) {
			logger.info("ConnectService ErrorCheck getRestErrorSingle error e : "+e);
			errorReturnDomain.setError_code(100);
			errorReturnDomain.setError_value("관리자에게 문의 바랍니다.");
		}
		
		errorReturn.setError_code(errorReturnDomain.getError_code());
		errorReturn.setError_value(errorReturnDomain.getError_value());
		
		return errorReturn;
	}
}

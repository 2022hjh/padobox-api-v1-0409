package com.pience.padobox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

@Service
public class BatchStatusService {
	
	private static final Logger logger = LoggerFactory.getLogger(AsyncStatusModifyService.class);
	
	@Autowired
	AsyncService asyncService;

    public String createBatch(String sellerId) {
        return sellerId + "-" + System.currentTimeMillis();
    }

	/**
	 * @desc 가능 변경 대용량 비동기 처리 
	 */
    public void updateStatus(String batchId, String seller_id, String status, String errorMessage) {
		Model model_up = new ExtendedModelMap();
		model_up.addAttribute("request_id", batchId);
		model_up.addAttribute("seller_id", seller_id);
		model_up.addAttribute("process_status", "FAIL");
		model_up.addAttribute("order_status_result", "");
		model_up.addAttribute("async_yn", "N");
		model_up.addAttribute("async_error", errorMessage);
		model_up.addAttribute("success_count", 0);
		model_up.addAttribute("failure_count", 0);
		model_up.addAttribute("unavailable_count", 0);
		
		int upRow = 0;
			upRow = asyncService.updateStatusProcessAsyncLog(model_up);
			logger.info("update upRow : "+upRow);
	    }
    
}
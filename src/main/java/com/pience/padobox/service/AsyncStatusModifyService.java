package com.pience.padobox.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.pience.padobox.model.OrderBodyDomain.*;
import com.pience.padobox.model.SellerIdInfoDomain;
import com.pience.padobox.model.SetDomain;

@Service
public class AsyncStatusModifyService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncStatusModifyService.class);

    private final OrderConnectService orderConnectService;
    private final BatchStatusService batchStatusService;

    public AsyncStatusModifyService(OrderConnectService orderConnectService,
                                    BatchStatusService batchStatusService) {
        this.orderConnectService = orderConnectService;
        this.batchStatusService = batchStatusService;
    }

    
	/**
	 * @desc 가능 변경 대용량 비동기 처리 
	 */
    @Async("statusModifyExecutor")
    public void executeStatusModifyAsync(
            String request_id,
            Model model,
            String connectType,
            String seller_id,
            ModifyBodyList requestModifyBody,
            SellerIdInfoDomain sellerid_info,
            String seller_token
    ) {
        try {
            logger.info("async start. request_id={}", request_id);
            SetDomain.ControllerResultStatusModifyReturnV2 ModifyReturnResultV2 = new SetDomain.ControllerResultStatusModifyReturnV2();
            ModifyReturnResultV2 = orderConnectService.postStatusModifyV3All(request_id, 
                    model, connectType, seller_id, requestModifyBody, sellerid_info, seller_token
            );
            if (ModifyReturnResultV2 != null && ModifyReturnResultV2.getError_code() > 0) {
                return;
            }
            logger.info("async done. request_id={}:", request_id);
        } catch (Exception e) {
            logger.error("async exception. request_id={}:", request_id, e);
            batchStatusService.updateStatus(request_id, seller_id, "FAILED", e.getMessage());
        }
    }
}
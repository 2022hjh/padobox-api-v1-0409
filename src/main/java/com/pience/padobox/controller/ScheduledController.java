package com.pience.padobox.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ExtendedModelMap;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.pience.padobox.config.DefaultConfig;
import com.pience.padobox.model.DefaultDomain;
import com.pience.padobox.model.SellerIdListDomain;
import com.pience.padobox.service.MoimApiService;
import com.pience.padobox.service.OrderConnectService;
import com.pience.padobox.service.OrderService;
import com.pience.padobox.utility.EmptyUtils;
import com.pience.padobox.utility.Json;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class ScheduledController {
	
	private final DefaultConfig defaultConfig;
	
    public ScheduledController(DefaultConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
	Gson gson = new Gson();
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	MoimApiService moimApiService;
	
	@Autowired
	OrderConnectService orderConnectService;
	
	/**
	 * @desc order list sync
	 */
	@GetMapping(value="/order-sync-all/{version}/{type}", headers="Accept=application/json;charset=UTF-8", produces="application/json;charset=UTF-8" )
	public @ResponseBody Json OrderSyncAll(
			HttpServletRequest httpServletRequest
			, @RequestHeader(value="X-Pado-REST-API-Key") String rest_api_key
    		, @RequestHeader(value="X-Pado-Session-Token") String rest_session_token
    		, @PathVariable("version") String rest_version
    		, @PathVariable("type") String sync_type
    		, @RequestParam Map<String, String> paramMap
    		, Model model
			) {
		
		// 새벽3시?(전 어부님들 동기화 시간계산 후 결정) 전체 동기화 스케쥴 : 별도 신규 생성 예정
		// 여기는 수동으로 동기화 처리하는 곳
		// step 1 : seller_list insert && plugin_seller_info insert (택배사, 기본 정보등)
		// step 2 : getSellerLista 
		// step 3 : 아래 동기화 진행 : 주문&정산서
		
		String connect_type = defaultConfig.getConnectType();
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		DefaultDomain.ErrorCheck error_status_2 = new DefaultDomain.ErrorCheck();
		List<DefaultDomain.SellerList> getSellerList = new ArrayList<DefaultDomain.SellerList>();
		
		
		if(sync_type.equals("1")==true) {
			// step 1 : seller_list insert
			// moin api get : prod https://payment.moim.co/sellers/CS:2NL6M30N/sub_sellers
			SellerIdListDomain get_sellerid_list = new SellerIdListDomain();
			
			get_sellerid_list = moimApiService.GetAllSellerIdList(connect_type, "");
			
			if(get_sellerid_list.getData().size()>0) {
				for(int i = 0; i < get_sellerid_list.getData().size(); i++) {
					logger.info("id:"+get_sellerid_list.getData().get(i).getId());
					logger.info("name:"+get_sellerid_list.getData().get(i).getName());
					
					Model model_ins = new ExtendedModelMap();
					model_ins.addAttribute("seller_id", get_sellerid_list.getData().get(i).getId());
					model_ins.addAttribute("seller_name", get_sellerid_list.getData().get(i).getName());
					orderService.insertSellerList(model_ins);
					
				}
				if(get_sellerid_list.getPaging() != null) {
					logger.info("getPaging:"+gson.toJson(get_sellerid_list.getPaging()));
					if(EmptyUtils.isEmpty(get_sellerid_list.getPaging().getAfter())==false) {
						logger.info("getAfter:"+get_sellerid_list.getPaging().getAfter());
						
						String after = get_sellerid_list.getPaging().getAfter();
						for(int ii = 0; ii < 1000; ii++) {
							SellerIdListDomain get_sellerid_list_1 = new SellerIdListDomain();
							get_sellerid_list_1 = moimApiService.GetAllSellerIdList(connect_type, after);
							if(get_sellerid_list_1.getPaging() != null) {
								logger.info("getPaging"+ii+":"+gson.toJson(get_sellerid_list_1.getPaging()));
								if(EmptyUtils.isEmpty(get_sellerid_list_1.getPaging().getAfter())==false) {
									logger.info("getAfte"+ii+":"+get_sellerid_list_1.getPaging().getAfter());
									after = get_sellerid_list_1.getPaging().getAfter();
									for(int k = 0; k < get_sellerid_list_1.getData().size(); k++) {
										logger.info("id:"+get_sellerid_list_1.getData().get(k).getId());
										logger.info("name:"+get_sellerid_list_1.getData().get(k).getName());
										Model model_ins = new ExtendedModelMap();
										model_ins.addAttribute("seller_id", get_sellerid_list_1.getData().get(k).getId());
										model_ins.addAttribute("seller_name", get_sellerid_list_1.getData().get(k).getName());
										orderService.insertSellerList(model_ins);
									}
								}
							}	
							if(get_sellerid_list_1.getPaging() == null) {
								break;	
							}else {
								if(EmptyUtils.isEmpty(get_sellerid_list_1.getPaging().getAfter())==true) {
									break;
								}	
							}
						}
					}
				}
			}
		}else if(sync_type.equals("2")==true) {
			// step 2 : getSellerLista
			model.addAttribute("idx_val", 0);
			model.addAttribute("limit_val", 10000000);
			getSellerList = orderService.getSellerList(model);
			
			if(getSellerList.size()>0) {
				for(int j = 0; j < getSellerList.size(); j++) {
					logger.info("sellerid : "+getSellerList.get(j).getSeller_id());
					
					error_status = orderConnectService.GetMoimSyncData(connect_type
							, getSellerList.get(j).getSeller_id(), "order_all");
			  		if(error_status.getError_code()>0) {
			  			logger.info("error_code:"+error_status.getError_code());
			  			logger.info("error_val:"+error_status.getError_val());
			  			break;
			  		}else {
						error_status_2 = orderConnectService.AccountsRdsSyncProcess(connect_type, getSellerList.get(j).getSeller_id(), "account_all");
						if(error_status_2.getError_code()>0) {
							logger.info("error_code 2:"+error_status_2.getError_code());
				  			logger.info("error_val 2:"+error_status_2.getError_val());
				  			break;
						}	
					}
				}
			}
		}
		
		return new Json("{}");
		
	}

}

package com.pience.padobox.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.google.gson.Gson;
import com.pience.padobox.config.DefaultConfig;
import com.pience.padobox.model.DefaultDomain;
import com.pience.padobox.model.SellerIdListDomain;
import com.pience.padobox.service.MoimApiService;
import com.pience.padobox.service.OrderConnectService;
import com.pience.padobox.service.OrderService;
import com.pience.padobox.utility.EmptyUtils;

@Configuration
@EnableScheduling
public class ScheduledConfiguration {
	
	private final DefaultConfig defaultConfig;
	
    public ScheduledConfiguration(DefaultConfig defaultConfig) {
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

	@Scheduled(cron = "0 0 20 * * ?") // 매일 한국 오전 5시 
	public void OrderSyncAllScheduleStart() {

		String connect_type = defaultConfig.getConnectType();
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		DefaultDomain.ErrorCheck error_status_2 = new DefaultDomain.ErrorCheck();
		List<DefaultDomain.SellerList> getSellerList = new ArrayList<DefaultDomain.SellerList>();

		if(connect_type.equals("prod")==true) {

			// step 1 : seller_list insert 
			// https://payment.moim.co/admin/sellers/CS:YA652QPS/delivery_contracts
			// 위 택배정보는 따로 해야해서 /setting/{version} 3번에 동기화 해놓음. 
			// moin api get : prod https://payment.moim.co/sellers/CS:2NL6M30N/sub_sellers
			SellerIdListDomain get_sellerid_list = new SellerIdListDomain();

			get_sellerid_list = moimApiService.GetAllSellerIdList(connect_type, "");

			if (get_sellerid_list.getData().size() > 0) {
				for (int i = 0; i < get_sellerid_list.getData().size(); i++) {
//					logger.info("id:" + get_sellerid_list.getData().get(i).getId());
//					logger.info("name:" + get_sellerid_list.getData().get(i).getName());

					Model model_ins = new ExtendedModelMap();
					model_ins.addAttribute("seller_id", get_sellerid_list.getData().get(i).getId());
					model_ins.addAttribute("seller_name", get_sellerid_list.getData().get(i).getName());
					orderService.insertSellerList(model_ins);

				}
				if (get_sellerid_list.getPaging() != null) {
//					logger.info("getPaging:" + gson.toJson(get_sellerid_list.getPaging()));
					if (EmptyUtils.isEmpty(get_sellerid_list.getPaging().getAfter()) == false) {
//						logger.info("getAfter:" + get_sellerid_list.getPaging().getAfter());

						String after = get_sellerid_list.getPaging().getAfter();
						for (int ii = 0; ii < 1000; ii++) {
							SellerIdListDomain get_sellerid_list_1 = new SellerIdListDomain();
							String page_1 = "";
							String after_1 = "";
							get_sellerid_list_1 = moimApiService.GetAllSellerIdList(connect_type, after);
							if(EmptyUtils.isEmpty(get_sellerid_list_1)==false) {
								if(EmptyUtils.isEmpty(get_sellerid_list_1.getPaging())==false) {
									page_1 = get_sellerid_list_1.getPaging().toString();
								}else {
									page_1 = "";
								}
								if (page_1 != "") {
									logger.info("getPaging" + ii + ":" + gson.toJson(get_sellerid_list_1.getPaging()));
									
									after_1 = get_sellerid_list_1.getPaging().getAfter();
									if (EmptyUtils.isEmpty(after_1) == false) {
//										logger.info("getAfte" + ii + ":" + get_sellerid_list_1.getPaging().getAfter());
										after = get_sellerid_list_1.getPaging().getAfter();
										for (int k = 0; k < get_sellerid_list_1.getData().size(); k++) {
//											logger.info("id:" + get_sellerid_list_1.getData().get(k).getId());
//											logger.info("name:" + get_sellerid_list_1.getData().get(k).getName());
											Model model_ins = new ExtendedModelMap();
											model_ins.addAttribute("seller_id", get_sellerid_list_1.getData().get(k).getId());
											model_ins.addAttribute("seller_name", get_sellerid_list_1.getData().get(k).getName());
											orderService.insertSellerList(model_ins);
										}
									}
								}
							}
							
							if (page_1 == "") {
								break;
							} else {
								if (EmptyUtils.isEmpty(after_1) == true) {
									break;
								}
							}
						}
					}
				}
			}

			// step 2 : getSellerLista
			Model model = new ExtendedModelMap();
//			model.addAttribute("idx_val", 0);
//			model.addAttribute("limit_val", 1000);
			getSellerList = orderService.getSellerListSchedule(model);

			if (getSellerList.size() > 0) {
				for (int j = 0; j < getSellerList.size(); j++) {
//					logger.info("sellerid : " + getSellerList.get(j).getSeller_id());

					error_status = orderConnectService.GetMoimSyncData(connect_type, getSellerList.get(j).getSeller_id(), "order_all_schedule");
					if (error_status.getError_code() > 0) {
						logger.info("error_code:" + error_status.getError_code());
						logger.info("error_val:" + error_status.getError_val());
						break;
					} else {
						error_status_2 = orderConnectService.AccountsRdsSyncProcess(connect_type, getSellerList.get(j).getSeller_id(), "account_all_schedule");
						if (error_status_2.getError_code() > 0) {
							logger.info("error_code 2:" + error_status_2.getError_code());
							logger.info("error_val 2:" + error_status_2.getError_val());
							break;
						}
					}
				}
			}
		}
	}
	
	
	@Scheduled(cron = "0 0 4 * * ?") // 매일 한국 pm1
	public void OrderSyncAllScheduleV2() {

		String connect_type = defaultConfig.getConnectType();
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		DefaultDomain.ErrorCheck error_status_2 = new DefaultDomain.ErrorCheck();
		List<DefaultDomain.SellerList> getSellerList = new ArrayList<DefaultDomain.SellerList>();
		
		if(connect_type.equals("prod")==true) {
			// step 2 : getSellerLista
			Model model = new ExtendedModelMap();
	//		model.addAttribute("idx_val", 0);
	//		model.addAttribute("limit_val", 1000);
			getSellerList = orderService.getSellerListSchedule(model);
	
			if (getSellerList.size() > 0) {
				for (int j = 0; j < getSellerList.size(); j++) {
	//				logger.info("sellerid : " + getSellerList.get(j).getSeller_id());
	
					error_status = orderConnectService.GetMoimSyncData(connect_type, getSellerList.get(j).getSeller_id(), "order_all_schedule");
					if (error_status.getError_code() > 0) {
						logger.info("error_code:" + error_status.getError_code());
						logger.info("error_val:" + error_status.getError_val());
						break;
					} else {
						error_status_2 = orderConnectService.AccountsRdsSyncProcess(connect_type, getSellerList.get(j).getSeller_id(), "account_all_schedule");
						if (error_status_2.getError_code() > 0) {
							logger.info("error_code 2:" + error_status_2.getError_code());
							logger.info("error_val 2:" + error_status_2.getError_val());
							break;
						}
					}
				}
			}
		}
	}
	
	
	@Scheduled(cron = "0 0 8 * * ?") // 매일 한국 pm5 
	public void OrderSyncAllScheduleV3() {

		String connect_type = defaultConfig.getConnectType();
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		DefaultDomain.ErrorCheck error_status_2 = new DefaultDomain.ErrorCheck();
		List<DefaultDomain.SellerList> getSellerList = new ArrayList<DefaultDomain.SellerList>();
		
		if(connect_type.equals("prod")==true) {

			// step 2 : getSellerLista
			Model model = new ExtendedModelMap();
	//		model.addAttribute("idx_val", 0);
	//		model.addAttribute("limit_val", 1000);
			getSellerList = orderService.getSellerListSchedule(model);
	
			if (getSellerList.size() > 0) {
				for (int j = 0; j < getSellerList.size(); j++) {
	//				logger.info("sellerid : " + getSellerList.get(j).getSeller_id());
	
					error_status = orderConnectService.GetMoimSyncData(connect_type, getSellerList.get(j).getSeller_id(), "order_all_schedule");
					if (error_status.getError_code() > 0) {
						logger.info("error_code:" + error_status.getError_code());
						logger.info("error_val:" + error_status.getError_val());
						break;
					} else {
						error_status_2 = orderConnectService.AccountsRdsSyncProcess(connect_type, getSellerList.get(j).getSeller_id(), "account_all_schedule");
						if (error_status_2.getError_code() > 0) {
							logger.info("error_code 2:" + error_status_2.getError_code());
							logger.info("error_val 2:" + error_status_2.getError_val());
							break;
						}
					}
				}
			}
		}
	}
	
	
	@Scheduled(cron = "0 0 13 * * ?") // 매일 한국 pm10 
	public void OrderSyncAllScheduleV4() {

		String connect_type = defaultConfig.getConnectType();
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		DefaultDomain.ErrorCheck error_status_2 = new DefaultDomain.ErrorCheck();
		List<DefaultDomain.SellerList> getSellerList = new ArrayList<DefaultDomain.SellerList>();
		
		if(connect_type.equals("prod")==true) {

			// step 2 : getSellerLista
			Model model = new ExtendedModelMap();
	//		model.addAttribute("idx_val", 0);
	//		model.addAttribute("limit_val", 1000);
			getSellerList = orderService.getSellerListSchedule(model);
	
			if (getSellerList.size() > 0) {
				for (int j = 0; j < getSellerList.size(); j++) {
	//				logger.info("sellerid : " + getSellerList.get(j).getSeller_id());
	
					error_status = orderConnectService.GetMoimSyncData(connect_type, getSellerList.get(j).getSeller_id(), "order_all_schedule");
					if (error_status.getError_code() > 0) {
						logger.info("error_code:" + error_status.getError_code());
						logger.info("error_val:" + error_status.getError_val());
						break;
					} else {
						error_status_2 = orderConnectService.AccountsRdsSyncProcess(connect_type, getSellerList.get(j).getSeller_id(), "account_all_schedule");
						if (error_status_2.getError_code() > 0) {
							logger.info("error_code 2:" + error_status_2.getError_code());
							logger.info("error_val 2:" + error_status_2.getError_val());
							break;
						}
					}
				}
			}
		}
	}
	
}



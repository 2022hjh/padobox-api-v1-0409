package com.pience.padobox.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.google.gson.Gson;
import com.pience.padobox.config.DefaultConfig;
import com.pience.padobox.model.AsyncDomain.*;
import com.pience.padobox.model.AccountDomain;
import com.pience.padobox.model.AlarmTalkPartnerDomain;
import com.pience.padobox.model.AccountDomain.AccountWeekData;
import com.pience.padobox.model.AccountDomain.DataSingle;
import com.pience.padobox.model.AccountDomain.ExcelProductSingle;
import com.pience.padobox.model.AccountDomain.ProductOptionSingle;
import com.pience.padobox.model.DefaultDomain;
import com.pience.padobox.model.OrderBodyDomain;
import com.pience.padobox.model.OrderBodyDomain.GroupedModifyBody;
import com.pience.padobox.model.OrderBodyDomain.ModifyBody;
import com.pience.padobox.model.OrderBodyDomain.ModifyBodyList;
import com.pience.padobox.model.OrderDomain;
import com.pience.padobox.model.OrderDomain.PluginOrderListDomain;
import com.pience.padobox.model.OrderDomain.PluginSellerInfo;
import com.pience.padobox.model.OrderDomain.ProductSingle;
import com.pience.padobox.model.OrderDomain.ProductVariantSingle;
import com.pience.padobox.model.OrderDomain.ReturnDatav1;
import com.pience.padobox.model.OrderGetDomain;
import com.pience.padobox.model.OrderGetDomain.StatusGroupIds;
import com.pience.padobox.model.OrderGetDomain.productVarinatSingle;
import com.pience.padobox.model.SellerIdDeliveryDomain;
import com.pience.padobox.model.SellerIdInfoDomain;
import com.pience.padobox.model.SetDomain;
import com.pience.padobox.model.SetDomain.*;
import com.pience.padobox.model.SetDomain.StatusModifyResultReturnDelivery.DeliveryReceptionResult.ResultV2.ResultV2TypeSingleData;
import com.pience.padobox.utility.*;

import io.jsonwebtoken.lang.Arrays;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeUtility;

@Service
public class OrderConnectService {
	
	private final DefaultConfig defaultConfig;
	
    public OrderConnectService(DefaultConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
	Gson gson = new Gson();
	
	@Autowired
	MoimApiService moimApiService;
	
	@Autowired
	OrderService orderService;
	
	@Autowired
	AsyncService asyncService;
	
	@Autowired
	AlarmPartnerService alarmPartnerService;
	
	private static class DeliverySpec {
	    List<String> items;
	    String courierId;

	    DeliverySpec(List<String> items, String courierId) {
	        this.items = items;
	        this.courierId = courierId;
	    }
	}
	

	/**
	 * @desc  
	 */
	public SellerIdInfoDomain SellerIdCheck(Model model, String connect_type, String seller_id) {
		SellerIdInfoDomain sellerid_info = new SellerIdInfoDomain();
		SetDomain.StatusModifyReturn getUserData = new SetDomain.StatusModifyReturn();
		try {
			getUserData = moimApiService.SellerIdCheck(connect_type, seller_id);
	  		if(getUserData.getError_code()==0) {
	  			sellerid_info = gson.fromJson(getUserData.getResult_return(), sellerid_info.getClass());
	  			sellerid_info.setError_code(0);
	  			sellerid_info.setError_val("");
	  		}else {
	  			sellerid_info.setError_code(102);
	  			sellerid_info.setError_val(getUserData.getError_val());
	  		}
		} catch (Exception e) {
			logger.info("SellerIdCheck > getUserData error : e"+e+"//"+seller_id);
			sellerid_info.setError_code(102);
  			sellerid_info.setError_val("SellerIdCheck > getUserData error : e"+e+"//"+seller_id);
		}
		return sellerid_info;
	}
	

	/**
	 * @desc  
	 */
	public OrderDomain getOrderListNewOrderV2(Model model, String order_type, String connect_type
			, String seller_id, OrderBodyDomain.ParamBody param_body) {
		
		//==========================================================
		// 주문 리스트 v2 order_type : requested
		//==========================================================
		int error_code = 0;
		String error_val = "";
		
		OrderDomain order_list = new OrderDomain();
		OrderGetDomain.GetData order_get_domain = new OrderGetDomain.GetData();
		List<productVarinatSingle> moim_get_data_list = new ArrayList<productVarinatSingle>();
		List<String> data_id_hold = new ArrayList<String>();
		List<ReturnDatav1> datav2 = new ArrayList<ReturnDatav1>();
		int order_new_cnt = 0;
		int observ_cnt = 0;
		int async_count = 0;
		
  		// 전체 신규 주문 -> 가능으로 변경 비동기 진행중인지 가져오기
  		List<StatusProcessAsyncLog> getAsyncLog = new ArrayList<StatusProcessAsyncLog>();
  		Model model_call = new ExtendedModelMap();
		model_call.addAttribute("seller_id", seller_id);
		getAsyncLog = asyncService.getStatusProcessAsyncLog(model_call);
		
		if(EmptyUtils.isEmpty(getAsyncLog)==false) {
			if(getAsyncLog.size()>0) {
				// limit 1로 바꿈
				if(getAsyncLog.get(0).getProcess_status().equals("READY")==true) {
					async_count = 1; // 진행중
				}else {
					async_count = 0; // 없음.
				}	
			}else {
				async_count = 0; // 없음.
			}
		}else {
			async_count = 0; // 없음.
		}
		
		// ===========================================
		// 현재 대용량 가능 변경 진행중에는 데이터를 안 보내려고 했으나 카운트 때문에 데이터 가져옴.
		// ===========================================
		
		String after = "";
		for(int i = 0; i < 100; i++) { // 50 * 100
			String check = "";
	  		check = moimApiService.OrderListGet(order_type, connect_type, seller_id, after, 0L, 0L);
	  		logger.info("getOrderListNewOrderV2 > OrderListGet : check:"+check+"//"+seller_id);
	  		if(check.startsWith("error:")==true) {
	  			error_code = 113;
	  			error_val = check;
	  		}else {
	  			order_get_domain = gson.fromJson(check, order_get_domain.getClass());
	  			if(EmptyUtils.isEmpty(order_get_domain)==false) {
	  				moim_get_data_list.addAll(order_get_domain.getData());
	  			}
	  		}
	  		if(EmptyUtils.isEmpty(order_get_domain.getData())==true) {
	  			logger.info("getOrderListNewOrderV2 111"+"//"+seller_id);	
	  			break;
	  		}else {
	  			if(order_get_domain.getData().size()==0) {
	  				logger.info("getOrderListNewOrderV2 222"+"//"+seller_id);
	  				break;
	  			}
	  		}
  			if(EmptyUtils.isEmpty(order_get_domain.getPaging())==false) {
  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getTotal())==false){
  					order_new_cnt = order_get_domain.getPaging().getTotal();
  				}
  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getAfter())==false) {
  					after = order_get_domain.getPaging().getAfter();
  				}else {// after null
  					logger.info("getOrderListNewOrderV2 333"+"//"+seller_id);
  					break;
  				}
  			}
		}
		if(moim_get_data_list.size()>0) {
			logger.info("getOrderListNewOrderV2 moim_get_data_list size() : "+moim_get_data_list.size()+": "+seller_id);
		}else {
			logger.info("getOrderListNewOrderV2 moim_get_data_list size() 0: "+seller_id);
		}
		//=====================================================
		// 관망중 data_id 가져오기
		if(error_code ==0){
			Integer order_status = 0;// 0 : 주문, 1 : 가능, 2 : 발송, 3 : 취소
			if(order_type.equals("requested")==true) {
				order_status = 0;
			}
			List<String> data_id_list = new ArrayList<String>();
			try {
				// RDS 관망중 체크
				for(int j = 0; j < moim_get_data_list.size(); j++) {
					data_id_list.add(moim_get_data_list.get(j).getId());
				}
	  			Model model_get_1 = new ExtendedModelMap();
				model_get_1.addAttribute("seller_id", seller_id);
				model_get_1.addAttribute("order_sub_status", 1);
				model_get_1.addAttribute("order_status", order_status);
				model_get_1.addAttribute("data_id_list", data_id_list);
				model_get_1.addAttribute("sort", "asc");
				List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
				getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
				if(EmptyUtils.isEmpty(getPluginOrderList)==false) {
					for(int j = 0; j < getPluginOrderList.size(); j++) {
						if(getPluginOrderList.get(j).getOrder_sub_status()==1) {
							data_id_hold.add(getPluginOrderList.get(j).getData_id()); 
							observ_cnt = observ_cnt+1;
						}
					}
				}
			} catch (Exception e) {
				logger.info("getOrderListNewOrderV2 getPluginOrderProductList : e :"+e+"//"+seller_id);
				error_code = 113;
	  			error_val = "getOrderListNewOrderV2 getPluginOrderProductList : e : "+e+"//"+seller_id;
			}
		}
		//=====================================================
		
		if(error_code==0){
			String last_poduct_key = "";
			Set<String> added_product_key_set = new HashSet<String>();
			
			for(int j = 0; j < moim_get_data_list.size(); j++) {
				try {
	  				if(data_id_hold.indexOf(moim_get_data_list.get(j).getId())==-1) {
		  				String group_check = "";
	  	  				group_check = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
	  	  				if(added_product_key_set.contains(group_check)==true) {
	  	  					continue;
	  	  				}
	  	  				added_product_key_set.add(group_check);
	  	  				
		  				if(last_poduct_key.equals(group_check)==false) {
		  					ReturnDatav1 date_single = new ReturnDatav1();
		  					List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
		  					Set<String> addedCiIds = new HashSet<String>();
		  					
		  					date_single = new ReturnDatav1();
	  						date_single.setOrder_key(moim_get_data_list.get(j).getPaymentId());
	  						date_single.setOrder_status(moim_get_data_list.get(j).getStatus());
	  						date_single.setUser_id(moim_get_data_list.get(j).getUserId());
	  						date_single.setUser_name(moim_get_data_list.get(j).getPurchase().getBuyerName());
	  						date_single.setRecipient_name(moim_get_data_list.get(j).getPurchase().getRecipientName());
	  						date_single.setProduct_key(moim_get_data_list.get(j).getProductId());
	  						date_single.setProduct_name(moim_get_data_list.get(j).getProductName());
	  						date_single.setRecipient_phone(moim_get_data_list.get(j).getPurchase().getBuyerPhone());
	  						date_single.setRecipient_zipcode(moim_get_data_list.get(j).getPurchase().getZipcode());
	  						date_single.setRecipient_address(moim_get_data_list.get(j).getPurchase().getAddress()+" "+moim_get_data_list.get(j).getPurchase().getAddress2());
	  						String pay_date = "";
			  	  			if(EmptyUtils.isEmpty(moim_get_data_list.get(j).getPaidAt())==false) {
								if(moim_get_data_list.get(j).getPaidAt()>0) {
									pay_date = LocationTimeCal.TimeStamptoDate(moim_get_data_list.get(j).getPaidAt());
								}
							}
		  	  				date_single.setPay_date(pay_date);
			  	  			
			  	  			for (int i2= 0; i2 < moim_get_data_list.size(); i2++) {
			  	  				if(moim_get_data_list.get(j).getPurchaseId().equals(moim_get_data_list.get(i2).getPurchaseId())==true
			  	  						&& moim_get_data_list.get(j).getProductId().equals(moim_get_data_list.get(i2).getProductId())==true 
			  	  						) {
			  	  					
			  	  					String ciId = moim_get_data_list.get(i2).getId();
			  	  					if(addedCiIds.contains(ciId)==true) {
			  	  						continue;
			  	  					}
			  	  					addedCiIds.add(ciId);
			  	  					
					  	  			OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
					  	  			pruduct_variant_single.setProduct_variant_key(moim_get_data_list.get(i2).getId()); 
			  						pruduct_variant_single.setProduct_variant_id(moim_get_data_list.get(i2).getProductVariantId());
			  						// 이렇게 가져오는 이유는 ko > 옵션 이라서 옵션은 domain > name 할 수 없음.   
			  						/*
										"productVariantValue":{
										    "ko":{
										       "옵션":"[밴댕이 회] 손질 후 200g"
										    }
										 }
				  					*/
			  						String var_val1 = "";
			  						String[] var_val_array1 = null;
			  						if(EmptyUtils.isEmpty(moim_get_data_list.get(i2).getProductVariantValue())==false) {
			  							var_val1 = moim_get_data_list.get(i2).getProductVariantValue().toString();
			  							var_val1 = var_val1.replaceAll("\\{", "");
			  							var_val1 = var_val1.replaceAll("\\}", "");
			  						}
			  						if(EmptyUtils.isEmpty(var_val1)==false) {
			  							var_val_array1 = var_val1.split("=");
			  							for(int ii = 0; ii < var_val_array1.length; ii++) {
			  								if(ii==2){
			  									if(EmptyUtils.isEmpty(var_val_array1[ii])==false) {
			  										var_val1 =	var_val_array1[ii];
			  									}
			  								}
			  							}
			  						}
									pruduct_variant_single.setProduct_variant_name(var_val1);
									pruduct_variant_single.setQuantity(moim_get_data_list.get(i2).getQuantity());
									product_variant_single_list.add(pruduct_variant_single);	
				  	  			}
			  	  			
			  	  			}
				  	  		date_single.setProduct_variant_list(product_variant_single_list);
		  					datav2.add(date_single);
		  				}
		  				last_poduct_key = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
	  				}
				} catch (Exception e) {
					logger.info("getOrderListNewOrderV2 data_id_hold.indexOf(moim_get_data_list.get(j).getId()) : e :"+e+"//"+seller_id);
					error_code = 113;
		  			error_val = "getOrderListNewOrderV2 data_id_hold.indexOf(moim_get_data_list.get(j).getId()) : e : "+e+"//"+seller_id;
				}
			}
		}
		logger.info("getOrderListNewOrderV2 order_new_cnt:"+order_new_cnt+"//"+seller_id);
		logger.info("getOrderListNewOrderV2 observ_cnt:"+observ_cnt+"//"+seller_id);
		order_list.setAsync_count(async_count);
  		order_list.setOrder_product_total_count(order_new_cnt);
  		order_list.setDatav2(datav2);
		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		return order_list;
	}


	/**
	 * @desc  
	 */
	public OrderDomain getOrderListNewOrder(Model model, String order_type, String connect_type
			, String seller_id, OrderBodyDomain.ParamBody param_body) {
		
		//==========================================================
		// 주문 리스트 order_type : requested
		//==========================================================
		int error_code = 0;
		String error_val = "";
		
		OrderDomain order_list = new OrderDomain();
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		OrderGetDomain.GetData order_get_domain = new OrderGetDomain.GetData();
		
		int order_product_total_count = 0;
	    
	    //=====================================================================================================
  		// 모든 status 동기화 
  		error_status = GetMoimSyncData(connect_type, seller_id, "requested");
  		if(error_status.getError_code()>0) {
  			error_code = error_status.getError_code();
  			error_val = error_status.getError_val();
  		}
  		logger.info("getOrderListNewOrder requested GetMoimSyncData end"+"//"+seller_id); 	
  		//--------------------------------------------------------------------------		
		// moimApiService.OrderListGet 신규 주문만 가져와서 rds에서 가져옴.  
  		// 왜? 동기화 신뢰를 못함. 상태별로 가져오는 api로 id 다 가져와서 리턴해주려고.
  		// 추가 : 동기화를 신뢰못함. 고로 상태별로 가져온 데이터 다시 동기화 해줌. 
  		String check = "";
  		List<String> data_id_list = new ArrayList<String>();// 주문리스트 가져온 모든 id가 있음. 리턴 데이터에 필요
  		// 우선 moim api 에서 requested 다 가져옴. 
  		// 위 동기화하고 별도임. 
  		check = moimApiService.OrderListGet(order_type, connect_type, seller_id, "", 0L, 0L);
  		logger.info("getOrderListNewOrder>OrderListGet : check:"+check+"//"+seller_id);
  		if(check.startsWith("error:")==true) {
  			error_code = 113;
  			error_val = check;
  		}else {
  			order_get_domain = gson.fromJson(check, order_get_domain.getClass());
  	  		if(EmptyUtils.isEmpty(order_get_domain.getData())==false){
	  	  		DefaultDomain.ErrorCheck error_status_1 = new DefaultDomain.ErrorCheck();
				DefaultDomain.ErrorCheck error_status_2 = new DefaultDomain.ErrorCheck();
				if(error_status_1.getError_code()==0) {
					for(int j = 0; j < order_get_domain.getData().size(); j++) {
						data_id_list.add(order_get_domain.getData().get(j).getId());
	  				}
					if(EmptyUtils.isEmpty(order_get_domain.getPaging())==false) {
						if(EmptyUtils.isEmpty(order_get_domain.getPaging().getAfter())==false) {
			  	  				int total_cnt = 0;
			  					int after_cnt = 0; 
			  					if(EmptyUtils.isEmpty(order_get_domain.getPaging().getAfter())==false) {
			  						after_cnt = Integer.parseInt(order_get_domain.getPaging().getAfter());
			  					}
			  					if(EmptyUtils.isEmpty(order_get_domain.getPaging().getTotal())==false) {
			  						total_cnt = order_get_domain.getPaging().getTotal();
			  					}
			  	  				
			  					if(total_cnt > after_cnt) {
			  						int size_val = 0;
			  						size_val = (int) Math.ceil((float)total_cnt / (float)after_cnt);
			  						size_val = size_val +1;
			  						String after_val = "";
			  						if(EmptyUtils.isEmpty(order_get_domain.getPaging().getAfter())==false) {
			  							after_val = order_get_domain.getPaging().getAfter();	
			  				  		}
			  			  			if(EmptyUtils.isEmpty(after_val)==false) {
			  			  				for(int kk = 0; kk < size_val; kk++) {
			  			  					String check_2 = "";
			  			  					check_2 = moimApiService.OrderListGet(order_type, connect_type, seller_id, after_val, 0L, 0L);
			  			  					OrderGetDomain.GetData order_get_domain_2 = new OrderGetDomain.GetData();
			  			  					order_get_domain_2 = gson.fromJson(check_2, order_get_domain_2.getClass());
			  			  					if(EmptyUtils.isEmpty(order_get_domain_2.getData())==false){
												if(error_status_2.getError_code()>0) {
													error_code = error_status_2.getError_code();
													error_val = "RdsSyncProcess check_2 error : "+error_status_2.getError_val();
												}else {
													for(int j = 0; j < order_get_domain_2.getData().size(); j++) {
				  										data_id_list.add(order_get_domain_2.getData().get(j).getId());
				  									}
				  			  						if(EmptyUtils.isEmpty(order_get_domain_2.getPaging().getAfter())==false) {
				  										after_val = order_get_domain_2.getPaging().getAfter();
				  									}else {
				  										logger.info("getOrderListNewOrder after_val empty!!"+"//"+seller_id);
				  										break;
				  									}
			  			  						}
			  			  					}
			  			  				}
			  			  			}
			  					}
			  	  			}
					}
	  	  			
				}else {
					error_code = 112;
		  			error_val = "getOrderListNewOrder requested RdsSyncProcess check error : "+error_status_1.getError_val();
				}
  	  			
  	  		}
  		}
  		if(error_code==0) {
			Integer order_status = 0;// 0 : 주문, 1 : 가능, 2 : 발송, 3 : 취소
			if(order_type.equals("requested")==true) {// 주문 리스트 
				order_status = 0;
			}else if(order_type.equals("processing")==true) {// 가능 리스트
				order_status = 1;
			}else if(order_type.equals("delivered")==true) {// 배송완료 리스트
				order_status = 2;
			}else if(order_type.equals("cancelled")==true) {// 취소 리스트
				order_status = 3;
			}
			logger.info("getOrderListNewOrderorder_status:"+order_status+"//"+seller_id);
			List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
			List<OrderDomain.ReturnData> data_list = new ArrayList<OrderDomain.ReturnData>();
			try {
				Model model_get_1 = new ExtendedModelMap();
				model_get_1.addAttribute("seller_id", seller_id);
				model_get_1.addAttribute("order_sub_status", 0);
				model_get_1.addAttribute("data_id_list", data_id_list);
				model_get_1.addAttribute("sort", "asc");
				getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
				
				if(EmptyUtils.isEmpty(getPluginOrderList)==false) {
					if(getPluginOrderList.size()>0) {
						String last_order_key = "";
						OrderDomain.ReturnData returnDataSingle = new OrderDomain.ReturnData();
						List<OrderDomain.ProductSingle> product_single_list = new ArrayList<OrderDomain.ProductSingle>();
						for(int j = 0; j < getPluginOrderList.size(); j++) {
							if(getPluginOrderList.get(j).getPurchaseId().equals(last_order_key)==false) {
								returnDataSingle = new OrderDomain.ReturnData();
								returnDataSingle.setOrder_key(getPluginOrderList.get(j).getPurchaseId());
								returnDataSingle.setOrder_status(getPluginOrderList.get(j).getMoim_status());
								returnDataSingle.setUser_id(getPluginOrderList.get(j).getUser_id());
								returnDataSingle.setUser_name(getPluginOrderList.get(j).getBuyer_name());
								returnDataSingle.setPay_date(getPluginOrderList.get(j).getPaid_date());
								product_single_list = new ArrayList<OrderDomain.ProductSingle>();
								String last_product_key = "";
								for(int jj = 0; jj < getPluginOrderList.size(); jj++) {
									if(getPluginOrderList.get(jj).getPurchaseId().equals(getPluginOrderList.get(j).getPurchaseId())==true) {
										if(last_product_key.equals(getPluginOrderList.get(jj).getProductId())==false){
											OrderDomain.ProductSingle product_single = new OrderDomain.ProductSingle();
											product_single.setProduct_key(getPluginOrderList.get(jj).getProductId());
											product_single.setProduct_name(getPluginOrderList.get(jj).getProduct_name());
											product_single.setRecipient_name(getPluginOrderList.get(jj).getRecipient_name());
											List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
											for(int ii = 0; ii < getPluginOrderList.size(); ii++) {
												if(getPluginOrderList.get(jj).getPurchaseId().equals(getPluginOrderList.get(ii).getPurchaseId())==true) {
													if(getPluginOrderList.get(jj).getProductId().equals(getPluginOrderList.get(ii).getProductId())==true) {
														OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
														pruduct_variant_single.setProduct_variant_id(getPluginOrderList.get(ii).getProduct_variant_id());
														pruduct_variant_single.setProduct_variant_value(getPluginOrderList.get(ii).getProduct_variant_name());
														pruduct_variant_single.setQuantity(getPluginOrderList.get(ii).getProduct_variant_quantity());
														product_variant_single_list.add(pruduct_variant_single);															
													}
												}
											}
											product_single.setProduct_variant_list(product_variant_single_list);
											product_single_list.add(product_single);	
										}
										last_product_key = getPluginOrderList.get(jj).getProductId();	
									}
								}
								returnDataSingle.setProduct_list(product_single_list);	
								data_list.add(returnDataSingle);	
							}
							last_order_key = getPluginOrderList.get(j).getPurchaseId();
						}
						
						SetDomain.MainMenuCount total_count = new SetDomain.MainMenuCount();
						Model model_count = new ExtendedModelMap();
						model_count.addAttribute("seller_id", seller_id);
						model_count.addAttribute("view_type", "new");
						total_count = orderService.getPluginMainCnt(model_count);
						order_product_total_count = total_count.getNew_order_cnt();
						
						
						order_list.setData(data_list);
						order_list.setLast_idx(null);  
						order_list.setOrder_product_total_count(order_product_total_count);
						order_list.setOrder_product_cnt(null);
					}
				}
			} catch (Exception e) {
				error_code = 114;
				error_val = "getOrderListNewOrder requested getPluginOrderProductList error : e : "+e;
			}
		}
		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		return order_list;
	}
	

	/**
	 * @desc  
	 */
	public OrderDomain getOrderListReceiveingDiliveryListV2(Model model, String order_type, String connect_type
			, String seller_id, OrderBodyDomain.ParamBody param_body) {
		
		//==========================================================
		// 주문 리스트 : 택배접수 리스트 : 20260306 
//		- 배송 접수 대기 페이지 
//		- 목적 : preparingForDelivery 배달준비중 --> 배송 접수 진행중 paid 다음 단계 
//		- 구매자한테 알림톡 발송(모임시스템)후 택배사 송장번호 지정이 되는 순간 waitingToBePickedUp 상태값은 가능 리스트로 변경됨. 
//		- 별도 리스트 이유 : 기존에는 신규 리스트에 있었는데 어장에서 가능변경했는데 왜 신규에 있냐고 해서 별도 리스트 추가됨. 
//		- 신규에 없애려고 waitingToBePickedUp (모임에서 변경) 진행이 되었으나 구매자 알림톡이 안가서 별도 리스트 추가됨. 
		//==========================================================
		int error_code = 0;
		String error_val = "";
		
		OrderDomain order_list = new OrderDomain();
		OrderGetDomain.GetData order_get_domain = new OrderGetDomain.GetData();
		List<productVarinatSingle> moim_get_data_list = new ArrayList<productVarinatSingle>();
		List<String> data_id_hold = new ArrayList<String>();
		List<ReturnDatav1> datav2 = new ArrayList<ReturnDatav1>();
		int order_new_cnt = 0;
		int observ_cnt = 0;
		
		String after = "";
		for(int i = 0; i < 100; i++) { 
			String check = "";
	  		check = moimApiService.OrderListGet(order_type, connect_type, seller_id, after, 0L, 0L);
	  		logger.info("getOrderListReceiveingDiliveryListV2 > OrderListGet: check:"+check+"//"+seller_id);
	  		if(check.startsWith("error:")==true) {
	  			error_code = 113;
	  			error_val = check;
	  		}else {
	  			order_get_domain = gson.fromJson(check, order_get_domain.getClass());
	  			if(EmptyUtils.isEmpty(order_get_domain)==false) {
	  				moim_get_data_list.addAll(order_get_domain.getData());
	  			}
	  		}
	  		if(EmptyUtils.isEmpty(order_get_domain.getData())==true) {
	  			logger.info("getOrderListReceiveingDiliveryListV2 111"+"//"+seller_id);	
	  			break;
	  		}else {
	  			if(order_get_domain.getData().size()==0) {
	  				logger.info("getOrderListReceiveingDiliveryListV2 222"+"//"+seller_id);
	  				break;
	  			}
	  		}
  			if(EmptyUtils.isEmpty(order_get_domain.getPaging())==false) {
  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getTotal())==false){
  					order_new_cnt = order_get_domain.getPaging().getTotal();
  				}
  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getAfter())==false) {
  					after = order_get_domain.getPaging().getAfter();
  				}else {
  					logger.info("getOrderListReceiveingDiliveryListV2 333"+"//"+seller_id);
  					break;
  				}
  			}
		}
		if(moim_get_data_list.size()>0) {
			logger.info("getOrderListReceiveingDiliveryListV2 moim_get_data_list size() : "+moim_get_data_list.size()+": "+seller_id);
		}else {
			logger.info("getOrderListReceiveingDiliveryListV2 moim_get_data_list size() 0: "+seller_id);
		}
		//=====================================================
		if(error_code ==0){
			Integer order_status = 0;// 0 : 주문, 1 : 가능, 2 : 발송, 3 : 취소
			if(order_type.equals("requested")==true) {// 주문 리스트 
				order_status = 0;
			}
			List<String> data_id_list = new ArrayList<String>();
			try {
				for(int j = 0; j < moim_get_data_list.size(); j++) {
					data_id_list.add(moim_get_data_list.get(j).getId());
				}
	  			Model model_get_1 = new ExtendedModelMap();
				model_get_1.addAttribute("seller_id", seller_id);
				model_get_1.addAttribute("order_sub_status", 1);
				model_get_1.addAttribute("order_status", order_status);
				model_get_1.addAttribute("data_id_list", data_id_list);
				model_get_1.addAttribute("sort", "asc");
				List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
				getPluginOrderList = orderService.getPluginOrderProductList(model_get_1); 
				if(EmptyUtils.isEmpty(getPluginOrderList)==false) {
					for(int j = 0; j < getPluginOrderList.size(); j++) {
						if(getPluginOrderList.get(j).getOrder_sub_status()==1) {
							data_id_hold.add(getPluginOrderList.get(j).getData_id());
							observ_cnt = observ_cnt+1;
						}
					}
				}
			} catch (Exception e) {
				logger.info("getOrderListNewOrderV2 getPluginOrderProductList : e :"+e+"//"+seller_id);
				error_code = 113;
	  			error_val = "getOrderListNewOrderV2 getPluginOrderProductList : e : "+e+"//"+seller_id;
			}
		}
		//=====================================================
		
		if(error_code==0){
			String last_poduct_key = "";
			for(int j = 0; j < moim_get_data_list.size(); j++) {
				try {
	  				if(data_id_hold.indexOf(moim_get_data_list.get(j).getId())==-1) {
		  				String group_check = "";
	  	  				group_check = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
		  				if(last_poduct_key.equals(group_check)==false) {
		  					ReturnDatav1 date_single = new ReturnDatav1();
		  					List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
		  					date_single = new ReturnDatav1();
	  						date_single.setOrder_key(moim_get_data_list.get(j).getPaymentId());
	  						date_single.setOrder_status(moim_get_data_list.get(j).getStatus());
	  						date_single.setUser_id(moim_get_data_list.get(j).getUserId());
	  						date_single.setUser_name(moim_get_data_list.get(j).getPurchase().getBuyerName());
	  						date_single.setRecipient_name(moim_get_data_list.get(j).getPurchase().getRecipientName());
	  						date_single.setProduct_key(moim_get_data_list.get(j).getProductId());
	  						date_single.setProduct_name(moim_get_data_list.get(j).getProductName());
	  						date_single.setRecipient_phone(moim_get_data_list.get(j).getPurchase().getBuyerPhone());
	  						date_single.setRecipient_zipcode(moim_get_data_list.get(j).getPurchase().getZipcode());
	  						date_single.setRecipient_address(moim_get_data_list.get(j).getPurchase().getAddress()+" "+moim_get_data_list.get(j).getPurchase().getAddress2());
	  						String pay_date = "";
			  	  			if(EmptyUtils.isEmpty(moim_get_data_list.get(j).getPaidAt())==false) {
								if(moim_get_data_list.get(j).getPaidAt()>0) {
									pay_date = LocationTimeCal.TimeStamptoDate(moim_get_data_list.get(j).getPaidAt());
								}
							}
		  	  				date_single.setPay_date(pay_date);
			  	  			for (int i2= 0; i2 < moim_get_data_list.size(); i2++) {
			  	  				if(moim_get_data_list.get(j).getPaymentId().equals(moim_get_data_list.get(i2).getPaymentId())==true
			  	  						&& moim_get_data_list.get(j).getProductId().equals(moim_get_data_list.get(i2).getProductId())==true 
			  	  						) {
					  	  			OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
					  	  			pruduct_variant_single.setProduct_variant_key(moim_get_data_list.get(i2).getId()); //CI:
			  						pruduct_variant_single.setProduct_variant_id(moim_get_data_list.get(i2).getProductVariantId());
			  						String var_val1 = "";
			  						String[] var_val_array1 = null;
			  						if(EmptyUtils.isEmpty(moim_get_data_list.get(i2).getProductVariantValue())==false) {
			  							var_val1 = moim_get_data_list.get(i2).getProductVariantValue().toString();
			  							var_val1 = var_val1.replaceAll("\\{", "");
			  							var_val1 = var_val1.replaceAll("\\}", "");
			  						}
			  						if(EmptyUtils.isEmpty(var_val1)==false) {
			  							var_val_array1 = var_val1.split("=");
			  							for(int ii = 0; ii < var_val_array1.length; ii++) {
			  								if(ii==2){
			  									if(EmptyUtils.isEmpty(var_val_array1[ii])==false) {
			  										var_val1 =	var_val_array1[ii];
			  									}
			  								}
			  							}
			  						}
									pruduct_variant_single.setProduct_variant_name(var_val1);
									pruduct_variant_single.setQuantity(moim_get_data_list.get(i2).getQuantity());
									product_variant_single_list.add(pruduct_variant_single);	
				  	  			}
			  	  			}
				  	  		date_single.setProduct_variant_list(product_variant_single_list);
		  					datav2.add(date_single);
		  				}
		  				last_poduct_key = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
	  				}
				} catch (Exception e) {
					logger.info("getOrderListNewOrderV2 data_id_hold.indexOf(moim_get_data_list.get(j).getId()) "
							+ ": e :"+e+"//"+seller_id);
					error_code = 113;
		  			error_val = "getOrderListNewOrderV2 data_id_hold.indexOf(moim_get_data_list.get(j).getId())"
		  					+ " : e : "+e+"//"+seller_id;
				}
			}
		}
		logger.info("getOrderListReceiveingDiliveryListV2 order_new_cnt:"+order_new_cnt+"//"+seller_id);
		logger.info("getOrderListReceiveingDiliveryListV2 observ_cnt:"+observ_cnt+"//"+seller_id);
  		order_list.setOrder_product_total_count(order_new_cnt);
  		order_list.setDatav2(datav2);
		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		return order_list;
	}
	

	/**
	 * @desc  
	 */
	public OrderDomain getOrderListPossibleOrderV2(Model model, String order_type, String connect_type
			, String seller_id, OrderBodyDomain.ParamBody param_body) {
		
		//==========================================================
		// 가능 리스트 v2 order_type : processing
		//==========================================================
		int error_code = 0;
		String error_val = "";
		
		OrderDomain order_list = new OrderDomain();
		OrderGetDomain.GetData order_get_domain = new OrderGetDomain.GetData();
		List<productVarinatSingle> moim_get_data_list = new ArrayList<productVarinatSingle>();
		List<String> data_id_hold = new ArrayList<String>();
		List<ReturnDatav1> datav2 = new ArrayList<ReturnDatav1>();
		int order_new_cnt = 0;
		int observ_cnt = 0;
		int async_count = 0;
		
		// 전체 신규 주문 -> 가능으로 변경 비동기 진행중인지 가져오기
  		List<StatusProcessAsyncLog> getAsyncLog = new ArrayList<StatusProcessAsyncLog>();
  		Model model_call = new ExtendedModelMap();
		model_call.addAttribute("seller_id", seller_id);
		getAsyncLog = asyncService.getStatusProcessAsyncLog(model_call);
		
		if(EmptyUtils.isEmpty(getAsyncLog)==false) {
			if(getAsyncLog.size()>0) {
				if(getAsyncLog.get(0).getProcess_status().equals("READY")==true) {
					async_count = 1; // 진행중
				}else {
					async_count = 0; // 없음.
				}	
			}else {
				async_count = 0; // 없음.
			}
		}else {
			async_count = 0; // 없음.
		}

		String after = "";
		for(int i = 0; i < 100; i++) {
			String check = "";
	  		check = moimApiService.OrderListGet(order_type, connect_type, seller_id, after, 0L, 0L);
	  		logger.info("getOrderListPossibleOrderV2>OrderListGet: check:"+check+"//"+seller_id);
	  		if(check.startsWith("error:")==true) {
	  			error_code = 113;
	  			error_val = check;
	  		}else {
	  			order_get_domain = gson.fromJson(check, order_get_domain.getClass());
	  			if(EmptyUtils.isEmpty(order_get_domain)==false) {
	  				moim_get_data_list.addAll(order_get_domain.getData());
	  			}
	  		}
	  		if(EmptyUtils.isEmpty(order_get_domain.getData())==true) {
	  			logger.info("getOrderListPossibleOrderV2 111"+"//"+seller_id);	
	  			break;
	  		}else {
	  			if(order_get_domain.getData().size()==0) {
	  				logger.info("getOrderListPossibleOrderV2 222"+"//"+seller_id);
	  				break;
	  			}
	  		}
  			if(EmptyUtils.isEmpty(order_get_domain.getPaging())==false) {
  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getTotal())==false){
  					order_new_cnt = order_get_domain.getPaging().getTotal();
  				}
  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getAfter())==false) {
  					after = order_get_domain.getPaging().getAfter();
  				}else {
  					logger.info("getOrderListPossibleOrderV2 333"+"//"+seller_id);
  					break;
  				}
  			}
		}
		
		if(moim_get_data_list.size()>0) {
			logger.info("getOrderListPossibleOrderV2 moim_get_data_list size() : "+moim_get_data_list.size() +": "+seller_id);
		}else {
			logger.info("getOrderListPossibleOrderV2 moim_get_data_list size() 0: "+seller_id);
		}
		
		//=====================================================
		if(error_code ==0){
			List<String> data_id_list = new ArrayList<String>();
			try {
				for(int j = 0; j < moim_get_data_list.size(); j++) {
					data_id_list.add(moim_get_data_list.get(j).getId());
				}
	  			Model model_get_1 = new ExtendedModelMap();
				model_get_1.addAttribute("seller_id", seller_id);
				model_get_1.addAttribute("order_sub_status", 1);
				model_get_1.addAttribute("data_id_list", data_id_list);
				model_get_1.addAttribute("sort", "asc");
				List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
				getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
				if(EmptyUtils.isEmpty(getPluginOrderList)==false) {
					for(int j = 0; j < getPluginOrderList.size(); j++) {
						if(getPluginOrderList.get(j).getOrder_sub_status()==1) {
							data_id_hold.add(getPluginOrderList.get(j).getData_id());
							observ_cnt = observ_cnt+1;
						}
					}
				}
			} catch (Exception e) {
				logger.info("getOrderListPossibleOrderV2 getPluginOrderProductList : e :"+e+"//"+seller_id);
				error_code = 113;
	  			error_val = "getOrderListPossibleOrderV2 getPluginOrderProductList : e : "+e+"//"+seller_id;
			}
		}
		//=====================================================
		
		if(error_code==0){
			String last_poduct_key = "";
			Set<String> added_product_key_set = new HashSet<String>();
			
			for(int j = 0; j < moim_get_data_list.size(); j++) {
				try {
					if(data_id_hold.indexOf(moim_get_data_list.get(j).getId())==-1) {
			  		}	
				} catch (Exception e) {
					logger.info("getOrderListPossibleOrderV2 chtch eeeeeeeeee: "+e+"//"+seller_id);
				}
				
				try {
					if(data_id_hold.indexOf(moim_get_data_list.get(j).getId())==-1) {
						
						String group_check = "";
		  	  			group_check = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
						if(added_product_key_set.contains(group_check)==true) {
							continue;
						}
						added_product_key_set.add(group_check);
		  				if(last_poduct_key.equals(group_check)==false) {
		  					ReturnDatav1 date_single = new ReturnDatav1();
		  					date_single = new ReturnDatav1();
								date_single.setOrder_key(moim_get_data_list.get(j).getPaymentId());
								date_single.setOrder_status(moim_get_data_list.get(j).getStatus());
								date_single.setUser_id(moim_get_data_list.get(j).getUserId());
								date_single.setUser_name(moim_get_data_list.get(j).getPurchase().getBuyerName());
								date_single.setRecipient_name(moim_get_data_list.get(j).getPurchase().getRecipientName());
								date_single.setProduct_key(moim_get_data_list.get(j).getProductId());
								date_single.setProduct_name(moim_get_data_list.get(j).getProductName());
								if(EmptyUtils.isEmpty(moim_get_data_list.get(j).getDeliveryCompanyName())==false) {
									date_single.setDelivery_company_name(moim_get_data_list.get(j).getDeliveryCompanyName());
								}else {
									date_single.setDelivery_company_name("");
								}
								if(EmptyUtils.isEmpty(moim_get_data_list.get(j).getTrackingNumber())==false) {
									date_single.setTracking_number(moim_get_data_list.get(j).getTrackingNumber());
								}else {
									date_single.setTracking_number("");
								}
								date_single.setBuyer_name(moim_get_data_list.get(j).getPurchase().getBuyerName());
								date_single.setBuyer_phone(moim_get_data_list.get(j).getPurchase().getBuyerPhone());
								date_single.setRecipient_phone(moim_get_data_list.get(j).getPurchase().getBuyerPhone());
								date_single.setRecipient_zipcode(moim_get_data_list.get(j).getPurchase().getZipcode());
								date_single.setRecipient_address(moim_get_data_list.get(j).getPurchase().getAddress()+" "+moim_get_data_list.get(j).getPurchase().getAddress2());
								if(EmptyUtils.isEmpty(moim_get_data_list.get(j).getPurchase().getMemo())==false) {
									date_single.setRecipient_memo(moim_get_data_list.get(j).getPurchase().getMemo());	
								}else {
									date_single.setRecipient_memo("");
								}
								String pay_date = "";
			  	  			if(EmptyUtils.isEmpty(moim_get_data_list.get(j).getPaidAt())==false) {
									if(moim_get_data_list.get(j).getPaidAt()>0) {
										pay_date = LocationTimeCal.TimeStamptoDate(moim_get_data_list.get(j).getPaidAt());
									}
								}
		  	  				date_single.setPay_date(pay_date);
		  	  				List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
		  	  				Set<String> addedCiIds = new HashSet<String>();
		  	  				
			  	  			for (int i2= 0; i2 < moim_get_data_list.size(); i2++) {
			  	  				if (moim_get_data_list.get(j).getPurchaseId().equals(moim_get_data_list.get(i2).getPurchaseId())
			  	  				    && moim_get_data_list.get(j).getProductId().equals(moim_get_data_list.get(i2).getProductId())) {
			  	  					
			  	  					String ciId = moim_get_data_list.get(i2).getId();
			  	  					if(addedCiIds.contains(ciId)==true) {
			  	  						continue;
			  	  					}
			  	  					addedCiIds.add(ciId);
			  	  					
			  	  					OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
				  	  				pruduct_variant_single.setProduct_variant_key(moim_get_data_list.get(i2).getId()); //CI:
			  						pruduct_variant_single.setProduct_variant_id(moim_get_data_list.get(i2).getProductVariantId());
			  						String var_val1 = "";
			  						String[] var_val_array1 = null;
			  						if(EmptyUtils.isEmpty(moim_get_data_list.get(i2).getProductVariantValue())==false) {
			  							var_val1 = moim_get_data_list.get(i2).getProductVariantValue().toString();
			  							var_val1 = var_val1.replaceAll("\\{", "");
			  							var_val1 = var_val1.replaceAll("\\}", "");
			  						}
			  						if(EmptyUtils.isEmpty(var_val1)==false) {
			  							var_val_array1 = var_val1.split("=");
			  							for(int ii = 0; ii < var_val_array1.length; ii++) {
			  								
			  								if(ii==2){
			  									if(EmptyUtils.isEmpty(var_val_array1[ii])==false) {
			  										var_val1 =	var_val_array1[ii];
			  									}
			  								}
			  							}
			  						}
									pruduct_variant_single.setProduct_variant_name(var_val1);
									pruduct_variant_single.setQuantity(moim_get_data_list.get(i2).getQuantity());
									product_variant_single_list.add(pruduct_variant_single);	
				  	  			}
			  	  			}
			  	  			date_single.setProduct_variant_list(product_variant_single_list);
							datav2.add(date_single);
		  				}
		  				last_poduct_key = group_check;
					}
				} catch (Exception e) {
					logger.info("getOrderListPossibleOrderV2 data_id_hold.indexOf(moim_get_data_list.get(j).getId())"
							+ " : e :"+e+"//"+seller_id);
					error_code = 113;
		  			error_val = "getOrderListPossibleOrderV2 data_id_hold.indexOf(moim_get_data_list.get(j).getId())"
		  					+ " : e : "+e+"//"+seller_id;
				}
			}
		}
		logger.info("getOrderListPossibleOrderV2 order_new_cnt:"+order_new_cnt+"//"+seller_id);
		logger.info("getOrderListPossibleOrderV2 observ_cnt:"+observ_cnt+"//"+seller_id);
		
		order_list.setAsync_count(async_count);
  		order_list.setOrder_product_total_count(order_new_cnt);
  		order_list.setDatav2(datav2);
		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		return order_list;
		
	}
	

	/**
	 * @desc  
	 */
	public OrderDomain getOrderListPossibleOrder(Model model, String order_type, String connect_type
			, String seller_id, OrderBodyDomain.ParamBody param_body) {
		
		//==========================================================
		// 가능 리스트 order_type : processing
		//==========================================================
		int error_code = 0;
		String error_val = "";
		OrderDomain order_list = new OrderDomain();
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		OrderGetDomain.GetData order_get_domain = new OrderGetDomain.GetData();
		int order_product_total_count = 0;
	    
	    //=====================================================================================================
  		// 모든 status 동기화 
  		error_status = GetMoimSyncData(connect_type, seller_id, "processing");
  		if(error_status.getError_code()>0) {
  			error_code = error_status.getError_code();
  			error_val = error_status.getError_val();
  		}
  		logger.info("getOrderListPossibleOrder processing GetMoimSyncData end"+"//"+seller_id); 	
  		//--------------------------------------------------------------------------		
  		String check = "";
  		List<String> data_id_list = new ArrayList<String>();
  	  	if(error_code == 0) {
  	  		check = moimApiService.OrderListGet(order_type, connect_type, seller_id, "", 0L, 0L);// 모두 가져오니 after = ""
  	  		if(check.startsWith("error:")==true) {
  	  			error_code = 113;
  	  			error_val = check;
  	  		}else {
  	  			order_get_domain = gson.fromJson(check, order_get_domain.getClass());
  	  	  		if(EmptyUtils.isEmpty(order_get_domain.getData())==false){
  		  	  		DefaultDomain.ErrorCheck error_status_1 = new DefaultDomain.ErrorCheck();
  					DefaultDomain.ErrorCheck error_status_2 = new DefaultDomain.ErrorCheck();
  					if(error_status_1.getError_code()==0) {
  						for(int j = 0; j < order_get_domain.getData().size(); j++) {
  		  					data_id_list.add(order_get_domain.getData().get(j).getId());
  		  				}
  		  	  			if(EmptyUtils.isEmpty(order_get_domain.getPaging().getAfter())==false) {
  		  	  				int total_cnt = order_get_domain.getPaging().getTotal();
  		  					int after_cnt = Integer.parseInt(order_get_domain.getPaging().getAfter());
  		  					if(total_cnt > after_cnt) {
  		  						int size_val = 0;
  		  						size_val = (int) Math.ceil((float)total_cnt / (float)after_cnt);
  		  						String after_val = "";
  		  						if(EmptyUtils.isEmpty(order_get_domain.getPaging().getAfter())==false) {
  		  							after_val = order_get_domain.getPaging().getAfter();	
  		  				  		}
  		  			  			if(EmptyUtils.isEmpty(after_val)==false) {
  		  			  				for(int kk = 0; kk < size_val; kk++) {
  		  			  					String check_2 = "";
  		  			  					check_2 = moimApiService.OrderListGet(order_type, connect_type, seller_id, after_val, 0L, 0L);
  		  			  					if(check_2.startsWith("error:")==true) {
		  		  			  	  			error_code = 113;
		  		  			  	  			error_val = check_2;
		  		  			  	  		}else {
		  		  			  	  			OrderGetDomain.GetData order_get_domain_2 = new OrderGetDomain.GetData();
			  			  					order_get_domain_2 = gson.fromJson(check_2, order_get_domain_2.getClass());
			  			  					if(EmptyUtils.isEmpty(order_get_domain_2.getData())==false){
												if(error_status_2.getError_code()>0) {
													error_code = error_status_2.getError_code();
													error_val = "RdsSyncProcess check_2 error : "+error_status_2.getError_val();
												}else {
													for(int j = 0; j < order_get_domain_2.getData().size(); j++) {
				  										data_id_list.add(order_get_domain_2.getData().get(j).getId());
				  										order_get_domain.getData().add(j, order_get_domain_2.getData().get(j));
				  									}
				  			  						if(EmptyUtils.isEmpty(order_get_domain_2.getPaging().getAfter())==false) {
				  										after_val = order_get_domain_2.getPaging().getAfter();
				  									}else {
				  										logger.info("getOrderListPossibleOrder after_val empty!!"+"//"+seller_id);
				  										break;
				  									}
			  			  						}
			  			  					}
		  		  			  	  		}
  		  			  				}
  		  			  			}
  		  					}
  		  	  			}
  					}else {
  						error_code = 112;
  			  			error_val = "processing RdsSyncProcess check error : "+error_status_1.getError_val()+"//"+seller_id;
  					}
  	  	  			
  	  	  		}
  	  		}
  		}
		
		//--------------------------------------------------------------------------
  		if(error_code==0) {
			Integer order_status = 0;// 0 : 주문, 1 : 가능, 2 : 발송, 3 : 취소
			if(order_type.equals("requested")==true) {// 주문 리스트
				order_status = 0;
			}else if(order_type.equals("processing")==true) {// 가능 리스트
				order_status = 1;
			}else if(order_type.equals("delivered")==true) {// 발송완료 리스트
				order_status = 2;
			}else if(order_type.equals("cancelled")==true) {// 취소 리스트
				order_status = 3;
			}
			logger.info("getOrderListPossibleOrder order_status:"+order_status+"//"+seller_id);
			
			List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
			List<OrderDomain.ReturnData> data_list = new ArrayList<OrderDomain.ReturnData>();
			Model model_get_1 = new ExtendedModelMap();
			model_get_1.addAttribute("seller_id", seller_id);
			model_get_1.addAttribute("order_sub_status", 0);
			model_get_1.addAttribute("order_status", order_status);
			model_get_1.addAttribute("data_id_list", data_id_list);
			model_get_1.addAttribute("sort", "asc");
			try {
				getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
				if(EmptyUtils.isEmpty(getPluginOrderList)==false) {
					if(getPluginOrderList.size()>0) {
						String last_order_key = "";
						OrderDomain.ReturnData returnDataSingle = new OrderDomain.ReturnData();
						List<OrderDomain.ProductSingle> product_single_list = new ArrayList<OrderDomain.ProductSingle>();
						for(int j = 0; j < getPluginOrderList.size(); j++) {
							if(getPluginOrderList.get(j).getPurchaseId().equals(last_order_key)==false) {
								returnDataSingle = new OrderDomain.ReturnData();
								returnDataSingle.setOrder_key(getPluginOrderList.get(j).getPurchaseId());
								returnDataSingle.setUser_id(getPluginOrderList.get(j).getUser_id());
								returnDataSingle.setUser_name(getPluginOrderList.get(j).getBuyer_name());
								returnDataSingle.setPay_date(getPluginOrderList.get(j).getPaid_date());
								product_single_list = new ArrayList<OrderDomain.ProductSingle>();
								
								String last_product_key = "";
								for(int jj = 0; jj < getPluginOrderList.size(); jj++) {
									if(getPluginOrderList.get(jj).getPurchaseId().equals(getPluginOrderList.get(j).getPurchaseId())==true) {
										if(last_product_key.equals(getPluginOrderList.get(jj).getProductId())==false){
											OrderDomain.ProductSingle product_single = new OrderDomain.ProductSingle();
											product_single.setProduct_key(getPluginOrderList.get(jj).getProductId());
											product_single.setProduct_name(getPluginOrderList.get(jj).getProduct_name());
											List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
											for(int ii = 0; ii < getPluginOrderList.size(); ii++) {
												if(getPluginOrderList.get(jj).getPurchaseId().equals(getPluginOrderList.get(ii).getPurchaseId())==true) {
													if(getPluginOrderList.get(jj).getProductId().equals(getPluginOrderList.get(ii).getProductId())==true) {
														OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
														pruduct_variant_single.setProduct_variant_id(getPluginOrderList.get(ii).getProduct_variant_id());
														pruduct_variant_single.setProduct_variant_value(getPluginOrderList.get(ii).getProduct_variant_name());
														pruduct_variant_single.setQuantity(getPluginOrderList.get(ii).getProduct_variant_quantity());
														product_variant_single_list.add(pruduct_variant_single);															
													}
												}
											}
											product_single.setProduct_variant_list(product_variant_single_list);
											String buyer_name = "";
											String buyer_phone = "";
											String recipient_name = "";
											String recipient_phone = "";
											String zipcode = "";
											String address = "";
											String memo = "";
											if(order_get_domain.getData().size()>0) {
												for(int k = 0; k < order_get_domain.getData().size(); k++) {
													if(order_get_domain.getData().get(k).getPurchaseId().equals(getPluginOrderList.get(jj).getPurchaseId())==true) {
														buyer_name = order_get_domain.getData().get(k).getPurchase().getBuyerName();
														buyer_phone = order_get_domain.getData().get(k).getPurchase().getBuyerPhone();
														recipient_name = order_get_domain.getData().get(k).getPurchase().getRecipientName();
														recipient_phone = order_get_domain.getData().get(k).getPurchase().getRecipientPhone();
														zipcode = order_get_domain.getData().get(k).getPurchase().getZipcode();
														address = order_get_domain.getData().get(k).getPurchase().getAddress()+order_get_domain.getData().get(k).getPurchase().getAddress2();
														memo = order_get_domain.getData().get(k).getPurchase().getMemo();
													}
												}
											}
											product_single.setBuyer_name(buyer_name);
											product_single.setBuyer_phone(buyer_phone);
											product_single.setRecipient_name(recipient_name);
											product_single.setRecipient_phone(recipient_phone);
											product_single.setRecipient_zipcode(zipcode);
											product_single.setRecipient_address(address);
											product_single.setRecipient_memo(memo);
											product_single_list.add(product_single);	
										}
										last_product_key = getPluginOrderList.get(jj).getProductId();	
									}
								}
								returnDataSingle.setProduct_list(product_single_list);	
								data_list.add(returnDataSingle);	
							}
							last_order_key = getPluginOrderList.get(j).getPurchaseId();//주문 id
						}
						
						SetDomain.MainMenuCount total_count = new SetDomain.MainMenuCount();
						Model model_count = new ExtendedModelMap();
						model_count.addAttribute("seller_id", seller_id);
						model_count.addAttribute("view_type", "possible");
						total_count = orderService.getPluginMainCnt(model_count);
						order_product_total_count = total_count.getPossible_order_cnt();
						
						order_list.setData(data_list);
						order_list.setLast_idx(null);
						order_list.setOrder_product_total_count(order_product_total_count);
						order_list.setOrder_product_cnt(null);
					}
				}
			} catch (Exception e) {
				error_code = 114;
				error_val = "processing getPluginOrderProductList error : e : "+e;
			}
		}
		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		return order_list;
	}
	

	/**
	 * @desc  
	 */
	public OrderDomain getObservListV2(Model model, String order_type, String connect_type
			, String seller_id) {
		
		//==========================================================
		// 관망중 리스트 order_type : observ
		//==========================================================
		int error_code = 0;
		String error_val = "";
		
		OrderDomain order_list = new OrderDomain();
		List<ReturnDatav1> datav2 = new ArrayList<ReturnDatav1>();
		OrderGetDomain.GetData order_get_domain = new OrderGetDomain.GetData();
		List<productVarinatSingle> moim_get_data_list = new ArrayList<productVarinatSingle>();
		int order_new_cnt = 0;
		int order_poss_cnt = 0;
		int order_product_total_count = 0;
		
		List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
		List<String> data_id_list = new ArrayList<String>();
		Model model_get_1 = new ExtendedModelMap();
		model_get_1.addAttribute("seller_id", seller_id);
		model_get_1.addAttribute("order_sub_status", 1);
		model_get_1.addAttribute("sort", "asc");
		try {
			getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
			if(EmptyUtils.isEmpty(getPluginOrderList)==false) {
				for(int j = 0; j < getPluginOrderList.size(); j++) {
					if(EmptyUtils.isEmpty(getPluginOrderList.get(j).getData_id())==false) {
						data_id_list.add(getPluginOrderList.get(j).getData_id());	
					}
				}
			}
		} catch (Exception e) {
			error_code = 114;
			error_val = "getObservListV2 getPluginOrderProductList error : e : "+e;
		}
		
		String check = "";
		if(data_id_list.size()>0) {
			check = moimApiService.OrderSingleGet(connect_type, seller_id, data_id_list);
	  		logger.info("getObservListV2 check:"+check+"//"+seller_id);
	  		if(check.startsWith("error:")==true) {
	  			error_code = 113;
	  			error_val = check;
	  		}else {
	  			order_get_domain = gson.fromJson(check, order_get_domain.getClass());
	  			if(EmptyUtils.isEmpty(order_get_domain)==false) {
	  				moim_get_data_list.addAll(order_get_domain.getData());
	  			}
	  		}				
		}
		
		if(error_code==0){
			String last_poduct_key = "";
			Set<String> added_product_key_set = new HashSet<String>();
			
			for(int j = 0; j < moim_get_data_list.size(); j++) {
				try {
		  				String group_check = "";
	  	  				group_check = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
	  	  				
	  	  				if(added_product_key_set.contains(group_check)==true) {
	  	  					if(order_get_domain.getData().get(j).getStatus().equals("paid")== true  
								|| order_get_domain.getData().get(j).getStatus().equals("preparingForDelivery")==true) {
								order_new_cnt = order_new_cnt+1;
							}
							if(order_get_domain.getData().get(j).getStatus().equals("waitingToBePickedUp")==true  
									|| order_get_domain.getData().get(j).getStatus().equals("waitingForDeliveryReception")==true 
									|| order_get_domain.getData().get(j).getStatus().equals("deliveryReceptionFailed")==true) {
								
								order_poss_cnt = order_poss_cnt+1;
							}
	  	  					continue;
	  	  				}
	  	  				added_product_key_set.add(group_check);
	  	  				
		  				if(last_poduct_key.equals(group_check)==false) {
		  					ReturnDatav1 date_single = new ReturnDatav1();
		  					List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
		  					Set<String> addedCiIds = new HashSet<String>();
		  					
		  					date_single = new ReturnDatav1();
	  						date_single.setOrder_key(moim_get_data_list.get(j).getPaymentId());
	  						date_single.setOrder_status(moim_get_data_list.get(j).getStatus());
	  						date_single.setUser_id(moim_get_data_list.get(j).getUserId());
	  						date_single.setUser_name(moim_get_data_list.get(j).getPurchase().getBuyerName());
	  						date_single.setRecipient_name(moim_get_data_list.get(j).getPurchase().getRecipientName());
	  						date_single.setProduct_key(moim_get_data_list.get(j).getProductId());
	  						date_single.setProduct_name(moim_get_data_list.get(j).getProductName());
	  						date_single.setRecipient_phone(moim_get_data_list.get(j).getPurchase().getBuyerPhone());
	  						date_single.setRecipient_zipcode(moim_get_data_list.get(j).getPurchase().getZipcode());
	  						date_single.setRecipient_address(moim_get_data_list.get(j).getPurchase().getAddress()+" "+moim_get_data_list.get(j).getPurchase().getAddress2());
	  						String pay_date = "";
			  	  			if(EmptyUtils.isEmpty(moim_get_data_list.get(j).getPaidAt())==false) {
								if(moim_get_data_list.get(j).getPaidAt()>0) {
									pay_date = LocationTimeCal.TimeStamptoDate(moim_get_data_list.get(j).getPaidAt());
								}
							}
		  	  				date_single.setPay_date(pay_date);
			  	  			
			  	  			for (int i2= 0; i2 < moim_get_data_list.size(); i2++) {
			  	  				if(moim_get_data_list.get(j).getPurchaseId().equals(moim_get_data_list.get(i2).getPurchaseId())==true
			  	  						&& moim_get_data_list.get(j).getProductId().equals(moim_get_data_list.get(i2).getProductId())==true 
			  	  						) {
			  	  					
			  	  					String ciId = moim_get_data_list.get(i2).getId();
			  	  					if(addedCiIds.contains(ciId)==true) {
			  	  						continue;
			  	  					}
			  	  					addedCiIds.add(ciId);
			  	  					
					  	  			OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
					  	  			pruduct_variant_single.setProduct_variant_key(moim_get_data_list.get(i2).getId()); //CI:
			  						pruduct_variant_single.setProduct_variant_id(moim_get_data_list.get(i2).getProductVariantId());
			  					
			  						String var_val1 = "";
			  						String[] var_val_array1 = null;
			  						if(EmptyUtils.isEmpty(moim_get_data_list.get(i2).getProductVariantValue())==false) {
			  							var_val1 = moim_get_data_list.get(i2).getProductVariantValue().toString();
			  							var_val1 = var_val1.replaceAll("\\{", "");
			  							var_val1 = var_val1.replaceAll("\\}", "");
			  						}
			  						if(EmptyUtils.isEmpty(var_val1)==false) {
			  							var_val_array1 = var_val1.split("=");
			  							for(int ii = 0; ii < var_val_array1.length; ii++) {
			  								if(ii==2){
			  									if(EmptyUtils.isEmpty(var_val_array1[ii])==false) {
			  										var_val1 =	var_val_array1[ii];
			  									}
			  								}
			  							}
			  						}
									pruduct_variant_single.setProduct_variant_name(var_val1);
									pruduct_variant_single.setQuantity(moim_get_data_list.get(i2).getQuantity());
									product_variant_single_list.add(pruduct_variant_single);	
				  	  			}
			  	  			
			  	  			}
				  	  		date_single.setProduct_variant_list(product_variant_single_list);
		  					datav2.add(date_single);
		  				}
		  				
		  				if(order_get_domain.getData().get(j).getStatus().equals("paid")== true  
								|| order_get_domain.getData().get(j).getStatus().equals("preparingForDelivery")==true) {
							order_new_cnt = order_new_cnt+1;
						}
						if(order_get_domain.getData().get(j).getStatus().equals("waitingToBePickedUp")==true  
								|| order_get_domain.getData().get(j).getStatus().equals("waitingForDeliveryReception")==true 
								|| order_get_domain.getData().get(j).getStatus().equals("deliveryReceptionFailed")==true) {
							
							order_poss_cnt = order_poss_cnt+1;
						}
		  				last_poduct_key = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
					} catch (Exception e) {
					logger.info("getObservListV2 data_id_hold.indexOf(moim_get_data_list.get(j).getId()) : e :"+e+"//"+seller_id);
					error_code = 113;
		  			error_val = "getObservListV2 data_id_hold.indexOf(moim_get_data_list.get(j).getId()) : e : "+e+"//"+seller_id;
				}
			}
		}
		logger.info("getObservListV2 order_new_cnt:"+order_new_cnt+"//"+seller_id);
		logger.info("getObservListV2 order_poss_cnt:"+order_poss_cnt+"//"+seller_id);
		order_product_total_count = order_new_cnt+order_poss_cnt;
		order_list.setOrder_new_cnt(order_new_cnt);
		order_list.setOrder_poss_cnt(order_poss_cnt);
		order_list.setOrder_product_total_count(order_product_total_count);
		order_list.setDatav2(datav2);
		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		return order_list;
	}


	/**
	 * @desc  
	 */
	public OrderDomain getObservList(Model model, String order_type, String connect_type
			, String seller_id) {
		
		//==========================================================
		// 관망중 리스트 order_type : observ
		//==========================================================
		int error_code = 0;
		String error_val = "";
		
		OrderDomain order_list = new OrderDomain();
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
	    
	    //=====================================================================================================
  		error_status = GetMoimSyncData(connect_type, seller_id, "observ");
  		if(error_status.getError_code()>0) {
  			error_code = error_status.getError_code();
  			error_val = error_status.getError_val();
  		}
  		logger.info("getObservList GetMoimSyncData end"+"//"+seller_id); 	
		
		//--------------------------------------------------------------------------
  		if(error_code==0) {

			List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
			List<OrderDomain.ReturnData> data_list = new ArrayList<OrderDomain.ReturnData>();
			int order_product_total_count = 0;
			int order_new_cnt = 0;
			int order_poss_cnt = 0;
			
			Model model_get_1 = new ExtendedModelMap();
			model_get_1.addAttribute("seller_id", seller_id);
			model_get_1.addAttribute("order_sub_status", 1);
			model_get_1.addAttribute("sort", "asc");
			try {	
				getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
				if(EmptyUtils.isEmpty(getPluginOrderList)==false) {
					if(getPluginOrderList.size()>0) {
						String last_order_key = "";
						OrderDomain.ReturnData returnDataSingle = new OrderDomain.ReturnData();
						List<OrderDomain.ProductSingle> product_single_list = new ArrayList<OrderDomain.ProductSingle>();
						for(int j = 0; j < getPluginOrderList.size(); j++) {
							if(getPluginOrderList.get(j).getPurchaseId().equals(last_order_key)==false) {
								returnDataSingle = new OrderDomain.ReturnData();
								returnDataSingle.setOrder_key(getPluginOrderList.get(j).getPurchaseId());
								returnDataSingle.setUser_id(getPluginOrderList.get(j).getUser_id());
								returnDataSingle.setUser_name(getPluginOrderList.get(j).getBuyer_name());
								returnDataSingle.setPay_date(getPluginOrderList.get(j).getPaid_date());
								product_single_list = new ArrayList<OrderDomain.ProductSingle>();
							
								String last_product_key = "";
								for(int jj = 0; jj < getPluginOrderList.size(); jj++) {
									if(getPluginOrderList.get(jj).getPurchaseId().equals(getPluginOrderList.get(j).getPurchaseId())==true) {
										if(last_product_key.equals(getPluginOrderList.get(jj).getProductId())==false){
											OrderDomain.ProductSingle product_single = new OrderDomain.ProductSingle();
											product_single.setProduct_key(getPluginOrderList.get(jj).getProductId());
											product_single.setProduct_name(getPluginOrderList.get(jj).getProduct_name());
											product_single.setRecipient_name(getPluginOrderList.get(jj).getRecipient_name());
											List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
											for(int ii = 0; ii < getPluginOrderList.size(); ii++) {
												if(getPluginOrderList.get(jj).getPurchaseId().equals(getPluginOrderList.get(ii).getPurchaseId())==true) {
													if(getPluginOrderList.get(jj).getProductId().equals(getPluginOrderList.get(ii).getProductId())==true) {
														OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
														pruduct_variant_single.setProduct_variant_id(getPluginOrderList.get(ii).getProduct_variant_id());
														pruduct_variant_single.setProduct_variant_value(getPluginOrderList.get(ii).getProduct_variant_name());
														pruduct_variant_single.setQuantity(getPluginOrderList.get(ii).getProduct_variant_quantity());
														product_variant_single_list.add(pruduct_variant_single);
														if(getPluginOrderList.get(ii).getOrder_status()==0) {
															order_new_cnt = order_new_cnt + 1;
														}else if(getPluginOrderList.get(ii).getOrder_status()==1) {
															order_poss_cnt = order_poss_cnt + 1;
														}	
													}
												}
											}
											product_single.setProduct_variant_list(product_variant_single_list);
											product_single_list.add(product_single);	
										}
										last_product_key = getPluginOrderList.get(jj).getProductId();	
									}
								}
								returnDataSingle.setProduct_list(product_single_list);	
								data_list.add(returnDataSingle);	
							}
							last_order_key = getPluginOrderList.get(j).getPurchaseId();//주문 id : group으로 묶어야 해서 필요
							logger.info("getObservList add last_order_key:"+last_order_key+"//"+seller_id);
						}
						
						SetDomain.MainMenuCount total_count = new SetDomain.MainMenuCount();
						Model model_count = new ExtendedModelMap();
						model_count.addAttribute("seller_id", seller_id);
						model_count.addAttribute("view_type", "observ");
						total_count = orderService.getPluginMainCnt(model_count);
						order_product_total_count = total_count.getObserv_order_cnt();
						
						order_list.setData(data_list);
						order_list.setLast_idx(null);  
						order_list.setOrder_product_total_count(order_product_total_count);
						order_list.setOrder_product_cnt(null);
						
						order_list.setOrder_new_cnt(order_new_cnt);
						order_list.setOrder_poss_cnt(order_poss_cnt);
					}
				}
			} catch (Exception e) {
				error_code = 114;
				error_val = "observ getPluginOrderProductList error : e : "+e;
			}
		}
		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		return order_list;
	}


	/**
	 * @desc  
	 */
	// 취소 리스트 v2 : 상품 기준
	public OrderDomain getOrderListCancelledV2(Model model, String order_type, String connect_type
			, String seller_id, OrderBodyDomain.ParamBody param_body) {
		
		//==========================================================
		// 취소된 주문 리스트 order_type : cancelled
		//==========================================================
		int error_code = 0;
		String error_val = "";
		
		OrderDomain order_list = new OrderDomain();
		OrderGetDomain.GetData order_get_domain = new OrderGetDomain.GetData();
		List<ReturnDatav1> datav2 = new ArrayList<ReturnDatav1>();
		String after = "";
		String last_after = "";
		int order_product_total_count = 0;
		Long start_val = 0L;
		Long end_val = 0L;
		String start_date = "";
		String end_date = "";
		List<productVarinatSingle> moim_get_data_list = new ArrayList<productVarinatSingle>();
		
		if(EmptyUtils.isEmpty(param_body.getStart_date())==false
				&& EmptyUtils.isEmpty(param_body.getEnd_date())==false) {
			start_date = param_body.getStart_date()+"T00:00:00.000Z";
			end_date = param_body.getEnd_date()+"T23:59:59.999Z";
			start_date = LocationTimeCal.CalDatePlus(start_date, 9);  
			end_date = LocationTimeCal.CalDatePlus(end_date, 9);  
			logger.info("getOrderListCancelledV2 start_date:"+start_date+"//"+seller_id);
			logger.info("getOrderListCancelledV2 end_date:"+end_date+"//"+seller_id);	
			start_val = LocationTimeCal.DatetoTimeStamp(start_date);
			end_val = LocationTimeCal.DatetoTimeStamp(end_date);
			logger.info("getOrderListCancelledV2 start_val:"+start_val+"//"+seller_id);
			logger.info("getOrderListCancelledV2 end_val:"+end_val+"//"+seller_id);	
		}
		if(EmptyUtils.isEmpty(param_body.getLast_idx())==false) {
			try {
				logger.info("getOrderListCancelledV2 param_body.getLast_idx(:"+param_body.getLast_idx()+"//"+seller_id);
				after = CryptoUtil.decrypt(defaultConfig.getLastIdxSeed(), param_body.getLast_idx());	
				logger.info("getOrderListCancelledV2 after enc :"+after+"//"+seller_id);
			} catch (Exception e) {
				logger.info("getOrderListCancelledV2 CryptoUtil.encrypt : "+param_body.getLast_idx()+" : e:"+e+"//"+seller_id);
			}
		}
		
		try {
			String check = "";
	  		check = moimApiService.OrderListGet(order_type, connect_type, seller_id, after, start_val, end_val);
	  		logger.info("getOrderListCancelledV2>OrderListGet:check:"+check+"//"+seller_id);
	  		if(check.startsWith("error:")==true) {
	  			error_code = 113;
	  			error_val = check;
	  		}else {
	  			order_get_domain = gson.fromJson(check, order_get_domain.getClass());
	  			if(EmptyUtils.isEmpty(order_get_domain)==false) {
	  				moim_get_data_list.addAll(order_get_domain.getData());
	  			}
	  			if(EmptyUtils.isEmpty(order_get_domain.getPaging())==false) {
	  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getTotal())==false){
	  					order_product_total_count = order_get_domain.getPaging().getTotal();
	  				}else {
	  					order_product_total_count = 0;
	  				}
	  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getAfter())==false){
	  					after = order_get_domain.getPaging().getAfter();
	  				}else {
	  					after = "";
	  				}
	  			}
	  		}
		} catch (Exception e) {
			logger.info("getOrderListCancelledV2 OrderListGet : e :"+e+"//"+seller_id);
			error_code = 113;
  			error_val = "getOrderListCancelledV2 OrderListGet : e : "+e+"//"+seller_id;
		}
  		if(error_code==0){
  			String last_poduct_key = "";
  			for(int j = 0; j < moim_get_data_list.size(); j++) {
  				try {
  					String group_check = "";
  	  				group_check = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
	  				if(last_poduct_key.equals(group_check)==false) {
	  					ReturnDatav1 date_single = new ReturnDatav1();
	  					date_single = new ReturnDatav1();
  						date_single.setOrder_key(moim_get_data_list.get(j).getPaymentId());
  						date_single.setOrder_status(moim_get_data_list.get(j).getStatus());
  						date_single.setUser_id(moim_get_data_list.get(j).getUserId());
  						date_single.setUser_name(moim_get_data_list.get(j).getPurchase().getBuyerName());
  						date_single.setRecipient_name(moim_get_data_list.get(j).getPurchase().getRecipientName());
  						date_single.setProduct_key(moim_get_data_list.get(j).getProductId());
  						date_single.setProduct_name(moim_get_data_list.get(j).getProductName());
  						String pay_date = "";
		  	  			if(EmptyUtils.isEmpty(moim_get_data_list.get(j).getPaidAt())==false) {
							if(moim_get_data_list.get(j).getPaidAt()>0) {
								pay_date = LocationTimeCal.TimeStamptoDate(moim_get_data_list.get(j).getPaidAt());
							}
						}
	  	  				date_single.setPay_date(pay_date);
	  	  				List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
		  	  			for (int i2= 0; i2 < moim_get_data_list.size(); i2++) {
		  	  				if(moim_get_data_list.get(j).getPaymentId().equals(moim_get_data_list.get(i2).getPaymentId())==true
		  	  						&& moim_get_data_list.get(j).getProductId().equals(moim_get_data_list.get(i2).getProductId())==true 
		  	  						) {
		  	  					OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
				  	  			pruduct_variant_single.setProduct_variant_key(moim_get_data_list.get(i2).getId()); //CI:
		  						pruduct_variant_single.setProduct_variant_id(moim_get_data_list.get(i2).getProductVariantId());
		  						String var_val1 = "";
		  						String[] var_val_array1 = null;
		  						if(EmptyUtils.isEmpty(moim_get_data_list.get(i2).getProductVariantValue())==false) {
		  							var_val1 = moim_get_data_list.get(i2).getProductVariantValue().toString();
		  							var_val1 = var_val1.replaceAll("\\{", "");
		  							var_val1 = var_val1.replaceAll("\\}", "");
		  						}
		  						if(EmptyUtils.isEmpty(var_val1)==false) {
		  							var_val_array1 = var_val1.split("=");
		  							for(int ii = 0; ii < var_val_array1.length; ii++) {
		  								if(ii==2){
		  									if(EmptyUtils.isEmpty(var_val_array1[ii])==false) {
		  										var_val1 =	var_val_array1[ii];
		  									}
		  								}
		  							}
		  						}
								pruduct_variant_single.setProduct_variant_name(var_val1);
								pruduct_variant_single.setQuantity(moim_get_data_list.get(i2).getQuantity());
								product_variant_single_list.add(pruduct_variant_single);	
			  	  			}
		  	  			}
		  	  		date_single.setProduct_variant_list(product_variant_single_list);
  					datav2.add(date_single);
	  				}
	  				last_poduct_key = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
  				} catch (Exception e) {
					logger.info("getOrderListCancelledV2 for(int j = 0; j < moim_get_data_list : e :"+e+"//"+seller_id);
					error_code = 113;
		  			error_val = "getOrderListCancelledV2 for(int j = 0; j < moim_get_data_list : e : "+e+"//"+seller_id;
				}
  			}
  		}
  		if(EmptyUtils.isEmpty(after)==false) {
  			try {
				logger.info("getOrderListCancelledV2 after:"+after+"//"+seller_id);
				last_after = CryptoUtil.encrypt(defaultConfig.getLastIdxSeed(), after);	
				logger.info("getOrderListCancelledV2 last_after enc :"+last_after+"//"+seller_id);
			} catch (Exception e) {
				logger.info("getOrderListCancelledV2 CryptoUtil.encrypt : "+after+" : e:"+e+"//"+seller_id);
			}
  		}
  		order_list.setLast_idx(last_after);
		order_list.setOrder_product_total_count(order_product_total_count);
  		order_list.setDatav2(datav2);
		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		return order_list;
	}
		

	/**
	 * @desc  
	 */
	// 취소 리스트 v1 : 주문 기준
	public OrderDomain getOrderListCancelled(Model model, String order_type, String connect_type
			, String seller_id, OrderBodyDomain.ParamBody param_body) {
		
		//==========================================================
		// 취소된 주문 리스트 order_type : cancelled
		//==========================================================
		int error_code = 0;
		String error_val = "";
		
		OrderDomain order_list = new OrderDomain();
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		int order_product_total_count = 0;
		int search_product_total_count = 0;
		
		if(EmptyUtils.isEmpty(param_body.getLast_idx())==true) {
			//=====================================================================================================
			error_status = GetMoimSyncData(connect_type, seller_id, "cancelled");
			if(error_status.getError_code()>0) {
				error_code = error_status.getError_code();
				error_val = error_status.getError_val();
			}
			logger.info("getOrderListCancelled GetMoimSyncData end"+"//"+seller_id); 	
			//=====================================================================================================			
		}

		// 리턴할  결과값 만들기 
		if(error_code==0) {
			Integer order_status = 0;// 0 : 주문, 1 : 가능, 2 : 발송, 3 : 취소
			if(order_type.equals("requested")==true) {// 주문 리스트
				order_status = 0;
			}else if(order_type.equals("processing")==true) {// 가능 리스트
				order_status = 1;
			}else if(order_type.equals("delivered")==true) {// 발송 완료 리스트
				order_status = 2;
			}else if(order_type.equals("cancelled")==true) {// 취소 주문 리스트
				order_status = 3;
			}
			logger.info("getOrderListCancelled order_status:"+order_status+"//"+seller_id);
			
			String last_idx = "";
			String start_date = "";
			String end_date = "";
			String search_keyword = "";
			if(EmptyUtils.isEmpty(param_body.getStart_date())==false
					|| EmptyUtils.isEmpty(param_body.getEnd_date())==false
					|| EmptyUtils.isEmpty(param_body.getSearch_keyword())==false
					){
				if(EmptyUtils.isEmpty(param_body.getStart_date())==false
						&& EmptyUtils.isEmpty(param_body.getEnd_date())==false) {
					start_date = param_body.getStart_date();
					end_date = param_body.getEnd_date();
				}
				if(EmptyUtils.isEmpty(param_body.getSearch_keyword())==false) {
					search_keyword = param_body.getSearch_keyword(); 
				}
			}
			if(EmptyUtils.isEmpty(param_body.getLast_idx())==false) {
				String dec_last_idx;
				try {
					dec_last_idx = CryptoUtil.decrypt(defaultConfig.getLastIdxSeed(), param_body.getLast_idx());
					if(EmptyUtils.isEmpty(dec_last_idx)==false) {
						last_idx = dec_last_idx;
					}else {
						last_idx = "";
					}	
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			logger.info("getOrderListCancelled start_date:"+start_date+"//"+seller_id);
			logger.info("getOrderListCancelled end_date:"+end_date+"//"+seller_id);
			logger.info("getOrderListCancelled search_keyword:"+search_keyword+"//"+seller_id);
			logger.info("getOrderListCancelled last_idx:"+last_idx+"//"+seller_id);
			
			if(EmptyUtils.isEmpty(start_date)==false) {
				List<OrderDomain.PluginOrderListDomain> getPluginOrderGroupListSearchType = new ArrayList<OrderDomain.PluginOrderListDomain>();
				Model model_group_search = new ExtendedModelMap();
				model_group_search.addAttribute("seller_id", seller_id);
				model_group_search.addAttribute("order_status", order_status);
				model_group_search.addAttribute("start_date", start_date);
				model_group_search.addAttribute("end_date", end_date);
				model_group_search.addAttribute("search_type", 1);
				
				getPluginOrderGroupListSearchType = orderService.getPluginOrderGroupList(model_group_search);
				
				if(getPluginOrderGroupListSearchType.get(0).getSearch_group_cnt()>0) {
					search_product_total_count = getPluginOrderGroupListSearchType.get(0).getSearch_group_cnt();
				}	
			}
			
			List<String> paymentId_list = new ArrayList<String>();
			List<OrderDomain.PluginOrderListDomain> getPluginOrderGroupList = new ArrayList<OrderDomain.PluginOrderListDomain>();
			Model model_group = new ExtendedModelMap();
			model_group.addAttribute("seller_id", seller_id);
			model_group.addAttribute("order_status", order_status);
			model_group.addAttribute("start_date", start_date);
			model_group.addAttribute("end_date", end_date);
			model_group.addAttribute("search_keyword", search_keyword);
			model_group.addAttribute("last_idx", last_idx);
			model_group.addAttribute("sort", "desc");
			model_group.addAttribute("limit_val", defaultConfig.getListLimit());
			
			getPluginOrderGroupList = orderService.getPluginOrderGroupList(model_group);	
			
			String last_paymentid = "";
			if(EmptyUtils.isEmpty(getPluginOrderGroupList)==false) {
				if(getPluginOrderGroupList.size()>0) {
					if(getPluginOrderGroupList.size()>0) {
						for(int i = 0; i < getPluginOrderGroupList.size(); i++) {
							paymentId_list.add(getPluginOrderGroupList.get(i).getPaymentId());
							last_paymentid = getPluginOrderGroupList.get(i).getPaymentId();
						}	
					}
				}
			}
			
			if(paymentId_list.size()>0) {
				try {
					List<OrderDomain.PluginOrderListDomain> getPluginOrderProductList = new ArrayList<OrderDomain.PluginOrderListDomain>();
					List<OrderDomain.ReturnData> data_list = new ArrayList<OrderDomain.ReturnData>();
					Model model_get_1 = new ExtendedModelMap();
					model_get_1.addAttribute("seller_id", seller_id);
					model_get_1.addAttribute("order_status", order_status); 
					model_get_1.addAttribute("sort", "desc");
					model_get_1.addAttribute("paymentId_list", paymentId_list);
					getPluginOrderProductList = orderService.getPluginOrderProductList(model_get_1);
					
					if(EmptyUtils.isEmpty(getPluginOrderProductList)==false) {
						if(getPluginOrderProductList.size()>0) {
							String last_order_key = "";
							OrderDomain.ReturnData returnDataSingle = new OrderDomain.ReturnData();
							List<OrderDomain.ProductSingle> product_single_list = new ArrayList<OrderDomain.ProductSingle>();
							for(int j = 0; j < getPluginOrderProductList.size(); j++) {
								if(getPluginOrderProductList.get(j).getPurchaseId().equals(last_order_key)==false) {
									returnDataSingle = new OrderDomain.ReturnData();
									returnDataSingle.setOrder_key(getPluginOrderProductList.get(j).getPurchaseId());
									returnDataSingle.setUser_id(getPluginOrderProductList.get(j).getUser_id());
									returnDataSingle.setUser_name(getPluginOrderProductList.get(j).getRecipient_name());
									returnDataSingle.setPay_date(getPluginOrderProductList.get(j).getPaid_date());
									product_single_list = new ArrayList<OrderDomain.ProductSingle>();
									
									String last_product_key = "";
									for(int jj = 0; jj < getPluginOrderProductList.size(); jj++) {
										if(getPluginOrderProductList.get(jj).getPurchaseId().equals(getPluginOrderProductList.get(j).getPurchaseId())==true) {
											if(last_product_key.equals(getPluginOrderProductList.get(jj).getProductId())==false){
												OrderDomain.ProductSingle product_single = new OrderDomain.ProductSingle();
												product_single.setProduct_key(getPluginOrderProductList.get(jj).getProductId());
												product_single.setProduct_name(getPluginOrderProductList.get(jj).getProduct_name());
												product_single.setRecipient_name(getPluginOrderProductList.get(jj).getRecipient_name());
												
												List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
												for(int ii = 0; ii < getPluginOrderProductList.size(); ii++) {
													if(getPluginOrderProductList.get(jj).getPurchaseId().equals(getPluginOrderProductList.get(ii).getPurchaseId())==true) {
														if(getPluginOrderProductList.get(jj).getProductId().equals(getPluginOrderProductList.get(ii).getProductId())==true) {
															OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
															pruduct_variant_single.setProduct_variant_id(getPluginOrderProductList.get(ii).getProduct_variant_id());
															pruduct_variant_single.setProduct_variant_value(getPluginOrderProductList.get(ii).getProduct_variant_name());
															pruduct_variant_single.setQuantity(getPluginOrderProductList.get(ii).getProduct_variant_quantity());
															product_variant_single_list.add(pruduct_variant_single);															
														}
													}
												}
												product_single.setProduct_variant_list(product_variant_single_list);
												product_single_list.add(product_single);	
											}
											last_product_key = getPluginOrderProductList.get(jj).getProductId();	
										}
									}
									returnDataSingle.setProduct_list(product_single_list);	
									data_list.add(returnDataSingle);	
								}
								
								last_order_key = getPluginOrderProductList.get(j).getPurchaseId();
							}

							SetDomain.MainMenuCount total_count = new SetDomain.MainMenuCount();
							Model model_count = new ExtendedModelMap();
							model_count.addAttribute("seller_id", seller_id);
							model_count.addAttribute("view_type", "cancelled");
							total_count = orderService.getPluginMainCnt(model_count);
							order_product_total_count = total_count.getCancel_order_cnt();
							try {
								last_paymentid = CryptoUtil.encrypt(defaultConfig.getLastIdxSeed(), last_paymentid);
							} catch (Exception e) {
								e.printStackTrace();
							}
							order_list.setData(data_list);
							order_list.setLast_idx(last_paymentid);
							order_list.setOrder_product_total_count(order_product_total_count);
							order_list.setSearch_product_total_count(search_product_total_count); 
							order_list.setOrder_product_cnt(null);
						}
					}
				} catch (Exception e) {
					error_code = 114;
					error_val = "cancelled getPluginOrderProductList error : e : "+e;
				}
			}
		}
		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		return order_list;
	}

	/**
	 * @desc  
	 */
	public OrderDomain getOrderListDeilveredV2(Model model, String order_type, String connect_type
			, String seller_id, OrderBodyDomain.ParamBody param_body) {

		//==========================================================
		// 발송 완료 리스트 order_type : delivered
		//==========================================================
		int error_code = 0;
		String error_val = "";
		
		OrderDomain order_list = new OrderDomain();
		OrderGetDomain.GetData order_get_domain = new OrderGetDomain.GetData();
		List<ReturnDatav1> datav2 = new ArrayList<ReturnDatav1>();
		String after = "";
		String last_after = "";
		int order_product_total_count = 0;
		Long start_val = 0L;
		Long end_val = 0L;
		String start_date = "";
		String end_date = "";
		List<productVarinatSingle> moim_get_data_list = new ArrayList<productVarinatSingle>();
		
		if(EmptyUtils.isEmpty(param_body.getStart_date())==false
				&& EmptyUtils.isEmpty(param_body.getEnd_date())==false) {
			start_date = param_body.getStart_date()+"T00:00:00.000Z";
			end_date = param_body.getEnd_date()+"T23:59:59.999Z";
			start_date = LocationTimeCal.CalDatePlus(start_date, 9);  
			end_date = LocationTimeCal.CalDatePlus(end_date, 9);  
			logger.info("getOrderListDeilveredV2 start_date:"+start_date+"//"+seller_id);
			logger.info("getOrderListDeilveredV2 end_date:"+end_date+"//"+seller_id);	
			start_val = LocationTimeCal.DatetoTimeStamp(start_date);
			end_val = LocationTimeCal.DatetoTimeStamp(end_date);
			logger.info("getOrderListDeilveredV2 start_val:"+start_val+"//"+seller_id);
			logger.info("getOrderListDeilveredV2 end_val:"+end_val+"//"+seller_id);	
		}
		if(EmptyUtils.isEmpty(param_body.getLast_idx())==false) {
			try {
				logger.info("getOrderListDeilveredV2 param_body.getLast_idx(:"+param_body.getLast_idx()+"//"+seller_id);
				after = CryptoUtil.decrypt(defaultConfig.getLastIdxSeed(), param_body.getLast_idx());	
				logger.info("getOrderListDeilveredV2 after enc :"+after+"//"+seller_id);
			} catch (Exception e) {
				logger.info("getOrderListDeilveredV2 CryptoUtil.encrypt : "+param_body.getLast_idx()+" : e:"+e+"//"+seller_id);
			}
		}
		
		try {
			String check = "";
	  		check = moimApiService.OrderListGet(order_type, connect_type, seller_id, after, start_val, end_val);
	  		logger.info("getOrderListDeilveredV2>OrderListGet:check:"+check+"//"+seller_id);
	  		if(check.startsWith("error:")==true) {
	  			error_code = 113;
	  			error_val = check;
	  		}else {
	  			order_get_domain = gson.fromJson(check, order_get_domain.getClass());
	  			if(EmptyUtils.isEmpty(order_get_domain)==false) {
	  				moim_get_data_list.addAll(order_get_domain.getData());
	  			}
	  			if(EmptyUtils.isEmpty(order_get_domain.getPaging())==false) {
	  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getTotal())==false){
	  					order_product_total_count = order_get_domain.getPaging().getTotal();
	  				}else {
	  					order_product_total_count = 0;
	  				}
	  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getAfter())==false){
	  					after = order_get_domain.getPaging().getAfter();
	  				}else {
	  					after = "";
	  				}
	  			}
	  		}
		} catch (Exception e) {
			logger.info("getOrderListDeilveredV2 OrderListGet : e :"+e+"//"+seller_id);
			error_code = 113;
  			error_val = "getOrderListDeilveredV2 OrderListGet : e : "+e+"//"+seller_id;
		}
  		if(error_code==0){
  			String last_poduct_key = "";
  			for(int j = 0; j < moim_get_data_list.size(); j++) {
  				try {
  					String group_check = "";
  	  				group_check = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
	  				if(last_poduct_key.equals(group_check)==false) {
	  					ReturnDatav1 date_single = new ReturnDatav1();
	  					date_single = new ReturnDatav1();
  						date_single.setOrder_key(moim_get_data_list.get(j).getPaymentId());
  						date_single.setOrder_status(moim_get_data_list.get(j).getStatus());
  						date_single.setUser_id(moim_get_data_list.get(j).getUserId());
  						date_single.setUser_name(moim_get_data_list.get(j).getPurchase().getBuyerName());
  						date_single.setRecipient_name(moim_get_data_list.get(j).getPurchase().getRecipientName());
  						date_single.setProduct_key(moim_get_data_list.get(j).getProductId());
  						date_single.setProduct_name(moim_get_data_list.get(j).getProductName());
  						String pay_date = "";
		  	  			if(EmptyUtils.isEmpty(moim_get_data_list.get(j).getPaidAt())==false) {
							if(moim_get_data_list.get(j).getPaidAt()>0) {
								pay_date = LocationTimeCal.TimeStamptoDate(moim_get_data_list.get(j).getPaidAt());
							}
						}
	  	  				date_single.setPay_date(pay_date);
	  	  				List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
		  	  			for (int i2= 0; i2 < moim_get_data_list.size(); i2++) {
		  	  				if(moim_get_data_list.get(j).getPaymentId().equals(moim_get_data_list.get(i2).getPaymentId())==true
		  	  						&& moim_get_data_list.get(j).getProductId().equals(moim_get_data_list.get(i2).getProductId())==true 
		  	  						) {
		  	  					OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
				  	  			pruduct_variant_single.setProduct_variant_key(moim_get_data_list.get(i2).getId()); //CI:
		  						pruduct_variant_single.setProduct_variant_id(moim_get_data_list.get(i2).getProductVariantId());
		  						String var_val1 = "";
		  						String[] var_val_array1 = null;
		  						if(EmptyUtils.isEmpty(moim_get_data_list.get(i2).getProductVariantValue())==false) {
		  							var_val1 = moim_get_data_list.get(i2).getProductVariantValue().toString();
		  							var_val1 = var_val1.replaceAll("\\{", "");
		  							var_val1 = var_val1.replaceAll("\\}", "");
		  						}
		  						if(EmptyUtils.isEmpty(var_val1)==false) {
		  							var_val_array1 = var_val1.split("=");
		  							for(int ii = 0; ii < var_val_array1.length; ii++) {
		  								if(ii==2){
		  									if(EmptyUtils.isEmpty(var_val_array1[ii])==false) {
		  										var_val1 =	var_val_array1[ii];
		  									}
		  								}
		  							}
		  						}
								pruduct_variant_single.setProduct_variant_name(var_val1);
								pruduct_variant_single.setQuantity(moim_get_data_list.get(i2).getQuantity());
								product_variant_single_list.add(pruduct_variant_single);	
			  	  			}
		  	  			}
		  	  		date_single.setProduct_variant_list(product_variant_single_list);
  					datav2.add(date_single);
	  				}
	  				last_poduct_key = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
  				} catch (Exception e) {
					logger.info("getOrderListDeilveredV2 for(int j = 0; j < moim_get_data_list : e :"+e+"//"+seller_id);
					error_code = 113;
		  			error_val = "getOrderListDeilveredV2 for(int j = 0; j < moim_get_data_list : e : "+e+"//"+seller_id;
				}
  			}
  		}
  		if(EmptyUtils.isEmpty(after)==false) {
  			try {
				logger.info("getOrderListDeilveredV2 after:"+after+"//"+seller_id);
				last_after = CryptoUtil.encrypt(defaultConfig.getLastIdxSeed(), after);	
				logger.info("getOrderListDeilveredV2 last_after enc :"+last_after+"//"+seller_id);
			} catch (Exception e) {
				logger.info("getOrderListDeilveredV2 CryptoUtil.encrypt : "+after+" : e:"+e+"//"+seller_id);
			}
  		}
  		order_list.setLast_idx(last_after);
		order_list.setOrder_product_total_count(order_product_total_count);
  		order_list.setDatav2(datav2);		
		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		return order_list;
	}
	

	/**
	 * @desc  
	 */
	public OrderDomain getOrderListDeilvered(Model model, String order_type, String connect_type
			, String seller_id, OrderBodyDomain.ParamBody param_body) {

		//==========================================================
		// 발송 완료 리스트 order_type : delivered
		//==========================================================
		
		int error_code = 0;
		String error_val = "";
		
		OrderDomain order_list = new OrderDomain();
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		int order_product_total_count = 0;
		int search_product_total_count = 0;
		
		if(EmptyUtils.isEmpty(param_body.getLast_idx())==true) {
			//=====================================================================================================
			error_status = GetMoimSyncData(connect_type, seller_id, "delivered");
			if(error_status.getError_code()>0) {
				error_code = error_status.getError_code();
				error_val = error_status.getError_val();
			}
			logger.info("getOrderListDeilvered GetMoimSyncData end"+"//"+seller_id); 	
			//=====================================================================================================			
		}

		if(error_code==0) {
			Integer order_status = 0;// 0 : 주문, 1 : 가능, 2 : 발송, 3 : 취소
			if(order_type.equals("requested")==true) {// 주문 리스트
				order_status = 0;
			}else if(order_type.equals("processing")==true) {// 가능리스트
				order_status = 1;
			}else if(order_type.equals("delivered")==true) {// 발송 리스트
				order_status = 2;
			}else if(order_type.equals("cancelled")==true) {// 취소 리스트
				order_status = 3;
			}
			logger.info("getOrderListDeilvered order_status:"+order_status+"//"+seller_id);
			String last_idx = "";
			String start_date = "";
			String end_date = "";
			String search_keyword = "";
			if(EmptyUtils.isEmpty(param_body.getStart_date())==false
					|| EmptyUtils.isEmpty(param_body.getEnd_date())==false
					|| EmptyUtils.isEmpty(param_body.getSearch_keyword())==false
					){
				if(EmptyUtils.isEmpty(param_body.getStart_date())==false
						&& EmptyUtils.isEmpty(param_body.getEnd_date())==false) {
					start_date = param_body.getStart_date();
					end_date = param_body.getEnd_date();
				}
				if(EmptyUtils.isEmpty(param_body.getSearch_keyword())==false) {
					search_keyword = param_body.getSearch_keyword(); 
				}
			}
			if(EmptyUtils.isEmpty(param_body.getLast_idx())==false) {
				String dec_last_idx;
				try {
					dec_last_idx = CryptoUtil.decrypt(defaultConfig.getLastIdxSeed(), param_body.getLast_idx());
					if(EmptyUtils.isEmpty(dec_last_idx)==false) {
						last_idx = dec_last_idx;
					}else {
						last_idx = "";
					}	
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			logger.info("getOrderListDeilvered start_date:"+start_date+"//"+seller_id);
			logger.info("getOrderListDeilvered end_date:"+end_date+"//"+seller_id);
			logger.info("getOrderListDeilvered search_keyword:"+search_keyword+"//"+seller_id);
			logger.info("getOrderListDeilvered last_idx:"+last_idx+"//"+seller_id);
			
			if(EmptyUtils.isEmpty(start_date)==false) {
				List<OrderDomain.PluginOrderListDomain> getPluginOrderGroupListSearchType = new ArrayList<OrderDomain.PluginOrderListDomain>();
				Model model_group_search = new ExtendedModelMap();
				model_group_search.addAttribute("seller_id", seller_id);
				model_group_search.addAttribute("order_status", order_status);
				model_group_search.addAttribute("start_date", start_date);
				model_group_search.addAttribute("end_date", end_date);
				model_group_search.addAttribute("search_type", 1);
				getPluginOrderGroupListSearchType = orderService.getPluginOrderGroupList(model_group_search);
				if(getPluginOrderGroupListSearchType.get(0).getSearch_group_cnt()>0) {
					search_product_total_count = getPluginOrderGroupListSearchType.get(0).getSearch_group_cnt();
				}	
			}
			
			List<String> paymentId_list = new ArrayList<String>();
			List<OrderDomain.PluginOrderListDomain> getPluginOrderGroupList = new ArrayList<OrderDomain.PluginOrderListDomain>();
			Model model_group = new ExtendedModelMap();
			model_group.addAttribute("seller_id", seller_id);
			model_group.addAttribute("order_status", order_status);
			model_group.addAttribute("start_date", start_date);
			model_group.addAttribute("end_date", end_date);
			model_group.addAttribute("search_keyword", search_keyword);
			model_group.addAttribute("last_idx", last_idx);
			model_group.addAttribute("sort", "desc");
			model_group.addAttribute("limit_val", defaultConfig.getListLimit());
			
			String last_paymentid = "";
			getPluginOrderGroupList = orderService.getPluginOrderGroupList(model_group);
			if(EmptyUtils.isEmpty(getPluginOrderGroupList)==false) {
				if(getPluginOrderGroupList.size()>0){
					for(int i = 0; i < getPluginOrderGroupList.size(); i++) {
						paymentId_list.add(getPluginOrderGroupList.get(i).getPaymentId());
						last_paymentid = getPluginOrderGroupList.get(i).getPaymentId();
					}	
				}
			}
			
			if(paymentId_list.size()>0) {
				try {
					List<OrderDomain.PluginOrderListDomain> getPluginOrderProductList = new ArrayList<OrderDomain.PluginOrderListDomain>();
					List<OrderDomain.ReturnData> data_list = new ArrayList<OrderDomain.ReturnData>();
					Model model_get_1 = new ExtendedModelMap();
					model_get_1.addAttribute("seller_id", seller_id);
					model_get_1.addAttribute("order_status", order_status); 
					model_get_1.addAttribute("sort", "desc");
					model_get_1.addAttribute("paymentId_list", paymentId_list);
					
					getPluginOrderProductList = orderService.getPluginOrderProductList(model_get_1);
					
					if(EmptyUtils.isEmpty(getPluginOrderProductList)==false) {
						if(getPluginOrderProductList.size()>0) {
							String last_order_key = "";
							OrderDomain.ReturnData returnDataSingle = new OrderDomain.ReturnData();
							List<OrderDomain.ProductSingle> product_single_list = new ArrayList<OrderDomain.ProductSingle>();
							for(int j = 0; j < getPluginOrderProductList.size(); j++) {
								if(getPluginOrderProductList.get(j).getPurchaseId().equals(last_order_key)==false) {
									returnDataSingle = new OrderDomain.ReturnData();
									returnDataSingle.setOrder_key(getPluginOrderProductList.get(j).getPurchaseId());
									returnDataSingle.setUser_id(getPluginOrderProductList.get(j).getUser_id());
									returnDataSingle.setUser_name(getPluginOrderProductList.get(j).getRecipient_name());
									returnDataSingle.setPay_date(getPluginOrderProductList.get(j).getPaid_date());
									product_single_list = new ArrayList<OrderDomain.ProductSingle>();
									
									String last_product_key = "";
									for(int jj = 0; jj < getPluginOrderProductList.size(); jj++) {
										if(getPluginOrderProductList.get(jj).getPurchaseId().equals(getPluginOrderProductList.get(j).getPurchaseId())==true) {
											if(last_product_key.equals(getPluginOrderProductList.get(jj).getProductId())==false){
												OrderDomain.ProductSingle product_single = new OrderDomain.ProductSingle();
												product_single.setProduct_key(getPluginOrderProductList.get(jj).getProductId());
												product_single.setProduct_name(getPluginOrderProductList.get(jj).getProduct_name());
												product_single.setRecipient_name(getPluginOrderProductList.get(jj).getRecipient_name());
												
												List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
												for(int ii = 0; ii < getPluginOrderProductList.size(); ii++) {
													if(getPluginOrderProductList.get(jj).getPurchaseId().equals(getPluginOrderProductList.get(ii).getPurchaseId())==true) {
														if(getPluginOrderProductList.get(jj).getProductId().equals(getPluginOrderProductList.get(ii).getProductId())==true) {
															OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
															pruduct_variant_single.setProduct_variant_id(getPluginOrderProductList.get(ii).getProduct_variant_id());
															pruduct_variant_single.setProduct_variant_value(getPluginOrderProductList.get(ii).getProduct_variant_name());
															pruduct_variant_single.setQuantity(getPluginOrderProductList.get(ii).getProduct_variant_quantity());
															product_variant_single_list.add(pruduct_variant_single);															
														}
													}
												}
												product_single.setProduct_variant_list(product_variant_single_list);
												product_single_list.add(product_single);	
											}
											last_product_key = getPluginOrderProductList.get(jj).getProductId();	
										}
									}
									returnDataSingle.setProduct_list(product_single_list);	
									data_list.add(returnDataSingle);	
								}
								
								last_order_key = getPluginOrderProductList.get(j).getPurchaseId();
							}
							
							SetDomain.MainMenuCount total_count = new SetDomain.MainMenuCount();
							Model model_count = new ExtendedModelMap();
							model_count.addAttribute("seller_id", seller_id);
							model_count.addAttribute("view_type", "delivered");
							total_count = orderService.getPluginMainCnt(model_count);
							order_product_total_count = total_count.getDelivery_order_cnt();
							
							try {
								last_paymentid = CryptoUtil.encrypt(defaultConfig.getLastIdxSeed(), last_paymentid);
							} catch (Exception e) {
								e.printStackTrace();
							}
							order_list.setData(data_list);
							order_list.setLast_idx(last_paymentid);
							order_list.setOrder_product_total_count(order_product_total_count);
							order_list.setSearch_product_total_count(search_product_total_count);
							order_list.setOrder_product_cnt(null);
						}
					}
				} catch (Exception e) {
					error_code = 114;
					error_val = "delivered getPluginOrderProductList error : e : "+e;
				}
			}
		}

		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		return order_list;
	}
	

	/**
	 * @desc  
	 */
	public AccountDomain.ReturnData getAccountsListV2(Model model, String connect_type, String seller_id
			, OrderBodyDomain.ParamBody param_body) {
		
		AccountDomain.ReturnData account_return_val = new AccountDomain.ReturnData(); 
		Integer view_type = 0;
		int error_code = 0;
		String error_val = "";
		String accounts_key = "";
	
		if(EmptyUtils.isEmpty(param_body.getView_type())==true) {
			error_code = 115;
			error_val = "accounts : empty view_type ";	
		}else {
			view_type = param_body.getView_type();
			if(EmptyUtils.isEmpty(param_body.getAccounts_key())==false) {
				accounts_key = param_body.getAccounts_key();
			}
		}
		
		if(error_code==0) {
			//============================================================================
			if(view_type==1) {
				Long start_val = 0L;
				Long end_val = 0L;
				String start_date = "";
				String end_date = "";
				SetDomain.StatusModifyReturn getAccountListData = new SetDomain.StatusModifyReturn();
				
				if(EmptyUtils.isEmpty(param_body.getStart_date())==false
						&& EmptyUtils.isEmpty(param_body.getEnd_date())==false) {
					
					start_date = param_body.getStart_date();
					end_date = param_body.getEnd_date();
				}else {
					error_code = 115;
					error_val = "accounts : empty view_type ";	
				}
				
				if(error_code==0) {
					start_val = LocationTimeCal.DatetoTimeStamp(start_date+"T23:00:00.000Z");
					end_val = LocationTimeCal.DatetoTimeStamp(end_date+"T23:00:00.000Z");
					
					try {
						String account_body = "";
						account_body = account_body +"{";
						account_body = account_body +"\"sellerIds\": [";
						account_body = account_body +"\""+seller_id+"\"";
						account_body = account_body +"],";
						account_body = account_body +"\"status\": [\"onGoing\",\"closed\",\"completed\"]";
						account_body = account_body +", \"settleDate\": {"
								+ "    \"gte\": "+start_val+","
								+ "    \"lte\": "+end_val+""
								+ "  }";
						account_body = account_body +"}";
						
						getAccountListData = moimApiService.AccountsListGet(connect_type, seller_id, account_body);
					} catch (Exception e1) {
						error_code = 122;
						error_val = "moimApiService.AccountsListGet error : e1 : "+e1;
					}
				}
				
				if(error_code==0 && EmptyUtils.isEmpty(getAccountListData.getResult_return())==false) {
					AccountDomain.MoimGetList account_moim_get_list = new AccountDomain.MoimGetList();
					try {
						account_moim_get_list = gson.fromJson(getAccountListData.getResult_return(), AccountDomain.MoimGetList.class);
						if(account_moim_get_list.getData().size()>0) {
							if(EmptyUtils.isEmpty(account_moim_get_list.getTotal())==false){
								AccountWeekData search_data_single = new AccountWeekData();
								search_data_single.setAccounts_start_date(param_body.getStart_date());
								search_data_single.setAccounts_end_date(param_body.getEnd_date());
								search_data_single.setAccounts_total_price(account_moim_get_list.getTotal().getTotalPrice());
								search_data_single.setSupply_price(account_moim_get_list.getTotal().getSupplyPrice());
								search_data_single.setDelivery_fee(account_moim_get_list.getTotal().getDeliveryFee());
								search_data_single.setExtra_price(account_moim_get_list.getTotal().getExtraPrice());
								search_data_single.setRefunded_price(account_moim_get_list.getTotal().getRefundedPrice());
								account_return_val.setSearch_data(search_data_single);
							}
						}
					} catch (Exception e2) {
						error_code = 122;
						error_val = "moim_get_list error : e2 : "+e2;
					}
				}
				
			//============================================================================
			}else if(view_type==2) {
				
				Long start_val = 0L;
				Long end_val = 0L;
				String start_date = "";
				String end_date = "";
				SetDomain.StatusModifyReturn getAccountListData = new SetDomain.StatusModifyReturn();
				
				if(EmptyUtils.isEmpty(param_body.getStart_date())==false
						|| EmptyUtils.isEmpty(param_body.getEnd_date())==false) {
					
					start_date = param_body.getStart_date();
					end_date = param_body.getEnd_date();
				}else {
					error_code = 115;
					error_val = "accounts : empty view_type ";	
				}
				
				if(error_code==0) {
					start_val = LocationTimeCal.DatetoTimeStamp(start_date+"T23:00:00.000Z");
					end_val = LocationTimeCal.DatetoTimeStamp(end_date+"T23:00:00.000Z");
					
					try {
						String account_body = "";
						account_body = account_body +"{";
						account_body = account_body +"\"sellerIds\": [";
						account_body = account_body +"\""+seller_id+"\"";
						account_body = account_body +"],";
						account_body = account_body +"\"status\": [\"onGoing\",\"closed\",\"completed\"]";
						account_body = account_body +", \"settleDate\": {"
								+ "    \"gte\": "+start_val+","
								+ "    \"lte\": "+end_val+""
								+ "  }";
						account_body = account_body +"}";
						
						getAccountListData = moimApiService.AccountsListGet(connect_type, seller_id, account_body);
					} catch (Exception e1) {
						error_code = 122;
						error_val = "moimApiService.AccountsListGet error : e1 : "+e1;
					}
				}
				if(error_code==0 && EmptyUtils.isEmpty(getAccountListData.getResult_return())==false) {
					AccountDomain.MoimGetList account_moim_get_list = new AccountDomain.MoimGetList();
					try {
						account_moim_get_list = gson.fromJson(getAccountListData.getResult_return(), AccountDomain.MoimGetList.class);
						if(account_moim_get_list.getData().size()>0) {
							List<DataSingle> account_moim_get_single_total_list = new ArrayList<DataSingle>();
							for(int k1 = 0; k1 < account_moim_get_list.getData().size(); k1++) {
								if(EmptyUtils.isEmpty(account_moim_get_list.getData().get(k1).getId())==false) {
									String after = "";
									String account_id = account_moim_get_list.getData().get(k1).getId();
									for(int k3 = 0; k3 < 10000; k3++) {
										SetDomain.StatusModifyReturn getAccountSingleData = new SetDomain.StatusModifyReturn();
										getAccountSingleData = moimApiService.AccountsSingleGet(connect_type, after, account_id);
										if(EmptyUtils.isEmpty(getAccountSingleData.getResult_return())==false) {
											AccountDomain.MoimGetSingle account_moim_get_single_sublist = new AccountDomain.MoimGetSingle();		
											account_moim_get_single_sublist = gson.fromJson(getAccountSingleData.getResult_return(), AccountDomain.MoimGetSingle.class);
											if(EmptyUtils.isEmpty(account_moim_get_single_sublist)==false) {
												List<String> data_id_list = new ArrayList<String>();
												for (int i = 0; i < account_moim_get_single_sublist.getData().size(); i++) {
													if(EmptyUtils.isEmpty(account_moim_get_single_sublist.getData().get(i).getPurchaseItemId())==false) {
														data_id_list.add(account_moim_get_single_sublist.getData().get(i).getPurchaseItemId());
													}
												}
												String get_order_single = "";
												get_order_single = moimApiService.OrderSingleGet(connect_type, seller_id, data_id_list);
												OrderGetDomain.GetData order_moim_get_domain = new OrderGetDomain.GetData();
												order_moim_get_domain = gson.fromJson(get_order_single, order_moim_get_domain.getClass());
												if(EmptyUtils.isEmpty(order_moim_get_domain)==false) {
													if(EmptyUtils.isEmpty(order_moim_get_domain.getData())==false) {
														for (int i = 0; i < account_moim_get_single_sublist.getData().size(); i++) {
															String getPurchaseItemId = "";
															if(EmptyUtils.isEmpty(account_moim_get_single_sublist.getData().get(i).getPurchaseItemId())==false) {
																getPurchaseItemId = account_moim_get_single_sublist.getData().get(i).getPurchaseItemId();
															}
															for (int i2= 0; i2 < order_moim_get_domain.getData().size(); i2++) {
																if(getPurchaseItemId.equals(order_moim_get_domain.getData().get(i2).getId())==true) {
																	account_moim_get_single_sublist.getData().get(i).setSettlement(account_moim_get_single_sublist.getSettlement());
																	account_moim_get_single_sublist.getData().get(i).setProduct_name(order_moim_get_domain.getData().get(i2).getProductName());
																	String var_val1 = "";
									  		  						String[] var_val_array1 = null;
									  		  						if(EmptyUtils.isEmpty(order_moim_get_domain.getData().get(i2).getProductVariantValue())==false) {
									  		  							var_val1 = order_moim_get_domain.getData().get(i2).getProductVariantValue().toString();
									  		  							var_val1 = var_val1.replaceAll("\\{", "");
									  		  							var_val1 = var_val1.replaceAll("\\}", "");
									  		  						}
									  		  						if(EmptyUtils.isEmpty(var_val1)==false) {
									  		  							var_val_array1 = var_val1.split("=");
									  		  							for(int ii = 0; ii < var_val_array1.length; ii++) {
									  		  								if(ii==2){
									  		  									if(EmptyUtils.isEmpty(var_val_array1[ii])==false) {
									  		  										var_val1 =	var_val_array1[ii];
									  		  									}
									  		  								}
									  		  							}
									  		  						}
									  		  						account_moim_get_single_sublist.getData().get(i).setProduct_variant_name(var_val1);
									  		  						account_moim_get_single_sublist.getData().get(i).setProduct_variant_total_price(account_moim_get_single_sublist.getData().get(i).getTotalPrice());
									  		  						account_moim_get_single_sublist.getData().get(i).setOrder_user_name(order_moim_get_domain.getData().get(i2).getPurchase().getBuyerName());
																}else {//deduction
																	if(EmptyUtils.isEmpty(account_moim_get_single_sublist.getData().get(i).getDescription())==false){
																		account_moim_get_single_sublist.getData().get(i).setSettlement(account_moim_get_single_sublist.getSettlement());
																		account_moim_get_single_sublist.getData().get(i).setProduct_name(account_moim_get_single_sublist.getData().get(i).getDescription());	
																		account_moim_get_single_sublist.getData().get(i).setProduct_variant_total_price(account_moim_get_single_sublist.getData().get(i).getTotalPrice());
																	}
																}
															}
														}
													}
												}
												for (int i = 0; i < account_moim_get_single_sublist.getData().size(); i++) {
													account_moim_get_single_total_list.add(account_moim_get_single_sublist.getData().get(i));
												}
												if(EmptyUtils.isEmpty(account_moim_get_single_sublist.getPaging())==false){
													if(EmptyUtils.isEmpty(account_moim_get_single_sublist.getPaging().getAfter())==false){
														after = account_moim_get_single_sublist.getPaging().getAfter();
													}else {
														break;
													}
												}else {
													break;
												}
											}else if(account_moim_get_single_total_list.size()==0) {
												AccountWeekData empty_search_data_single = new AccountWeekData();
												List<ProductOptionSingle> product_list = new ArrayList<ProductOptionSingle>();
												List<ExcelProductSingle> excel_data = new ArrayList<ExcelProductSingle>();
												Integer accounts_total_price = 0;
												Integer supply_price = 0;
												Integer delivery_fee = 0;
												Integer extra_price = 0;
												Integer refunded_price = 0;
												empty_search_data_single.setAccounts_start_date(param_body.getStart_date());
												empty_search_data_single.setAccounts_end_date(param_body.getEnd_date());
												empty_search_data_single.setAccounts_total_price(accounts_total_price);
												empty_search_data_single.setSupply_price(supply_price);
												empty_search_data_single.setDelivery_fee(delivery_fee);
												empty_search_data_single.setExtra_price(extra_price);
												empty_search_data_single.setRefunded_price(refunded_price);
												empty_search_data_single.setProduct_list(product_list);
												empty_search_data_single.setExcel_data(excel_data);
												account_return_val.setSearch_data(empty_search_data_single);
												break;
											}
										}
									} 
								}
							
							}
							
							
							if(account_moim_get_single_total_list.size()>0) {
								try {
									AccountWeekData search_data_single = new AccountWeekData();
									List<ProductOptionSingle> product_list = new ArrayList<ProductOptionSingle>();
									List<ExcelProductSingle> excel_data = new ArrayList<ExcelProductSingle>();
									Integer accounts_total_price = 0;
									Integer supply_price = 0;
									Integer delivery_fee = 0;
									Integer extra_price = 0;
									Integer refunded_price = 0;
									for (int i = 0; i < account_moim_get_single_total_list.size(); i++) {
										ProductOptionSingle product_option_single = new ProductOptionSingle();
										ExcelProductSingle excel_product_single = new ExcelProductSingle();
										
										product_option_single.setProduct_name(account_moim_get_single_total_list.get(i).getProduct_name());
										product_option_single.setProduct_variant_name(account_moim_get_single_total_list.get(i).getProduct_variant_name());
										product_option_single.setProduct_variant_total_price(account_moim_get_single_total_list.get(i).getProduct_variant_total_price());
										product_list.add(product_option_single);
										
										if(EmptyUtils.isEmpty(account_moim_get_single_total_list.get(i).getPurchaseItemId())==false) {
											excel_product_single.setOrder_id(account_moim_get_single_total_list.get(i).getPurchaseItemId());	
										}else {
											excel_product_single.setOrder_id("");
										}
										
										String settle_date = "";
										String completed_at = "";
										String targeted_date = "";
										try {
											if(EmptyUtils.isEmpty(account_moim_get_single_total_list.get(i).getSettlement())==false) {
												if(EmptyUtils.isEmpty(account_moim_get_single_total_list.get(i).getSettlement().getSettleDate())==false) {
													settle_date = LocationTimeCal.TimeStamptoDate(account_moim_get_single_total_list.get(i).getSettlement().getSettleDate());
												}
												if(EmptyUtils.isEmpty(account_moim_get_single_total_list.get(i).getSettlement().getCompletedAt())==false) {
													completed_at = LocationTimeCal.TimeStamptoDate(account_moim_get_single_total_list.get(i).getSettlement().getCompletedAt());
												}
											}
											if(EmptyUtils.isEmpty(account_moim_get_single_total_list.get(i).getTargetedDate())==false) {
												targeted_date = LocationTimeCal.TimeStamptoDate(account_moim_get_single_total_list.get(i).getTargetedDate());
											}
										} catch (Exception e1) {
											logger.info("LocationTimeCal.TimeStamptoDate e1:"+e1+"//"+seller_id);
										}
										
										excel_product_single.setSettle_date(settle_date);
										excel_product_single.setCompleted_at(completed_at);
										excel_product_single.setTargeted_date(targeted_date);
										excel_product_single.setProduct_name(account_moim_get_single_total_list.get(i).getProduct_name());
										excel_product_single.setOrder_user_name(account_moim_get_single_total_list.get(i).getOrder_user_name());
										excel_product_single.setQuantity(account_moim_get_single_total_list.get(i).getQuantity());
										excel_product_single.setSupply_price(account_moim_get_single_total_list.get(i).getSupplyPrice());
										excel_product_single.setProduct_price(account_moim_get_single_total_list.get(i).getProductPrice());
										excel_product_single.setDelivery_fee(account_moim_get_single_total_list.get(i).getDeliveryFee());
										excel_product_single.setCommission(account_moim_get_single_total_list.get(i).getCommission());
										excel_product_single.setTotal_price(account_moim_get_single_total_list.get(i).getTotalPrice());
										excel_product_single.setExtra_price(account_moim_get_single_total_list.get(i).getExtraPrice());
										excel_product_single.setRefunded_price(account_moim_get_single_total_list.get(i).getRefundedPrice());
										excel_data.add(excel_product_single);

									}
									accounts_total_price = account_moim_get_list.getTotal().getTotalPrice();
									supply_price = account_moim_get_list.getTotal().getSupplyPrice();
									delivery_fee = account_moim_get_list.getTotal().getDeliveryFee();
									extra_price = account_moim_get_list.getTotal().getExtraPrice();
									refunded_price = account_moim_get_list.getTotal().getRefundedPrice();
									
									search_data_single.setAccounts_start_date(param_body.getStart_date());
									search_data_single.setAccounts_end_date(param_body.getEnd_date());
									search_data_single.setAccounts_total_price(accounts_total_price);
									search_data_single.setSupply_price(supply_price);
									search_data_single.setDelivery_fee(delivery_fee);
									search_data_single.setExtra_price(extra_price);
									search_data_single.setRefunded_price(refunded_price);
									
									search_data_single.setProduct_list(product_list);
									search_data_single.setExcel_data(excel_data);
									
									account_return_val.setSearch_data(search_data_single);
								} catch (Exception e) {
									error_code = 122;
									error_val = "account_moim_get_single_total_list error : e2 : "+e;
								}
							}
						}
					} catch (Exception e2) {
						error_code = 122;
						error_val = "moim_get_list error : e2 : "+e2;
					}
				}
			//============================================================================
			}else if(view_type==11) {
				Long start_val = 0L;
				Long end_val = 0L;
				String account_idx = "";
				Long at_val = 0L;
				String last_account_idx = "";
				
				SetDomain.StatusModifyReturn getAccountListData = new SetDomain.StatusModifyReturn();
					
				try {
					if(EmptyUtils.isEmpty(param_body.getLast_account_idx())==false) {
						account_idx = CryptoUtil.decrypt(defaultConfig.getLastIdxSeed(), param_body.getLast_account_idx());
						at_val = Long.parseLong(account_idx);
					}
					if(at_val>0) {
						logger.info("getAccountsListV2 at_val:"+at_val+"//"+seller_id);
						String parse_start_time = LocationTimeCal.TimeStamptoDate(at_val);	
						start_val = LocationTimeCal.UnixTimeStampCalAccountList(3, parse_start_time); 
						end_val = LocationTimeCal.UnixTimeStampCalAccountList(0, parse_start_time); 
					}else {
						logger.info("getAccountsListV2 at_val 0:"+at_val+"//"+seller_id);
						start_val = LocationTimeCal.UnixTimeStampCalAccountList(3, "");
						end_val = LocationTimeCal.UnixTimeStampCalAccountList(2, "");
					}
					
					logger.info("getAccountsListV2 start_val:"+start_val+"//"+LocationTimeCal.TimeStamptoDate(start_val)+"//"+seller_id);
					logger.info("getAccountsListV2 end_val:"+end_val+"//"+LocationTimeCal.TimeStamptoDate(end_val)+"//"+seller_id);
					
				} catch (Exception e) {
					error_code = 122;
					error_val = "getAccountsListV2 LocationTimeCal.UnixTimeStampCal error : e : "+e;
				}
					
				if(error_code==0) {
					try {
						String account_body = "";
						account_body = account_body +"{";
						account_body = account_body +"\"sellerIds\": [";
						account_body = account_body +"\""+seller_id+"\"";
						account_body = account_body +"],";
						account_body = account_body +"\"status\": [\"onGoing\",\"closed\",\"completed\"]";
						account_body = account_body +", \"settleDate\": {"
								+ "    \"gte\": "+start_val+","
								+ "    \"lte\": "+end_val+""
								+ "  }";
						account_body = account_body +"}";
						
						getAccountListData = moimApiService.AccountsListGet(connect_type, seller_id, account_body);
					} catch (Exception e1) {
						error_code = 122;
						error_val = "getAccountsListV2 moimApiService.AccountsListGet error : e1 : "+e1;
					}
				}
				if(error_code==0 && EmptyUtils.isEmpty(getAccountListData.getResult_return())==false) {
					AccountDomain.MoimGetList account_moim_get_week_list = new AccountDomain.MoimGetList();
					try {
						account_moim_get_week_list = gson.fromJson(getAccountListData.getResult_return(), AccountDomain.MoimGetList.class);
						if(account_moim_get_week_list.getData().size()>0) {
							
							List<AccountWeekData> account_week_list = new ArrayList<AccountWeekData>();
							
							for(int k1 = 0; k1 < account_moim_get_week_list.getData().size(); k1++) {
								if(EmptyUtils.isEmpty(account_moim_get_week_list.getData().get(k1).getId())==false) {
									AccountWeekData week_data_single = new AccountWeekData();
									
									String account_key = "";
									account_key = CryptoUtil.encrypt(defaultConfig.getLastIdxSeed(), account_moim_get_week_list.getData().get(k1).getId());
									
									week_data_single.setAccounts_key(account_key);
									String start_date = "";
									String end_date = "";
									try {
										if(EmptyUtils.isEmpty(account_moim_get_week_list.getData().get(k1).getSettleDate())==false) {
											last_account_idx = account_moim_get_week_list.getData().get(k1).getSettleDate().toString();
											start_date = LocationTimeCal.TimeStamptoDate(account_moim_get_week_list.getData().get(k1).getSettleDate());
										}
										if(EmptyUtils.isEmpty(account_moim_get_week_list.getData().get(k1).getCompletedAt())==false) {
											end_date = LocationTimeCal.TimeStamptoDate(account_moim_get_week_list.getData().get(k1).getCompletedAt());
										}
									} catch (Exception eTime) {
										logger.info("LocationTimeCal.TimeStamptoDate: e:"+eTime+"//"+seller_id);
									}
									week_data_single.setAccounts_start_date(start_date);
									week_data_single.setAccounts_end_date(end_date);
									week_data_single.setAccounts_total_price(account_moim_get_week_list.getData().get(k1).getTotalPrice());
									Integer accounts_status = 0;
									if(EmptyUtils.isEmpty(account_moim_get_week_list.getData().get(k1).getStatus())==false) {
										if(account_moim_get_week_list.getData().get(k1).getStatus().equals("onGoing")==true) {
											accounts_status = 1;
										}else if(account_moim_get_week_list.getData().get(k1).getStatus().equals("closed")==true) {
											accounts_status = 2;
										}else if(account_moim_get_week_list.getData().get(k1).getStatus().equals("completed")==true) {
											accounts_status = 3;
										}
									}
									week_data_single.setAccounts_status(accounts_status);
									week_data_single.setSupply_price(account_moim_get_week_list.getData().get(k1).getSupplyPrice());
									week_data_single.setDelivery_fee(account_moim_get_week_list.getData().get(k1).getDeliveryFee());
									week_data_single.setExtra_price(account_moim_get_week_list.getData().get(k1).getExtraPrice());
									week_data_single.setRefunded_price(account_moim_get_week_list.getData().get(k1).getRefundedPrice());
									account_week_list.add(week_data_single);
								}
							}
							account_return_val.setAccount_week_list(account_week_list);
						}
					} catch (Exception e) {
						error_code = 122;
						error_val = "getAccountsListV2 view_type==11 error : e2 : "+e;
					}
				}
				
				if(EmptyUtils.isEmpty(last_account_idx)==false) {
					try {
						last_account_idx = CryptoUtil.encrypt(defaultConfig.getLastIdxSeed(), last_account_idx);
						
						logger.info("getAccountsListV2 last_account_idx:"+last_account_idx+"//"+seller_id);
						logger.info("getAccountsListV2 last_account_idx enc :"+last_account_idx+"//"+seller_id);
					} catch (Exception e) {
						logger.info("getAccountsListV2 CryptoUtil.encrypt : "+last_account_idx+" : e:"+e+"//"+seller_id);
					}
					account_return_val.setLast_account_idx(last_account_idx);
				}
				
				if(EmptyUtils.isEmpty(account_return_val.getAccount_week_list())==true) {
					List<AccountWeekData> empty_account_week_list = new ArrayList<AccountWeekData>();
					account_return_val.setAccount_week_list(empty_account_week_list);
					account_return_val.setLast_account_idx(null);
				}
				
			//============================================================================
			}else if(view_type==12 && EmptyUtils.isEmpty(accounts_key)==false){
				String after = "";
				String account_id = ""; 
				try {
					account_id = CryptoUtil.decrypt(defaultConfig.getLastIdxSeed(), accounts_key);
				} catch (Exception e) {
					
				}
				List<DataSingle> account_moim_get_single_week_all_list = new ArrayList<DataSingle>();
				if(EmptyUtils.isEmpty(account_id)==false) {	
					for(int k3 = 0; k3 < 10; k3++) {
						SetDomain.StatusModifyReturn getAccountSingleData = new SetDomain.StatusModifyReturn();
						getAccountSingleData = moimApiService.AccountsSingleGet(connect_type, after, account_id);
						if(EmptyUtils.isEmpty(getAccountSingleData.getResult_return())==false) {
							AccountDomain.MoimGetSingle account_moim_get_single_sublist = new AccountDomain.MoimGetSingle();		
							account_moim_get_single_sublist = gson.fromJson(getAccountSingleData.getResult_return(), AccountDomain.MoimGetSingle.class);
							if(EmptyUtils.isEmpty(account_moim_get_single_sublist)==false) {
								List<String> data_id_list = new ArrayList<String>();
								if(account_moim_get_single_sublist.getData().size()>0) {
									for(int i = 0; i < account_moim_get_single_sublist.getData().size(); i++) {
										if(EmptyUtils.isEmpty(account_moim_get_single_sublist.getData().get(i).getPurchaseItemId())==false) {
											data_id_list.add(account_moim_get_single_sublist.getData().get(i).getPurchaseItemId());
										}
									}	
								}else {
									break;
								}
								
								if(data_id_list.size()>0) {
									String get_order_single = "";
									get_order_single = moimApiService.OrderSingleGet(connect_type, seller_id, data_id_list);
									
									OrderGetDomain.GetData order_moim_get_domain = new OrderGetDomain.GetData();
									order_moim_get_domain = gson.fromJson(get_order_single, order_moim_get_domain.getClass());
									if(EmptyUtils.isEmpty(order_moim_get_domain)==false) {
										if(EmptyUtils.isEmpty(order_moim_get_domain.getData())==false) {
										
											for(int i = 0; i < account_moim_get_single_sublist.getData().size(); i++) {
												String getPurchaseItemId = "";
												if(EmptyUtils.isEmpty(account_moim_get_single_sublist.getData().get(i).getPurchaseItemId())==false) {
													getPurchaseItemId = account_moim_get_single_sublist.getData().get(i).getPurchaseItemId();
												}
												for (int i2= 0; i2 < order_moim_get_domain.getData().size(); i2++) {
													if(getPurchaseItemId.equals(order_moim_get_domain.getData().get(i2).getId())==true) {
														account_moim_get_single_sublist.getData().get(i).setSettlement(account_moim_get_single_sublist.getSettlement());
														account_moim_get_single_sublist.getData().get(i).setProduct_name(order_moim_get_domain.getData().get(i2).getProductName());
														
														String var_val1 = "";
						  		  						String[] var_val_array1 = null;
						  		  						if(EmptyUtils.isEmpty(order_moim_get_domain.getData().get(i2).getProductVariantValue())==false) {
						  		  							var_val1 = order_moim_get_domain.getData().get(i2).getProductVariantValue().toString();
						  		  							var_val1 = var_val1.replaceAll("\\{", "");
						  		  							var_val1 = var_val1.replaceAll("\\}", "");
						  		  						}
						  		  						if(EmptyUtils.isEmpty(var_val1)==false) {
						  		  							var_val_array1 = var_val1.split("=");
						  		  							for(int ii = 0; ii < var_val_array1.length; ii++) {
						  		  								if(ii==2){
						  		  									if(EmptyUtils.isEmpty(var_val_array1[ii])==false) {
						  		  										var_val1 =	var_val_array1[ii];
						  		  									}
						  		  								}
						  		  							}
						  		  						}
						  		  						account_moim_get_single_sublist.getData().get(i).setProduct_variant_name(var_val1);
						  		  						account_moim_get_single_sublist.getData().get(i).setProduct_variant_total_price(account_moim_get_single_sublist.getData().get(i).getTotalPrice());
						  		  						account_moim_get_single_sublist.getData().get(i).setOrder_user_name(order_moim_get_domain.getData().get(i2).getPurchase().getBuyerName());
													}else {
														if(EmptyUtils.isEmpty(account_moim_get_single_sublist.getData().get(i).getDescription())==false){
															account_moim_get_single_sublist.getData().get(i).setSettlement(account_moim_get_single_sublist.getSettlement());
															account_moim_get_single_sublist.getData().get(i).setProduct_name(account_moim_get_single_sublist.getData().get(i).getDescription());	
															account_moim_get_single_sublist.getData().get(i).setProduct_variant_total_price(account_moim_get_single_sublist.getData().get(i).getTotalPrice());
														}
													}
												}
												
												account_moim_get_single_week_all_list.add(account_moim_get_single_sublist.getData().get(i));
											}
											if(EmptyUtils.isEmpty(account_moim_get_single_sublist.getPaging())==false){
												if(EmptyUtils.isEmpty(account_moim_get_single_sublist.getPaging().getAfter())==false){
													after = account_moim_get_single_sublist.getPaging().getAfter();
												}else {
													break;
												}
											}else {
												break;
											}
										}
									}
								}
							}else {
								break;
							}
						}
					}	
				}
				
				if(account_moim_get_single_week_all_list.size()>0) {
					try {
						List<AccountWeekData> account_week_list = new ArrayList<AccountWeekData>();
						List<ProductOptionSingle> product_list = new ArrayList<ProductOptionSingle>();
						List<ExcelProductSingle> excel_data = new ArrayList<ExcelProductSingle>();
						AccountWeekData week_data_single = new AccountWeekData();
						Integer accounts_total_price = 0;
						Integer supply_price = 0;
						Integer delivery_fee = 0;
						Integer extra_price = 0;
						Integer refunded_price = 0;
						for (int i = 0; i < account_moim_get_single_week_all_list.size(); i++) {
							ProductOptionSingle product_option_single = new ProductOptionSingle();
							ExcelProductSingle excel_product_single = new ExcelProductSingle();
							
							product_option_single.setProduct_name(account_moim_get_single_week_all_list.get(i).getProduct_name());
							product_option_single.setProduct_variant_name(account_moim_get_single_week_all_list.get(i).getProduct_variant_name());
							product_option_single.setProduct_variant_total_price(account_moim_get_single_week_all_list.get(i).getProduct_variant_total_price());
							product_list.add(product_option_single);
							
							if(EmptyUtils.isEmpty(account_moim_get_single_week_all_list.get(i).getPurchaseItemId())==false) {
								excel_product_single.setOrder_id(account_moim_get_single_week_all_list.get(i).getPurchaseItemId());	
							}else {
								excel_product_single.setOrder_id("");
							}
							
							String settle_date = "";
							String completed_at = "";
							String targeted_date = "";
							try {
								if(EmptyUtils.isEmpty(account_moim_get_single_week_all_list.get(i).getSettlement())==false) {
									if(EmptyUtils.isEmpty(account_moim_get_single_week_all_list.get(i).getSettlement().getSettleDate())==false) {
										settle_date = LocationTimeCal.TimeStamptoDate(account_moim_get_single_week_all_list.get(i).getSettlement().getSettleDate());
									}
									if(EmptyUtils.isEmpty(account_moim_get_single_week_all_list.get(i).getSettlement().getCompletedAt())==false) {
										completed_at = LocationTimeCal.TimeStamptoDate(account_moim_get_single_week_all_list.get(i).getSettlement().getCompletedAt());
									}
								}
								if(EmptyUtils.isEmpty(account_moim_get_single_week_all_list.get(i).getTargetedDate())==false) {
									targeted_date = LocationTimeCal.TimeStamptoDate(account_moim_get_single_week_all_list.get(i).getTargetedDate());
								}
							} catch (Exception e1) {
								logger.info("getAccountsListV2 LocationTimeCal.TimeStamptoDate e1:"+e1+"//"+seller_id);
							}
							
							excel_product_single.setSettle_date(settle_date);
							excel_product_single.setCompleted_at(completed_at);
							excel_product_single.setTargeted_date(targeted_date);
							excel_product_single.setProduct_name(account_moim_get_single_week_all_list.get(i).getProduct_name());
							excel_product_single.setOrder_user_name(account_moim_get_single_week_all_list.get(i).getOrder_user_name());
							excel_product_single.setQuantity(account_moim_get_single_week_all_list.get(i).getQuantity());
							excel_product_single.setSupply_price(account_moim_get_single_week_all_list.get(i).getSupplyPrice());
							excel_product_single.setProduct_price(account_moim_get_single_week_all_list.get(i).getProductPrice());
							excel_product_single.setDelivery_fee(account_moim_get_single_week_all_list.get(i).getDeliveryFee());
							excel_product_single.setCommission(account_moim_get_single_week_all_list.get(i).getCommission());
							excel_product_single.setTotal_price(account_moim_get_single_week_all_list.get(i).getTotalPrice());
							excel_product_single.setExtra_price(account_moim_get_single_week_all_list.get(i).getExtraPrice());
							excel_product_single.setRefunded_price(account_moim_get_single_week_all_list.get(i).getRefundedPrice());
							excel_data.add(excel_product_single);
						}
						
						accounts_total_price = account_moim_get_single_week_all_list.get(0).getSettlement().getTotalPrice();
						supply_price = account_moim_get_single_week_all_list.get(0).getSettlement().getSupplyPrice();
						delivery_fee = account_moim_get_single_week_all_list.get(0).getSettlement().getDeliveryFee();
						extra_price = account_moim_get_single_week_all_list.get(0).getSettlement().getExtraPrice();
						refunded_price = account_moim_get_single_week_all_list.get(0).getSettlement().getRefundedPrice();
						
						week_data_single.setAccounts_key(account_moim_get_single_week_all_list.get(0).getSettlement().getId());
						String start_date = "";
						String end_date = "";
						try {
							if(EmptyUtils.isEmpty(account_moim_get_single_week_all_list.get(0).getSettlement().getSettleDate())==false) {
								
								start_date = LocationTimeCal.TimeStamptoDate(account_moim_get_single_week_all_list.get(0).getSettlement().getSettleDate());
							}
							if(EmptyUtils.isEmpty(account_moim_get_single_week_all_list.get(0).getSettlement().getCompletedAt())==false) {
								end_date = LocationTimeCal.TimeStamptoDate(account_moim_get_single_week_all_list.get(0).getSettlement().getCompletedAt());
							}
						} catch (Exception eTime) {
							logger.info("getAccountsListV2 LocationTimeCal.TimeStamptoDate: e:"+eTime+"//"+seller_id);
						}
						week_data_single.setAccounts_start_date(start_date);
						week_data_single.setAccounts_end_date(end_date);
						week_data_single.setAccounts_total_price(accounts_total_price);
						week_data_single.setSupply_price(supply_price);
						week_data_single.setDelivery_fee(delivery_fee);
						week_data_single.setExtra_price(extra_price);
						week_data_single.setRefunded_price(refunded_price);
						
						week_data_single.setProduct_list(product_list);
						week_data_single.setExcel_data(excel_data);
						account_week_list.add(week_data_single);
						account_return_val.setAccount_week_list(account_week_list);
					} catch (Exception e) {
						error_code = 122;
						error_val = "getAccountsListV2 account_moim_get_single_total_list error : e2 : "+e;
					}
				}
									
			} 
		}
		account_return_val.setError_code(error_code);
		account_return_val.setError_val(error_val);
		return account_return_val;
	}
	

	/**
	 * @desc  
	 */
	public AccountDomain.ReturnData getAccountsList(Model model, String connect_type, String seller_id
			, OrderBodyDomain.ParamBody param_body) {
		
		AccountDomain.ReturnData account_return_val = new AccountDomain.ReturnData(); 
		
		int error_code = 0;
		String error_val = "";
		
		account_return_val.setError_code(error_code);
		account_return_val.setError_val(error_val);
	
		return account_return_val;
	}
	

	/**
	 * @desc  
	 */
	// 30개 이상 한번에 처리 
	// 모든 주문안에 CI: data_id로 한번에 상태변경, 배송접수 처리 로직 // 관망중은 동일함. 
	public SetDomain.ControllerResultStatusModifyReturnV2 postStatusModifyV3All(
			String request_id,  Model model, String connect_type, String seller_id
			, OrderBodyDomain.ModifyBodyList requestModifyBody, SellerIdInfoDomain sellerid_info, String seller_token) {
		
		SetDomain.ControllerResultStatusModifyReturnV2 return_val = new SetDomain.ControllerResultStatusModifyReturnV2(); 
		int error_code = 0;
		String error_val = "";
		ReturnModifyResultV2 modify_result = new ReturnModifyResultV2();
		String status_modify_log = "";// log 저장용
		String delivery_reception_log = "";// log 저장용
		logger.info("postStatusModifyV3All : "+request_id+": now:"+LocationTimeCal.GetNowDateTime()+"//"+seller_id);
		String now_date_time_start = LocationTimeCal.GetNowDateTime();
		status_modify_log = "//now_date_time_start:"+now_date_time_start+"//[";
		delivery_reception_log = "//now_date_time_start:"+now_date_time_start+"//[";
		ModifyResult ModifyResult = new ModifyResult();
		int batchSize = defaultConfig.getBatchSize();
		
		if(requestModifyBody.getModify_key()==1 && requestModifyBody.getModify_status()>0) {
			if(requestModifyBody.getModify_status()==3) {
				 List<OrderDomain.ReturnDatav1> success_result_list = new ArrayList<>();
			    List<OrderDomain.ReturnDatav1> failure_result_list = new ArrayList<>();
			    List<OrderDomain.ReturnDatav1> unavailable_result_list = new ArrayList<>();
				List<productVarinatSingle> moim_get_data_list = new ArrayList<productVarinatSingle>();
				List<String> purchase_send_ids = new ArrayList<String>(); //  paid ids
				List<String> status_put_send_cu_ids = new ArrayList<String>(); //  preparingForDelivery 상태변경 보내는 CU:00000
				int order_new_cnt = 0;
				StatusModifyResultReturnDelivery delivery_reception_result_domain = new StatusModifyResultReturnDelivery();
				List<GroupedModifyBody> grouprequestModifyBody = new ArrayList<GroupedModifyBody>();
				logger.info("postStatusModifyV3All : "+request_id+": postStatusModifyV3All start "
						+ ": request_id : "+request_id+"//"+seller_id);
				logger.info("postStatusModifyV3All : "+request_id+": postStatusModifyV3All start "
						+ ": start_date : "+now_date_time_start+"//"+seller_id);
				String process_status = "READY";//READY, PROCESSING, DONE, FAIL
				String async_yn = "";
				String async_error = "";
				//------------------------------------------
				// step 1 : 모임에서 새로운 주문 모두 가져옴. 
				String after = "";
				for(int i = 0; i < 1000; i++) {
					OrderGetDomain.GetData order_get_domain = new OrderGetDomain.GetData();
					String check = "";
			  		check = moimApiService.OrderListGet("requested", connect_type, seller_id, after, 0L, 0L);
			  		logger.info("postStatusModifyV3All : "+request_id+": check:"+check+"//"+seller_id);
			  		if(check.startsWith("error:")==true) {
			  			error_code = 113;
			  			error_val = check;
			  		}else {
			  			order_get_domain = gson.fromJson(check, order_get_domain.getClass());
			  			if(EmptyUtils.isEmpty(order_get_domain)==false) {
			  				moim_get_data_list.addAll(order_get_domain.getData());
			  			}
			  		}
			  		if(EmptyUtils.isEmpty(order_get_domain.getData())==true) {
			  			logger.info("postStatusModifyV3All : "+request_id+": 111"+"//"+seller_id);	
			  			break;
			  		}else {
			  			if(order_get_domain.getData().size()==0) {
			  				logger.info("postStatusModifyV3All : "+request_id+": 222"+"//"+seller_id);
			  				break;
			  			}
			  		}
		  			if(EmptyUtils.isEmpty(order_get_domain.getPaging())==false) {
		  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getTotal())==false){
		  					order_new_cnt = order_get_domain.getPaging().getTotal();
		  				}
		  				if(EmptyUtils.isEmpty(order_get_domain.getPaging().getAfter())==false) {
		  					after = order_get_domain.getPaging().getAfter();
		  				}else {
		  					logger.info("postStatusModifyV3All : "+request_id+": 333"+"//"+seller_id);
		  					break;
		  				}
		  			}
		  			
				}
				
				logger.info("postStatusModifyV3All : "+request_id+": total : order_new_cnt:"+order_new_cnt);
				if(order_new_cnt != moim_get_data_list.size() || order_new_cnt == 0) {
					logger.info("postStatusModifyV3All : "+request_id+": error ==============================");
					logger.info("postStatusModifyV3All : "+request_id+": order_new_cnt: "+order_new_cnt+ "// seller_id: "+seller_id);
					logger.info("postStatusModifyV3All : "+request_id+": moim_get_data_list.size(): "+moim_get_data_list.size());
					logger.info("postStatusModifyV3All : "+request_id+": end ==============================");
					
					error_code = 500;
					async_error = "moim_get_data_list.size(): "+moim_get_data_list.size()+"//"
							+ "order_new_cnt: "+order_new_cnt+ "// seller_id: "+seller_id;
					error_val = async_error;  
					
				}else {
					error_code = 0; 
					logger.info("postStatusModifyV3All : "+request_id+": order_new_cnt: "+order_new_cnt+"//"+seller_id);
					if(moim_get_data_list.size()>0) {
						for (int i = 0; i < moim_get_data_list.size(); i++) {
							OrderGetDomain.productVarinatSingle item = moim_get_data_list.get(i);
							if ("paid".equals(item.getStatus())
						        && "paid".equals(item.getPurchase().getStatus())
						        && "paid".equals(item.getDisplayingStatus())) {

						        purchase_send_ids.add(item.getId());
						        status_put_send_cu_ids.add(item.getPurchaseId());
						    }
						}	
					}
				}
				
				//------------------------------------------
				// step 1-1 : 비동기 시작 로그 저장 : 모든 신규 주문 가져와서 무조건 로그 저장으로 시작
				
				String order_ids_all = gson.toJson(purchase_send_ids);
				StatusProcessAsyncLog insert_data = new StatusProcessAsyncLog();
				if(error_code==0) {
					process_status = "READY";
					async_yn = "";
				}else {
					process_status = "DONE";
					async_yn = "N";
					if(EmptyUtils.isEmpty(async_error)==true) {
						async_error = error_val;
					}
				}
				insert_data.setRequest_id(request_id);
				insert_data.setSeller_id(seller_id);
				insert_data.setOrder_data_all(order_ids_all);
				insert_data.setProcess_status(process_status);
				insert_data.setOrder_status_result("");
				insert_data.setAsync_yn(async_yn);
				insert_data.setAsync_error(async_error);
				insert_data.setTotal_count(order_new_cnt);
				insert_data.setSuccess_count(0);
				insert_data.setFailure_count(0);
				insert_data.setUnavailable_count(0);
									
				int iRow = 0;
				iRow = asyncService.insertStatusProcessAsyncLog(insert_data);
				logger.info("postStatusModifyV3All : "+request_id+": log 1 insert iRow : "+iRow+"//"+seller_id);
				logger.info("postStatusModifyV3All : "+request_id+": log 1 insert idx : "+insert_data.getIdx()+"//"+seller_id);
					
				//------------------------------------------
				// step 2 : 전체 주문 30개씩 상태변경, 택배접수 처리  
				// 새로운 주문 전체 moim_get_data_list 있음. 
				// status_put_send_cu_ids : cu list 전체 
				// 
				if(error_code==0) {
					status_put_send_cu_ids = status_put_send_cu_ids.stream().distinct().collect(Collectors.toList());
					if(status_put_send_cu_ids.size() > 0) {
						
						List<String> status_modify_failures = new ArrayList<String>();
						List<String> status_modify_success = new ArrayList<String>();
						List<UnavailablesStatusModify> status_modify_unavailables = new ArrayList<UnavailablesStatusModify>();
						
						for (int i = 0; i < status_put_send_cu_ids.size(); i+=batchSize) {
							logger.info("postStatusModifyV3All : "+request_id+": status_put_send_cu_ids for i : "+i+"//"+seller_id);
							int end = Math.min(i + batchSize, status_put_send_cu_ids.size());
							List<String> group_ids = status_put_send_cu_ids.subList(i, end);
							StatusModifyResultReturn status_modify_result_domaim = new StatusModifyResultReturn();
							status_modify_log = status_modify_log + "// purchase_ids_array : "+gson.toJson(group_ids);
							
							SetDomain.StatusModifyReturn result = new SetDomain.StatusModifyReturn();
							result = moimApiService.OrderStatusModifyPreparingFroDelivery(connect_type, seller_id, group_ids);
							status_modify_log = status_modify_log + "// modify modify_result_string : "+gson.toJson(result);
							
							if(result.getError_code()==0) {
								status_modify_result_domaim = gson.fromJson(result.getResult_return(), StatusModifyResultReturn.class);
								status_modify_failures.addAll(status_modify_result_domaim.getResult().getFailures());
								status_modify_success.addAll(status_modify_result_domaim.getResult().getSuccess());
								status_modify_unavailables.addAll(status_modify_result_domaim.getResult().getUnavailables());
							}else {
								error_code = 500;
								error_val = result.getError_val();
							}
						}
						
						if(error_code==0) {
							//=================================================
							logger.info("postStatusModifyV3All : "+request_id+": "
									+ "status ModifyResult failures all:"+gson.toJson(status_modify_failures)+"//"+seller_id);
							logger.info("postStatusModifyV3All : "+request_id+": "
									+ "status ModifyResult success all:"+gson.toJson(status_modify_success)+"//"+seller_id);
							logger.info("postStatusModifyV3All : "+request_id+": "
									+ "status ModifyResult unavailables all:"+gson.toJson(status_modify_unavailables)+"//"+seller_id);
							ModifyResult.setFailures(status_modify_failures);
							ModifyResult.setSuccess(status_modify_success);
							ModifyResult.setUnavailables(status_modify_unavailables);
							
							logger.info("postStatusModifyV3All : "+request_id+": ModifyResult All:"+gson.toJson(ModifyResult)+"//"+seller_id);
						}
						
					}
				}
				
				if(error_code==0) {
					// 모임에서 paid 상태변경이 안되는 건 특별한 경우임. : 서버오류, 처리시점에서 paid가 아님.  
					// 자동택배접수 : 자동으로 다음 상태로 변경됨. 
					// 수동택배접수 : OrderStatusModifyPreparingFroDelivery 변경이 되어야 어부앱>엑셀 다운 받음.  
					// moim_get_data_list > grouprequestModifyBody 만들어야 함.
					StatusModifyResultReturnDelivery mergedDeliveryResult = new StatusModifyResultReturnDelivery();
				    StatusModifyResultReturnDelivery.DeliveryReceptionResult mergedResult = new StatusModifyResultReturnDelivery.DeliveryReceptionResult();
				    StatusModifyResultReturnDelivery.DeliveryReceptionResult.ResultV2 mergedV2 = new StatusModifyResultReturnDelivery.DeliveryReceptionResult.ResultV2();
				    mergedV2.setSuccess(new ArrayList<>());
				    mergedV2.setFailures(new ArrayList<>());
				    mergedV2.setUnavailables(new ArrayList<>());

				    mergedResult.setV2(mergedV2);
				    mergedDeliveryResult.setResult(mergedResult);
				    
					//======================================================
					// 전체 주문 30개씩 처리 돌림
					for (int j = 0; j < moim_get_data_list.size(); j += batchSize) {
					    int end = Math.min(j + batchSize, moim_get_data_list.size());
					    List<OrderGetDomain.productVarinatSingle> chunkItems =
					            new ArrayList<>(moim_get_data_list.subList(j, end));
					    grouprequestModifyBody = buildGroupedModifyBodyFromOrderItems(chunkItems, sellerid_info);
						logger.info("postStatusModifyV3All : "+request_id+": "
								+ "grouprequestModifyBody from moim_get_data_list: "+ gson.toJson(grouprequestModifyBody)+"//"+seller_id);
					    List<String> purchase_send_id_list = new ArrayList<>();
					    for (OrderGetDomain.productVarinatSingle item : chunkItems) {
					        if ("paid".equals(item.getStatus())
					                && "paid".equals(item.getPurchase().getStatus())
					                && "paid".equals(item.getDisplayingStatus())) {
					        	purchase_send_id_list.add(item.getId());
					        }
					    }
					    List<String> deliveriesBodies = buildDeliveryReceptionBodies(
					            chunkItems,
					            purchase_send_id_list,
					            buildPurchaseIdToCourierId(grouprequestModifyBody)
					    );

						// =====================================================
						// STEP 3-3. 그룹별 배송 접수 호출 : 분리한 CU: 그룹들 
						// =====================================================
					    
					    for (int i = 0; i < deliveriesBodies.size(); i++) {
					        String body = deliveriesBodies.get(i);

					        if (EmptyUtils.isEmpty(body)) continue;
					        logger.info("postStatusModifyV3All : "+request_id+": deliveriesBody[" + i + "] = " + body+"//"+seller_id);
					        
					        delivery_reception_log = delivery_reception_log+"//deliveries_body_single : "+body;
					        SetDomain.StatusModifyReturn apiResult =
					                moimApiService.OrderDeliveryReception(connect_type, seller_id, body);
					        
					        if (apiResult.getError_code() > 0) {
					            error_code = apiResult.getError_code();
					            error_val  = apiResult.getError_val();
					            break;
					        }
					        if (EmptyUtils.isEmpty(apiResult.getResult_return())) continue;
					        delivery_reception_log = delivery_reception_log+"// result_return["+i+"] : delivery_reception_moimapi_result : "+apiResult.getResult_return();
					        StatusModifyResultReturnDelivery single =
					                gson.fromJson(apiResult.getResult_return(), StatusModifyResultReturnDelivery.class);
					        if (single == null || single.getResult() == null || single.getResult().getV2() == null) continue;
					        mergeV2List(mergedV2.getSuccess(), single.getResult().getV2().getSuccess());
					        mergeV2List(mergedV2.getFailures(), single.getResult().getV2().getFailures());
					        mergeV2List(mergedV2.getUnavailables(), single.getResult().getV2().getUnavailables());
					    }
					}
					delivery_reception_result_domain = mergedDeliveryResult;
				}
				if(error_code ==0) {
				    Map<String, OrderGetDomain.productVarinatSingle> orderItemMap = new HashMap<>();
				    for (OrderGetDomain.productVarinatSingle item : moim_get_data_list) {
				        orderItemMap.put(item.getId(), item); 
				    }
				    StatusModifyResultReturnDelivery.DeliveryReceptionResult.ResultV2 v2 =
				    		delivery_reception_result_domain.getResult().getV2();
				    buildReturnListFromV2(
				        v2.getSuccess(),
				        orderItemMap,
				        success_result_list,
				        0,
				        null
				    );
				    buildReturnListFromV2(
				        v2.getFailures(),
				        orderItemMap,
				        failure_result_list,
				        1,
				        "배송 접수 실패"
				    );
				    buildReturnListFromV2(
				        v2.getUnavailables(),
				        orderItemMap,
				        unavailable_result_list,
				        2,
				        "배송 접수 불가"
				    );
				}
				if(error_code ==0) {
					
					if(success_result_list.size()>0) {
						modify_result.setSuccess(success_result_list);  
					}else {
						modify_result.setSuccess(success_result_list); 
					}
					modify_result.setFailures(failure_result_list);	
					modify_result.setUnavailables(unavailable_result_list);
					///////////////////////////////////////////////////////////////////////////////////////////////////////////////// 
					modify_result.setAll_process_yn("Y"); // 대용량 처리
					/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					return_val.setResult_return(modify_result);
					
					logger.info("postStatusModifyV3All : "+request_id+": "
							+ "return_val.setResult_return :"+gson.toJson(return_val.getResult_return())+"//"+seller_id);
				}
				
				if(error_code==0) {
					
					String now_date_time_end = LocationTimeCal.GetNowDateTime();
					status_modify_log = status_modify_log + "]//now_date_time_end:"+now_date_time_end;
					delivery_reception_log = delivery_reception_log + "]//now_date_time_end:"+now_date_time_end;

					logger.info("postStatusModifyV3All : "+request_id+": status_modify_log :"+status_modify_log+"//"+seller_id);
					logger.info("postStatusModifyV3All : "+request_id+": delivery_reception_log :"+delivery_reception_log+"//"+seller_id);
					
					String data_val = "";
					String result_failures = "";
					String result_success = "";
					String result_unavailables = "";
					data_val = "|| rquest_id:"+request_id+"//"+gson.toJson(order_ids_all); 
					if(EmptyUtils.isEmpty(modify_result.getFailures())==false) {
						result_failures = gson.toJson(modify_result.getFailures());	
					}else {
						result_failures = "[result_failures empty]";
					}
					if(EmptyUtils.isEmpty(modify_result.getSuccess())==false) {
						result_success = gson.toJson(modify_result.getSuccess());	
					}else {
						result_success = "[result_success empty]";
					}
					if(EmptyUtils.isEmpty(modify_result.getUnavailables())==false) {
						result_unavailables = gson.toJson(modify_result.getUnavailables());	
					}else {
						result_unavailables = "[result_unavailables empty]";
					}
					Model model_ins = new ExtendedModelMap();
					model_ins.addAttribute("seller_id", seller_id);
					model_ins.addAttribute("modify_key", requestModifyBody.getModify_key());
					model_ins.addAttribute("modify_status", requestModifyBody.getModify_status());
					model_ins.addAttribute("data_val", data_val);
					model_ins.addAttribute("status_modify", status_modify_log); 
					model_ins.addAttribute("delivery_reception", delivery_reception_log); 
					model_ins.addAttribute("result_failures", result_failures);
					model_ins.addAttribute("result_success", result_success);
					model_ins.addAttribute("result_unavailables", result_unavailables);
					try {
						
						int iiRow = 0;
						iiRow = orderService.insertPluginStatusPorcessLog(model_ins);
						logger.info("postStatusModifyV3All : "+request_id+": log update upRow : "+iiRow+"//"+seller_id);
						
					} catch (Exception e) {
						// 실패시 지메일에서 회사계정으로 메일 발송 
						sendMail("postStatusModifyV3All : "+request_id, seller_id, sellerid_info.getName()+"//PorcessLog//", "");
					}
					
					String alarm_send = "";// succes, failure
					if(success_result_list.size()>0) {
						alarm_send = AlarmTalkSeller(success_result_list, connect_type, sellerid_info, seller_token);	
					}else if(failure_result_list.size()>0) {
						alarm_send = AlarmTalkSeller(failure_result_list, connect_type, sellerid_info, seller_token);
					}else if(unavailable_result_list.size()>0) {
						alarm_send = AlarmTalkSeller(unavailable_result_list, connect_type, sellerid_info, seller_token);							
					}
					
					if(EmptyUtils.isEmpty(alarm_send)==false) {
						modify_result.setAlarm_send(alarm_send);
					}else {
						modify_result.setAlarm_send("N");
					}
				}
				
				String ins_modify_result = "";
				if(EmptyUtils.isEmpty(ModifyResult)==false) {
					ins_modify_result = gson.toJson(ModifyResult);
				}
				if(error_code == 0) {
					process_status = "DONE";
					async_yn = "Y";
					async_error = "";
				}else {
					process_status = "DONE";
					async_yn = "N";
					async_error = error_val;
					logger.info("postStatusModifyV3All : "+request_id+": log update : error_val : "+async_error+"//"+seller_id);
				}
				int delivery_success_count = 0;
				int delivery_failures_count = 0;
				int delivery_unavailables_count = 0;
				int variant_count = 0;
				for (int j = 0; j < modify_result.getSuccess().size(); j++) {
					if(modify_result.getSuccess().get(j).getProduct_variant_list().size()>0) {
						variant_count += modify_result.getSuccess().get(j).getProduct_variant_list().size();
					}
				}
				delivery_success_count = variant_count;
				
				variant_count = 0;
				for (int j = 0; j < modify_result.getFailures().size(); j++) {
					if(modify_result.getFailures().get(j).getProduct_variant_list().size()>0) {
						variant_count += modify_result.getFailures().get(j).getProduct_variant_list().size();
					}
				}
				delivery_failures_count = variant_count;
				
				variant_count = 0;
				for (int j = 0; j < modify_result.getUnavailables().size(); j++) {
					if(modify_result.getUnavailables().get(j).getProduct_variant_list().size()>0) {
						variant_count += modify_result.getUnavailables().get(j).getProduct_variant_list().size();
					}
				}
				delivery_unavailables_count = variant_count;
				
				Model model_up = new ExtendedModelMap();
				model_up.addAttribute("request_id", request_id);
				model_up.addAttribute("seller_id", seller_id);
				model_up.addAttribute("process_status", process_status);
				model_up.addAttribute("order_status_result", ins_modify_result);
				model_up.addAttribute("async_yn", async_yn);
				model_up.addAttribute("async_error", async_error);
				model_up.addAttribute("success_count", delivery_success_count);
				model_up.addAttribute("failure_count", delivery_failures_count);
				model_up.addAttribute("unavailable_count", delivery_unavailables_count);
				model_up.addAttribute("alarm_send", modify_result.getAlarm_send());
				
				int upRow = 0;
				upRow = asyncService.updateStatusProcessAsyncLog(model_up);
				logger.info("postStatusModifyV3All : "+request_id+": log update upRow : "+upRow+"//"+seller_id);
				
				if(async_yn == "N") {
					//=======================================
					// 실패시 메일 발송 
					sendMail("postStatusModifyV3All : "+request_id, seller_id, sellerid_info.getName()+"//controller//", async_error);
					//=======================================
				}
			}
		}
		return_val.setError_code(error_code);
		return_val.setError_val(error_val);
		return return_val;
		
	}
	

	/**
	 * @desc  
	 */
	// 모든 주문안에 CI: data_id로 한번에 상태변경, 배송접수 처리 로직 // 관망중은 동일함. 
	public SetDomain.ControllerResultStatusModifyReturnV2 postStatusModifyV3(Model model
			, String connect_type, String seller_id
			, OrderBodyDomain.ModifyBodyList requestModifyBody, SellerIdInfoDomain sellerid_info, String seller_token) {
		
		SetDomain.ControllerResultStatusModifyReturnV2 return_val = new SetDomain.ControllerResultStatusModifyReturnV2(); 
		int error_code = 0;
		String error_val = "";
		ReturnModifyResultV2 modify_result = new ReturnModifyResultV2();
		String status_modify_log = "";
		String delivery_reception_log = "";
		logger.info("postStatusModifyV3 now:"+LocationTimeCal.GetNowDateTime()+"//"+seller_id);
		String now_date_time_start = LocationTimeCal.GetNowDateTime();
		status_modify_log = "//now_date_time_start:"+now_date_time_start+"//[";
		delivery_reception_log = "//now_date_time_start:"+now_date_time_start+"//["; 
		
		// body check
		if(requestModifyBody.getModify_key()>0 && requestModifyBody.getModify_status()>0
				&& requestModifyBody.getData().size()>0 && error_code ==0) {
			
			if(requestModifyBody.getModify_key()==1) {
				if(requestModifyBody.getModify_status()==2) {
					ReturnModifyResultV2 returnResult = new ReturnModifyResultV2();
				    List<OrderDomain.ReturnDatav1> success_result_list = new ArrayList<>();
				    List<OrderDomain.ReturnDatav1> failure_result_list = new ArrayList<>();
				    List<OrderDomain.ReturnDatav1> unavailable_result_list = new ArrayList<>();
					List<String> purchase_send_ids = new ArrayList<String>();
					List<String> status_put_send_cu_ids = new ArrayList<String>();
					logger.info("postStatusModifyV3 1 body-->"+gson.toJson(requestModifyBody)+"//"+seller_id);
					List<GroupedModifyBody> grouprequestModifyBody = new ArrayList<GroupedModifyBody>();
					grouprequestModifyBody = groupByOrderKey(requestModifyBody);
					logger.info("postStatusModifyV3 2 groupByOrderKey "
							+ ":body-->"+gson.toJson(grouprequestModifyBody)+"//"+seller_id);
					if(grouprequestModifyBody.size()>0) {
						OrderGetDomain.GetData orderinfo_moim_get = new OrderGetDomain.GetData();
						List<String> order_info_get_ids_moim_send = new ArrayList<String>();
						StatusModifyResultReturnDelivery delivery_reception_result_domaim = new StatusModifyResultReturnDelivery();
						
						for (int i = 0; i < grouprequestModifyBody.size(); i++) {
							order_info_get_ids_moim_send.addAll(grouprequestModifyBody.get(i).getProduct_variant_key());
						}
						logger.info("postStatusModifyV3 order_info_get_ids_moim_send "
								+ "CI list:"+gson.toJson(order_info_get_ids_moim_send)+"//"+seller_id);
						String get_order_single = "";
						
						get_order_single = moimApiService.OrderSingleGet(connect_type, seller_id, order_info_get_ids_moim_send);
						orderinfo_moim_get = gson.fromJson(get_order_single, orderinfo_moim_get.getClass());
						logger.info("postStatusModifyV3 orderinfo_moim_get.getData().size()"
								+ ":"+orderinfo_moim_get.getData().size()+"//"+seller_id);

						for (int i = 0; i < orderinfo_moim_get.getData().size(); i++) {
						    OrderGetDomain.productVarinatSingle item =
						        orderinfo_moim_get.getData().get(i);

						    logger.info("postStatusModifyV3 getStatus: " + item.getStatus()+"//"+seller_id);
						    logger.info("postStatusModifyV3 getPurchaseStatus: " + item.getPurchase().getStatus()+"//"+seller_id);
						    logger.info("postStatusModifyV3 getPurchaseId: " + item.getPurchaseId()+"//"+seller_id);
						    logger.info("postStatusModifyV3 getDeliveryGroupId: " + item.getDeliveryGroupId()+"//"+seller_id);

						    if ("paid".equals(item.getStatus())
						        && "paid".equals(item.getPurchase().getStatus())
						        && "paid".equals(item.getDisplayingStatus())) {

						        purchase_send_ids.add(item.getId());
						        status_put_send_cu_ids.add(item.getPurchaseId()); 
						    }

						    for (GroupedModifyBody g : grouprequestModifyBody) {
						        if (g.getOrder_key().equals(item.getPaymentId())) {
						            g.setOrder_id(item.getPurchaseId());
						        }
						    }
						}

						logger.info("postStatusModifyV3 purchase_send_ids: " + gson.toJson(purchase_send_ids)+"//"+seller_id);
						logger.info("postStatusModifyV3 grouprequestModifyBody:groupByOrderKey body "
						            + gson.toJson(grouprequestModifyBody)+"//"+seller_id);
						status_put_send_cu_ids = status_put_send_cu_ids.stream().distinct().collect(Collectors.toList());
						status_modify_log = status_modify_log + "// purchase_ids_array : "+gson.toJson(status_put_send_cu_ids);
						if(status_put_send_cu_ids.size() > 0) {
							SetDomain.StatusModifyReturn  modify_result_string = new SetDomain.StatusModifyReturn();
							modify_result_string = moimApiService.OrderStatusModifyPreparingFroDelivery(
									connect_type, seller_id, status_put_send_cu_ids);
							status_modify_log = status_modify_log + "// modify modify_result_string : "+gson.toJson(modify_result_string);
						}
						
						if (error_code == 0) {

						    // =====================================================
						    // 배송 접수 결과 누적 객체 초기화
						    // =====================================================
						    StatusModifyResultReturnDelivery mergedDeliveryResult = new StatusModifyResultReturnDelivery();
						    StatusModifyResultReturnDelivery.DeliveryReceptionResult mergedResult = new StatusModifyResultReturnDelivery.DeliveryReceptionResult();
						    StatusModifyResultReturnDelivery.DeliveryReceptionResult.ResultV2 mergedV2 = new StatusModifyResultReturnDelivery.DeliveryReceptionResult.ResultV2();

						    mergedV2.setSuccess(new ArrayList<>());
						    mergedV2.setFailures(new ArrayList<>());
						    mergedV2.setUnavailables(new ArrayList<>());

						    mergedResult.setV2(mergedV2);
						    mergedDeliveryResult.setResult(mergedResult);
						   
						    List<String> deliveriesBodies = buildDeliveryReceptionBodies(
						            orderinfo_moim_get.getData(), 
						            purchase_send_ids,
						            buildPurchaseIdToCourierId(grouprequestModifyBody)
						    );

						    for (int i = 0; i < deliveriesBodies.size(); i++) {
						        String body = deliveriesBodies.get(i);

						        if (EmptyUtils.isEmpty(body)) continue;
						        logger.info("postStatusModifyV3 deliveriesBody[" + i + "] = " + body);
						        
						        delivery_reception_log = delivery_reception_log+"//deliveries_body_single : "+body;
						        SetDomain.StatusModifyReturn apiResult =
						                moimApiService.OrderDeliveryReception(connect_type, seller_id, body);

						        if (apiResult.getError_code() > 0) {
						            error_code = apiResult.getError_code();
						            error_val  = apiResult.getError_val();
						            break;
						        }
						        
						        if (EmptyUtils.isEmpty(apiResult.getResult_return())) continue;
						        delivery_reception_log = delivery_reception_log+"// result_return["+i+"] : delivery_reception_moimapi_result : "+apiResult.getResult_return();
						        StatusModifyResultReturnDelivery single =
						                gson.fromJson(apiResult.getResult_return(), StatusModifyResultReturnDelivery.class);

						        if (single == null || single.getResult() == null || single.getResult().getV2() == null) continue;

						        mergeV2List(mergedV2.getSuccess(), single.getResult().getV2().getSuccess());
						        mergeV2List(mergedV2.getFailures(), single.getResult().getV2().getFailures());
						        mergeV2List(mergedV2.getUnavailables(), single.getResult().getV2().getUnavailables());
						    }

						    delivery_reception_result_domaim = mergedDeliveryResult;
						}
						if(error_code ==0) {
							
						    Map<String, OrderGetDomain.productVarinatSingle> orderItemMap = new HashMap<>();
						    for (OrderGetDomain.productVarinatSingle item : orderinfo_moim_get.getData()) {
						        orderItemMap.put(item.getId(), item);
						    }

						    StatusModifyResultReturnDelivery.DeliveryReceptionResult.ResultV2 v2 =
						        delivery_reception_result_domaim.getResult().getV2();
						    buildReturnListFromV2(
						        v2.getSuccess(),
						        orderItemMap,
						        success_result_list,
						        0,
						        null
						    );

						    buildReturnListFromV2(
						        v2.getFailures(),
						        orderItemMap,
						        failure_result_list,
						        1,
						        "배송 접수 실패"
						    );

						    buildReturnListFromV2(
						        v2.getUnavailables(),
						        orderItemMap,
						        unavailable_result_list,
						        2,
						        "배송 접수 불가"
						    );

						    returnResult.setSuccess(success_result_list);
						    returnResult.setFailures(failure_result_list);
						    returnResult.setUnavailables(unavailable_result_list);

						}
						if(error_code ==0) {
							if(success_result_list.size()>0) {
								modify_result.setSuccess(success_result_list);
								if(error_code == 0) {  
									String alarm_send = "";
									alarm_send = AlarmTalkSeller(success_result_list, connect_type, sellerid_info, seller_token);
									if(EmptyUtils.isEmpty(alarm_send)==false) {
										modify_result.setAlarm_send(alarm_send);
									}
								}
							}else {
								modify_result.setSuccess(success_result_list); 
							}
							modify_result.setFailures(failure_result_list);	
							modify_result.setUnavailables(unavailable_result_list);
							/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
							modify_result.setAll_process_yn("N"); // 대용량 처리가 아니라는 뜻. 개별 선택이거나 30개 이하 처리는 결과가 있음.
							/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
							
							return_val.setResult_return(modify_result);
							
							logger.info("postStatusModifyV3 return_val.setResult_return :"+gson.toJson(return_val.getResult_return())+"//"+seller_id);
						}
						
						if(error_code==0) {
							String now_date_time_end = LocationTimeCal.GetNowDateTime();
							status_modify_log = status_modify_log + "]//now_date_time_end:"+now_date_time_end;
							delivery_reception_log = delivery_reception_log + "]//now_date_time_end:"+now_date_time_end;
							
							logger.info("postStatusModifyV3 status_modify_log :"+status_modify_log+"//"+seller_id);
							logger.info("postStatusModifyV3 delivery_reception_log :"+delivery_reception_log+"//"+seller_id);
							
							//======================================================
							// 가능으로 변경 : 배송 신청 결과 로그 저장하는 곳
							String data_val = "";
							String result_failures = "";
							String result_success = "";
							String result_unavailables = "";
							data_val = gson.toJson(grouprequestModifyBody);
							if(EmptyUtils.isEmpty(modify_result.getFailures())==false) {
								result_failures = gson.toJson(modify_result.getFailures());	
							}else {
								result_failures = "[result_failures empty]";
							}
							if(EmptyUtils.isEmpty(modify_result.getSuccess())==false) {
								result_success = gson.toJson(modify_result.getSuccess());	
							}else {
								result_success = "[result_success empty]";
							}
							if(EmptyUtils.isEmpty(modify_result.getUnavailables())==false) {
								result_unavailables = gson.toJson(modify_result.getUnavailables());	
							}else {
								result_unavailables = "[result_unavailables empty]";
							}
							Model model_ins = new ExtendedModelMap();
							model_ins.addAttribute("seller_id", seller_id);
							model_ins.addAttribute("modify_key", requestModifyBody.getModify_key());
							model_ins.addAttribute("modify_status", requestModifyBody.getModify_status());
							model_ins.addAttribute("data_val", data_val);
							model_ins.addAttribute("status_modify", status_modify_log); 
							model_ins.addAttribute("delivery_reception", delivery_reception_log); 
							model_ins.addAttribute("result_failures", result_failures);
							model_ins.addAttribute("result_success", result_success);
							model_ins.addAttribute("result_unavailables", result_unavailables);
							orderService.insertPluginStatusPorcessLog(model_ins);
							//======================================================			
						}
						
					}
					else {
						error_code = 120;
						error_val = " empty requestModifyBody or convert error  ";
					}
				}
			}
		}
		
		return_val.setError_code(error_code);
		return_val.setError_val(error_val);
		return return_val;
		
	}
	

	/**
	 * @desc  
	 */
	private List<GroupedModifyBody> buildGroupedModifyBodyFromOrderItems(
	        List<OrderGetDomain.productVarinatSingle> orderItems, SellerIdInfoDomain sellerid_info
	) {
	    Map<String, GroupedModifyBody> groupedMap = new LinkedHashMap<>();

	    for (OrderGetDomain.productVarinatSingle item : orderItems) {
	        if (item == null) continue;
	        if (EmptyUtils.isEmpty(item.getPaymentId())) continue;

	        String orderKey = item.getPaymentId();

	        GroupedModifyBody grouped = groupedMap.computeIfAbsent(orderKey, k -> {
	            GroupedModifyBody g = new GroupedModifyBody();
	            g.setOrder_key(orderKey);
	            g.setOrder_id(item.getPurchaseId());
	            g.setStatus_val("");
	            g.setDelivery_val("");
	            g.setDelivery_group_id("");

	            String courierId = "";
	            if (EmptyUtils.isEmpty(sellerid_info.getDelivery_contracts_id()) == false) {
	                courierId = sellerid_info.getDelivery_contracts_id();
	            } else if (EmptyUtils.isEmpty(sellerid_info.getDefaultDeliveryContractId()) == false) {
	                courierId = sellerid_info.getDefaultDeliveryContractId();
	            }
	            g.setCourier_id(courierId);

	            return g;
	        });

	        if (EmptyUtils.isEmpty(item.getId()) == false) {
	            grouped.getProduct_variant_key().add(item.getId());
	        }

	        if (EmptyUtils.isEmpty(grouped.getOrder_id())
	                && EmptyUtils.isEmpty(item.getPurchaseId()) == false) {
	            grouped.setOrder_id(item.getPurchaseId());
	        }
	    }

	    return new ArrayList<>(groupedMap.values());
	}
	

	/**
	 * @desc  
	 */
	public class DeliveryBodyBuilder {
	    public static String build(
	            List<GroupedModifyBody> groupList,
	            List<String> purchase_send_ids
	    ) {
	        StringBuilder sb = new StringBuilder();
	        sb.append("{\"deliveries\":{");
	        boolean hasAny = false;
	        for (GroupedModifyBody body : groupList) {
	            List<String> validItems = DuplicateUtil.findDuplicates(
	                body.getProduct_variant_key(),
	                purchase_send_ids
	            );
	            if (validItems == null || validItems.isEmpty()) {
	                continue;
	            }
	            if (hasAny) {
	                sb.append(",");
	            }
	            sb.append("\"")
	              .append(body.getOrder_id())
	              .append("\":{");
	            sb.append("\"purchaseItemIds\":[");
	            for (int i = 0; i < validItems.size(); i++) {
	                if (i > 0) sb.append(",");
	                sb.append("\"").append(validItems.get(i)).append("\"");
	            }
	            sb.append("]");
	            if (!EmptyUtils.isEmpty(body.getCourier_id())) {
	                sb.append(",\"deliveryContractId\":\"")
	                  .append(body.getCourier_id())
	                  .append("\"");
	            }
	            sb.append("}");
	            hasAny = true;
	        }
	        sb.append("}}");
	        return hasAny ? sb.toString() : "";
	    }
	}
	

	/**
	 * @desc  
	 */
	private void mergeV2List(
		    List<ResultV2TypeSingleData> target,
		    List<ResultV2TypeSingleData> source
		) {
		    if (source == null) return;

		    Set<String> exists = target.stream()
		        .map(v -> v.getData().getPurchaseId() + "|" + v.getData().getItems())
		        .collect(Collectors.toSet());

		    for (ResultV2TypeSingleData s : source) {
		        String key = s.getData().getPurchaseId() + "|" + s.getData().getItems();
		        if (!exists.contains(key)) {
		            target.add(s);
		        }
		    }
		}
	

	/**
	 * @desc  
	 */
	private List<String> buildDeliveryReceptionBodies(
	        List<OrderGetDomain.productVarinatSingle> orderItems,
	        List<String> purchase_send_ids,
	        Map<String, String> purchaseIdToCourierId
	) {
	    Set<String> validCiSet = new HashSet<>(purchase_send_ids);
	    Map<String, Map<String, List<String>>> purchaseMap = new LinkedHashMap<>();

	    for (OrderGetDomain.productVarinatSingle item : orderItems) {
	        if (item == null) continue;
	        if (!validCiSet.contains(item.getId())) continue;

	        String purchaseId = item.getPurchaseId();
	        String groupKey = EmptyUtils.isEmpty(item.getDeliveryGroupId())
	                ? "NO_GROUP"
	                : item.getDeliveryGroupId();

	        purchaseMap
	            .computeIfAbsent(purchaseId, k -> new LinkedHashMap<>())
	            .computeIfAbsent(groupKey, k -> new ArrayList<>())
	            .add(item.getId());
	    }

	    List<String> resultBodies = new ArrayList<>();
	    Map<String, DeliverySpec> batchDeliveries = new LinkedHashMap<>();

	    for (Map.Entry<String, Map<String, List<String>>> entry : purchaseMap.entrySet()) {
	        String purchaseId = entry.getKey();
	        Map<String, List<String>> groupMap = entry.getValue();

	        if (groupMap.size() >= 2) {
	            for (List<String> items : groupMap.values()) {
	                Map<String, DeliverySpec> single = new LinkedHashMap<>();
	                single.put(
	                        purchaseId,
	                        new DeliverySpec(items, purchaseIdToCourierId.get(purchaseId))
	                );
	                resultBodies.add(buildDeliveriesJson(single));
	            }
	        }
	        else {
	            List<String> items = groupMap.values().iterator().next();
	            batchDeliveries.put(
	                    purchaseId,
	                    new DeliverySpec(items, purchaseIdToCourierId.get(purchaseId))
	            );
	        }
	        logger.info("buildDeliveryReceptionBodies : purchaseId=" + purchaseId + " deliveryGroup keys=" + groupMap.keySet());
	    }

	    if (!batchDeliveries.isEmpty()) {
	        resultBodies.add(buildDeliveriesJson(batchDeliveries));
	    }
	    return resultBodies;
	}


	/**
	 * @desc  
	 */
	private String buildDeliveriesJson(Map<String, DeliverySpec> deliveries) {
	    StringBuilder sb = new StringBuilder();
	    sb.append("{\"deliveries\":{");
	    boolean first = true;
	    for (Map.Entry<String, DeliverySpec> e : deliveries.entrySet()) {
	        DeliverySpec spec = e.getValue();
	        if (spec.items == null || spec.items.isEmpty()) continue;

	        if (!first) sb.append(",");
	        first = false;
	        sb.append("\"").append(e.getKey()).append("\":{");
	        sb.append("\"purchaseItemIds\":[");
	        for (int i = 0; i < spec.items.size(); i++) {
	            if (i > 0) sb.append(",");
	            sb.append("\"").append(spec.items.get(i)).append("\"");
	        }
	        sb.append("]");
	        if (!EmptyUtils.isEmpty(spec.courierId)) {
	            sb.append(",\"deliveryContractId\":\"")
	              .append(spec.courierId)
	              .append("\"");
	        }
	        sb.append("}");
	    }
	    sb.append("}}");
	    return first ? "" : sb.toString();
	}
	
	/**
	 * @desc  
	 */
	private Map<String, String> buildPurchaseIdToCourierId(
	        List<GroupedModifyBody> grouprequestModifyBody
	) {
	    Map<String, String> map = new HashMap<>();
	    for (GroupedModifyBody g : grouprequestModifyBody) {
	        if (EmptyUtils.isEmpty(g.getOrder_id())) continue;
	        if (EmptyUtils.isEmpty(g.getCourier_id())) continue;
	        map.putIfAbsent(g.getOrder_id(), g.getCourier_id());
	    }
	    return map;
	}
	

	/**
	 * @desc  
	 */
	private void buildReturnListFromV2(
		    List<StatusModifyResultReturnDelivery.DeliveryReceptionResult.ResultV2.ResultV2TypeSingleData> v2List,
		    Map<String, OrderGetDomain.productVarinatSingle> orderItemMap,
		    List<OrderDomain.ReturnDatav1> targetList,
		    int reasonType,
		    String reason
		) {
		    if (v2List == null) return;

		    for (var v2 : v2List) {
		        List<String> items = v2.getData().getItems(); 

		        Map<String, List<OrderGetDomain.productVarinatSingle>> grouped =
		            items.stream()
		                .map(orderItemMap::get)
		                .filter(Objects::nonNull)
		                .collect(Collectors.groupingBy(
		                    o -> o.getPaymentId() + "|" + o.getProductId()
		                ));

		        for (List<OrderGetDomain.productVarinatSingle> groupItems : grouped.values()) {
		            OrderDomain.ReturnDatav1 r = buildReturnDatav1(groupItems);

		            if (reasonType == 1) r.setFailures_reason(reason);
		            if (reasonType == 2) r.setUnavailables_reason(reason);
		            if (reasonType == 0) r.setDelivery_reception("배송 접수 완료");

		            targetList.add(r);
		        }
		    }
		}
	

	/**
	 * @desc  
	 */
	private OrderDomain.ReturnDatav1 buildReturnDatav1(
		    List<OrderGetDomain.productVarinatSingle> items
		) {
		    OrderGetDomain.productVarinatSingle first = items.get(0);
		    OrderDomain.ReturnDatav1 r = new OrderDomain.ReturnDatav1();
		    r.setOrder_key(first.getPaymentId());
		    r.setOrder_status(first.getStatus());
		    r.setUser_id(first.getUserId());
		    r.setUser_name(first.getPurchase().getBuyerName());
		    r.setRecipient_name(first.getPurchase().getRecipientName());
		    r.setProduct_key(first.getProductId());
		    r.setProduct_name(first.getProductName());

		    if (first.getPaidAt() != null && first.getPaidAt() > 0) {
		        r.setPay_date(LocationTimeCal.TimeStamptoDate(first.getPaidAt()));
		    }
		    List<OrderDomain.ProductVariantSingle> variantList = new ArrayList<>();
		    for (OrderGetDomain.productVarinatSingle item : items) {
		        OrderDomain.ProductVariantSingle v = new OrderDomain.ProductVariantSingle();
		        v.setProduct_variant_key(item.getId()); // CI
		        v.setProduct_variant_id(item.getProductVariantId());
		        v.setProduct_variant_name(
		            item.getProductVariantValue() != null
		                ? item.getProductVariantValue().toString()
		                : ""
		        );
		        v.setQuantity(item.getQuantity());
		        variantList.add(v);
		    }

		    r.setProduct_variant_list(variantList);

		    return r;
		}


	/**
	 * @desc  
	 */
	public SetDomain.ControllerResultStatusModifyReturnV2 postStatusModifyV2(Model model, String connect_type, String seller_id
			, OrderBodyDomain.ModifyBodyList requestModifyBody, SellerIdInfoDomain sellerid_info, String seller_token) {
		
		SetDomain.ControllerResultStatusModifyReturnV2 return_val = new SetDomain.ControllerResultStatusModifyReturnV2(); 
		
		int error_code = 0;
		String error_val = "";
		
		ReturnModifyResultV2 modify_result = new ReturnModifyResultV2();
		
		String status_modify_log = "";
		String delivery_reception_log = "";
		status_modify_log = "[";
		delivery_reception_log = "["; 
		
		if(requestModifyBody.getModify_key()>0 && requestModifyBody.getModify_status()>0
				&& requestModifyBody.getData().size()>0 && error_code ==0) {
			
			if(requestModifyBody.getModify_key()==1) {
				if(requestModifyBody.getModify_status()==2) {
					List<String> status_modify_failure_id_list = new ArrayList<String>();
					List<String> status_modify_succes_id_list = new ArrayList<String>();
					List<String> status_modify_unavailable_id_list = new ArrayList<String>();

					List<OrderDomain.ReturnDatav1> failures_result_list = new ArrayList<OrderDomain.ReturnDatav1>();
					List<OrderDomain.ReturnDatav1> success_result_list = new ArrayList<OrderDomain.ReturnDatav1>();
					List<OrderDomain.ReturnDatav1> unavailables_result_list = new ArrayList<OrderDomain.ReturnDatav1>();
					
					List<GroupedModifyBody> grouprequestModifyBody = new ArrayList<GroupedModifyBody>();
					grouprequestModifyBody = groupByOrderKey(requestModifyBody);
					
					if(grouprequestModifyBody.size()>0) {
						for (int i = 0; i < grouprequestModifyBody.size(); i++) {
							logger.info("postStatusModifyV2 v2 ////////////////////////////////////////// start order_key");
							logger.info("postStatusModifyV2 order_key : paymentId :"+grouprequestModifyBody.get(i).getOrder_key()+"//"+seller_id);
							
							if(i>0) {
								if(status_modify_log.equals("[")==false) {
									status_modify_log = status_modify_log + ", ";	
								}
								delivery_reception_log = delivery_reception_log + ", ";
							}
							
							OrderGetDomain.GetData orderinfo_moim_get = new OrderGetDomain.GetData();
							StatusModifyResultReturn status_modify_result_domaim = new StatusModifyResultReturn();
							String status_pass = "pass"; 
							String send_purchaseId = "";
							String deliveries_body_single = "";
							SetDomain.StatusModifyReturn  delivery_reception_moimapi_result = new SetDomain.StatusModifyReturn();
							StatusModifyResultReturnDelivery delivery_reception_result_domaim = new StatusModifyResultReturnDelivery();
							List<String> order_info_get_ids_moim_send = new ArrayList<String>();
							order_info_get_ids_moim_send.addAll(grouprequestModifyBody.get(i).getProduct_variant_key());
							for (int j = 0; j < order_info_get_ids_moim_send.size(); j++) {
								logger.info("postStatusModifyV2 "+order_info_get_ids_moim_send.get(j)+":order_info_get_ids_moim_send"+"//"+seller_id);
							}
							try {
								String get_order_single = "";
								get_order_single = moimApiService.OrderSingleGet(connect_type, seller_id, order_info_get_ids_moim_send);
								orderinfo_moim_get = gson.fromJson(get_order_single, orderinfo_moim_get.getClass());
								logger.info("postStatusModifyV2 orderinfo_moim_get.getData().size():"+orderinfo_moim_get.getData().size()+"//"+seller_id);
								if(EmptyUtils.isEmpty(orderinfo_moim_get)==true) {
									error_code = 116;
									error_val = "moimApiService.OrderSingleGet empty";
								}
								
							} catch (Exception e) {
								error_code = 116;
								error_val = "moimApiService.OrderSingleGet e:"+e;
							}
							if(error_code == 0) {
								try {
									for (int j = 0; j < orderinfo_moim_get.getData().size(); j++) {
										logger.info("postStatusModifyV2 j:"+j+""
												+ "//"+orderinfo_moim_get.getData().get(j).getStatus()+"====>for getStatus"+"//"+seller_id);
									}
									
									send_purchaseId = orderinfo_moim_get.getData().get(0).getPurchaseId();
									logger.info("postStatusModifyV2 "+send_purchaseId+"=>send_purchaseId"+"//"+seller_id);
									
									if(orderinfo_moim_get.getData().get(0).getStatus().equals("paid")==true) {
										status_pass = "modify";// pass, modify
										logger.info("postStatusModifyV2 "+status_pass+"=>modify"+"//"+seller_id);
									}
									
									if(orderinfo_moim_get.getData().get(0).getStatus().equals("preparingForDelivery")==true) {
										for (int j = 0; j < orderinfo_moim_get.getData().size(); j++) {
											status_modify_succes_id_list.add(orderinfo_moim_get.getData().get(j).getId());
											logger.info("postStatusModifyV2 "+orderinfo_moim_get.getData().get(j).getId()+""
													+ "=>status_modify_succes_id_list add"+"//"+seller_id);
										}
									}
								} catch (Exception e) {
									error_code = 116;
									error_val = "orderinfo_moim_get list check e:"+e;
								}
							}
						
							if(error_code ==0) {
						
								if(status_pass.equals("modify")==true) {
									List<String> purchase_ids_array = new ArrayList<String>();
									purchase_ids_array.add(send_purchaseId);
									
									status_modify_log = status_modify_log + "{\"status_"+i+"_send_body\":";
									status_modify_log = status_modify_log + "{\"ids\":["+gson.toJson(purchase_ids_array)+"]}, \"status_send_result\": ";
									
									SetDomain.StatusModifyReturn  modify_result_string = new SetDomain.StatusModifyReturn();
									modify_result_string = moimApiService.OrderStatusModifyPreparingFroDelivery(connect_type, seller_id, purchase_ids_array);
									String result_return = modify_result_string.getResult_return();
									
									logger.info("postStatusModifyV2 moim OrderStatusModifyPreparingFroDelivery : "+result_return+"//"+seller_id);
									
									status_modify_log = status_modify_log + "{\"status_result_val\":"+result_return+"";
									status_modify_log = status_modify_log + "}}";
						
									if(EmptyUtils.isEmpty(result_return)==false) {
										try {
											status_modify_result_domaim = gson.fromJson(result_return, StatusModifyResultReturn.class);
											
											if(status_modify_result_domaim.getResult().getSuccess().size()>0) {
												List<String> success_list = new ArrayList<String>();
												for (int j = 0; j < status_modify_result_domaim.getResult().getSuccess().size(); j++) {
													success_list.add(status_modify_result_domaim.getResult().getSuccess().get(j));
												}
												status_modify_succes_id_list = DuplicateUtil.findDuplicates(order_info_get_ids_moim_send, success_list);
												logger.info("postStatusModifyV2 duplicate "
														+ "status_modify_succes_id_list:"+gson.toJson(status_modify_succes_id_list)+"//"+seller_id);
											}
											if(status_modify_result_domaim.getResult().getFailures().size()>0) {
												List<String> failures_list = new ArrayList<String>();
												for (int j = 0; j < status_modify_result_domaim.getResult().getFailures().size(); j++) {
													failures_list.add(status_modify_result_domaim.getResult().getFailures().get(j));
												}
												status_modify_failure_id_list = DuplicateUtil.findDuplicates(order_info_get_ids_moim_send, failures_list);
												logger.info("postStatusModifyV2 duplicate "
														+ "status_modify_failure_id_list:"+gson.toJson(status_modify_failure_id_list)+"//"+seller_id);
											}
											if(status_modify_result_domaim.getResult().getUnavailables().size()>0) {
												List<String> unavailables_list = new ArrayList<String>();
												for (int j = 0; j < status_modify_result_domaim.getResult().getUnavailables().size(); j++) {
													unavailables_list.add(status_modify_result_domaim.getResult().getUnavailables().get(j).getId());
												}
												status_modify_unavailable_id_list = DuplicateUtil.findDuplicates(order_info_get_ids_moim_send, unavailables_list);
												logger.info("postStatusModifyV2 duplicate "
														+ "status_modify_unavailable_id_list:"+gson.toJson(status_modify_unavailable_id_list)+"//"+seller_id);
											}
										} catch (Exception e) {
											logger.info("postStatusModifyV2 result_domaim json -> e :"+e+"//"+seller_id);
											error_code = 117;
											error_val = "result_domaim json e:"+e;
										}
									}
								}
							}
							
							if(error_code ==0) {
								if(status_modify_succes_id_list.size()>0) {
									
									deliveries_body_single = "{\"deliveries\": {\""+send_purchaseId+"\":{";
									deliveries_body_single = deliveries_body_single + "\"purchaseItemIds\": [ ";
									
									for (int jj = 0; jj < status_modify_succes_id_list.size(); jj++) {
										if(jj > 0) {
											deliveries_body_single = deliveries_body_single + ", ";
										}
										deliveries_body_single = deliveries_body_single + "\""+status_modify_succes_id_list.get(jj)+"\"";
									}
									deliveries_body_single = deliveries_body_single + " ]";
									if(EmptyUtils.isEmpty(grouprequestModifyBody.get(i).getCourier_id())==false) {
										deliveries_body_single = deliveries_body_single + " ,\"deliveryContractId\":\""+grouprequestModifyBody.get(i).getCourier_id()+"\"";
									}
									deliveries_body_single = deliveries_body_single + "}}}";	
									if(status_modify_succes_id_list.size()==0) {
										deliveries_body_single = "";
									}
									logger.info("postStatusModifyV2 deliveries_body_single:"+deliveries_body_single+"//"+seller_id);
									
									delivery_reception_moimapi_result = moimApiService.OrderDeliveryReception(connect_type, seller_id, deliveries_body_single);

									logger.info("postStatusModifyV2 ==> 배송 접수 결과 :"
											+ ""+delivery_reception_moimapi_result.getError_code()+""
													+ "//"+delivery_reception_moimapi_result.getError_val()+"//"+seller_id);
									logger.info("postStatusModifyV2 result_val "
											+ "OrderStatusModifydeliveryReception:"+delivery_reception_moimapi_result.getResult_return()+"//"+seller_id);
									
									delivery_reception_log = delivery_reception_log + "{\"delivery_"+i+"_send body\":";
									delivery_reception_log = delivery_reception_log + "" +deliveries_body_single;
									delivery_reception_log = delivery_reception_log + ", \"delivery_result_val\":"+delivery_reception_moimapi_result.getResult_return();
									delivery_reception_log = delivery_reception_log + "}";
									
									logger.info("postStatusModifyV2 delivery_reception_log: "+delivery_reception_log+"//"+seller_id);		
									
									if(delivery_reception_moimapi_result.getError_code()>0) {
										error_code = delivery_reception_moimapi_result.getError_code();	
										error_val = delivery_reception_moimapi_result.getError_val();
									}else {
										if(EmptyUtils.isEmpty(delivery_reception_moimapi_result.getResult_return())==false) {
											delivery_reception_result_domaim = gson.fromJson(delivery_reception_moimapi_result.getResult_return(), StatusModifyResultReturnDelivery.class);
										}
									}
								}
							}
							if(error_code ==0) {
								if(status_modify_succes_id_list.size()>0) {
									List<String> delivery_success_data_id = new ArrayList<String>();
									List<String> delivery_failures_data_id = new ArrayList<String>();
									List<String> delivery_unavailables_data_id = new ArrayList<String>();
									if(EmptyUtils.isEmpty(delivery_reception_result_domaim.getResult().getV2())==false){
										for (int jj = 0; jj < delivery_reception_result_domaim.getResult().getV2().getSuccess().size(); jj++) {
											for (int jjj = 0; jjj < delivery_reception_result_domaim.getResult().getV2().getSuccess().get(jj).getData().getItems().size(); jjj++) {
												String data_id = delivery_reception_result_domaim.getResult().getV2().getSuccess().get(jj).getData().getItems().get(jjj);
												delivery_success_data_id.add(data_id);
											}
										}
										for (int jj = 0; jj < delivery_reception_result_domaim.getResult().getV2().getFailures().size(); jj++) {
											for (int jjj = 0; jjj < delivery_reception_result_domaim.getResult().getV2().getFailures().get(jj).getData().getItems().size(); jjj++) {
												String data_id = delivery_reception_result_domaim.getResult().getV2().getFailures().get(jj).getData().getItems().get(jjj);
												delivery_failures_data_id.add(data_id);
											}
										}
										for (int jj = 0; jj < delivery_reception_result_domaim.getResult().getV2().getUnavailables().size(); jj++) {
											for (int jjj = 0; jjj < delivery_reception_result_domaim.getResult().getV2().getUnavailables().get(jj).getData().getItems().size(); jjj++) {
												String data_id = delivery_reception_result_domaim.getResult().getV2().getUnavailables().get(jj).getData().getItems().get(jjj);
												delivery_unavailables_data_id.add(data_id);
											}
										}
									}
									
									logger.info("postStatusModifyV2 result status_modify_succes_id_list:"+gson.toJson(status_modify_succes_id_list)+"//"+seller_id);
									String delivery_reason = "empty";
									for (int jj = 0; jj < delivery_success_data_id.size(); jj++) {
										logger.info("postStatusModifyV2 "+delivery_success_data_id.get(jj)+":delivery_success_data_id.get(jj)"+"//"+seller_id);
										delivery_reason = "succes";
									}
									for (int jj = 0; jj < delivery_failures_data_id.size(); jj++) {
										logger.info("postStatusModifyV2 "+delivery_failures_data_id.get(jj)+":delivery_failures_data_id.get(jj)"+"//"+seller_id);
										delivery_reason = "failures";
									}
									for (int jj = 0; jj < delivery_unavailables_data_id.size(); jj++) {
										logger.info("postStatusModifyV2 "+delivery_unavailables_data_id.get(jj)+":delivery_unavailables_data_id.get(jj)"+"//"+seller_id);
										delivery_reason = "unavailables";
									}
									
									logger.info("postStatusModifyV2 delivery_reason:"+delivery_reason+"//"+seller_id);
									
									List<OrderDomain.ReturnDatav1> order_info_result_list = new ArrayList<OrderDomain.ReturnDatav1>();
									order_info_result_list = OrderListMakeListV2(connect_type
											, status_modify_succes_id_list, seller_id, 0, delivery_reason, 3);
									
									success_result_list.addAll(order_info_result_list);
								}
								if(status_modify_failure_id_list.size()>0) {
									logger.info("postStatusModifyV2 status_modify_failure_id_list:"+gson.toJson(status_modify_failure_id_list)+"//"+seller_id);
									
									List<OrderDomain.ReturnDatav1> order_info_result_list = new ArrayList<OrderDomain.ReturnDatav1>();
									order_info_result_list = OrderListMakeListV2(connect_type
											, status_modify_failure_id_list, seller_id, 0, "", 3);
									
									failures_result_list.addAll(order_info_result_list);
								}
								if(status_modify_unavailable_id_list.size()>0) {
									
									logger.info("postStatusModifyV2 status_modify_unavailable_id_list:"+gson.toJson(status_modify_unavailable_id_list)+"//"+seller_id);
									List<OrderDomain.ReturnDatav1> order_info_result_list = new ArrayList<OrderDomain.ReturnDatav1>();
									order_info_result_list = OrderListMakeListV2(connect_type
											, status_modify_unavailable_id_list, seller_id, 0, "", 3);
									
									unavailables_result_list.addAll(order_info_result_list);
								}
							}
						}
					}
					
					if(success_result_list.size()>0) {
						modify_result.setSuccess(success_result_list);
						if(error_code == 0) {
							String alarm_send = "";
							alarm_send = AlarmTalkSeller(success_result_list, connect_type, sellerid_info, seller_token);
							if(EmptyUtils.isEmpty(alarm_send)==false) {
								modify_result.setAlarm_send(alarm_send);
							}
						}
					}else {
						modify_result.setSuccess(success_result_list); 
					}
					
					modify_result.setFailures(failures_result_list);	
					modify_result.setUnavailables(unavailables_result_list);	
					/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					modify_result.setAll_process_yn(""); // 대용량 처리가 아니라는 뜻. 개별 선택이거나 30개 이하 처리는 결과가 있음.
					/////////////////////////////////////////////////////////////////////////////////////////////////////////////////
					
					return_val.setResult_return(modify_result);
					
					if(error_code==0) {
						
						status_modify_log = status_modify_log + "]";
						logger.info("postStatusModifyV2 status_modify_log :"+status_modify_log+"//"+seller_id);
						delivery_reception_log = delivery_reception_log + "]";
						logger.info("postStatusModifyV2 delivery_reception_log :"+delivery_reception_log+"//"+seller_id);
						
						//======================================================
						// 가능으로 변경 로그 저장 
						String data_val = "";
						String result_failures = "";
						String result_success = "";
						String result_unavailables = "";
						data_val = gson.toJson(grouprequestModifyBody);
						result_failures = gson.toJson(modify_result.getFailures());
						result_success = 	gson.toJson(modify_result.getSuccess());
						result_unavailables = gson.toJson(modify_result.getUnavailables());
						
						Model model_ins = new ExtendedModelMap();
						model_ins.addAttribute("seller_id", seller_id);
						model_ins.addAttribute("modify_key", requestModifyBody.getModify_key());
						model_ins.addAttribute("modify_status", requestModifyBody.getModify_status());
						model_ins.addAttribute("data_val", data_val);
						model_ins.addAttribute("status_modify", status_modify_log);
						model_ins.addAttribute("delivery_reception", delivery_reception_log);
						model_ins.addAttribute("result_failures", result_failures);
						model_ins.addAttribute("result_success", result_success);
						model_ins.addAttribute("result_unavailables", result_unavailables);
						orderService.insertPluginStatusPorcessLog(model_ins);
						//======================================================			
					}
					
				}else if(requestModifyBody.getModify_status()==1	|| requestModifyBody.getModify_status()==11 ) {
					if(requestModifyBody.getModify_status()==1 ) {// 관망중으로 변경 : modify_status  1 : 관망중, 11 : 관망 해제
						DefaultDomain.ErrorCheck error_status_observ = new DefaultDomain.ErrorCheck();
						error_status_observ = Observ(requestModifyBody, connect_type, seller_id);
						if(error_status_observ.getError_code()>0) {
							error_code = error_status_observ.getError_code();
							error_val = error_status_observ.getError_val();
						}else {
							String data_val = "";
							data_val = gson.toJson(requestModifyBody.getData());
							Model model_ins = new ExtendedModelMap();
							model_ins.addAttribute("seller_id", seller_id);
							model_ins.addAttribute("modify_key", requestModifyBody.getModify_key());
							model_ins.addAttribute("modify_status", requestModifyBody.getModify_status());
							model_ins.addAttribute("data_val", data_val);
							model_ins.addAttribute("status_modify", null);
							model_ins.addAttribute("delivery_reception", null);
							model_ins.addAttribute("result_failures", null);
							model_ins.addAttribute("result_success", null);
							model_ins.addAttribute("result_unavailables", null);
							orderService.insertPluginStatusPorcessLog(model_ins);
						}
					}else if(requestModifyBody.getModify_status()==11 ) {
						DefaultDomain.ErrorCheck error_status_observ = new DefaultDomain.ErrorCheck();
						error_status_observ = Observ(requestModifyBody, connect_type, seller_id);
						if(error_status_observ.getError_code()>0) {
							error_code = error_status_observ.getError_code();
							error_val = error_status_observ.getError_val();
						}else {
							String data_val = "";
							data_val = gson.toJson(requestModifyBody.getData());
							Model model_ins = new ExtendedModelMap();
							model_ins.addAttribute("seller_id", seller_id);
							model_ins.addAttribute("modify_key", requestModifyBody.getModify_key());
							model_ins.addAttribute("modify_status", requestModifyBody.getModify_status());
							model_ins.addAttribute("data_val", data_val);
							model_ins.addAttribute("status_modify", null);
							model_ins.addAttribute("delivery_reception", null);
							model_ins.addAttribute("result_failures", null);
							model_ins.addAttribute("result_success", null);
							model_ins.addAttribute("result_unavailables", null);
							orderService.insertPluginStatusPorcessLog(model_ins);
						}
					}
				}
			}else if(requestModifyBody.getModify_key()==2) {
				if(requestModifyBody.getModify_status()==1 ) {
					DefaultDomain.ErrorCheck error_status_observ = new DefaultDomain.ErrorCheck();
					error_status_observ = Observ(requestModifyBody, connect_type, seller_id);
					if(error_status_observ.getError_code()>0) {
						error_code = error_status_observ.getError_code();
						error_val = error_status_observ.getError_val();
					}else {
						String data_val = "";
						data_val = gson.toJson(requestModifyBody.getData());
						Model model_ins = new ExtendedModelMap();
						model_ins.addAttribute("seller_id", seller_id);
						model_ins.addAttribute("modify_key", requestModifyBody.getModify_key());
						model_ins.addAttribute("modify_status", requestModifyBody.getModify_status());
						model_ins.addAttribute("data_val", data_val);
						model_ins.addAttribute("status_modify", null);
						model_ins.addAttribute("delivery_reception", null);
						model_ins.addAttribute("result_failures", null);
						model_ins.addAttribute("result_success", null);
						model_ins.addAttribute("result_unavailables", null);
						orderService.insertPluginStatusPorcessLog(model_ins);
					}
				}else if(requestModifyBody.getModify_status()==11 ) {
					DefaultDomain.ErrorCheck error_status_observ = new DefaultDomain.ErrorCheck();
					error_status_observ = Observ(requestModifyBody, connect_type, seller_id);
					if(error_status_observ.getError_code()>0) {
						error_code = error_status_observ.getError_code();
						error_val = error_status_observ.getError_val();
					}
					
				}else {
					error_code = 120;
					error_val = "getModify_status()  error ";
				}
			}else if(requestModifyBody.getModify_key()==3) {
				DefaultDomain.ErrorCheck error_status_observ = new DefaultDomain.ErrorCheck();
				error_status_observ = Observ(requestModifyBody, connect_type, seller_id);
				if(error_status_observ.getError_code()>0) {
					error_code = error_status_observ.getError_code();
					error_val = error_status_observ.getError_val();
				}else {
					String data_val = "";
					data_val = gson.toJson(requestModifyBody.getData());
					Model model_ins = new ExtendedModelMap();
					model_ins.addAttribute("seller_id", seller_id);
					model_ins.addAttribute("modify_key", requestModifyBody.getModify_key());
					model_ins.addAttribute("modify_status", requestModifyBody.getModify_status());
					model_ins.addAttribute("data_val", data_val);
					model_ins.addAttribute("status_modify", null);
					model_ins.addAttribute("delivery_reception", null);
					model_ins.addAttribute("result_failures", null);
					model_ins.addAttribute("result_success", null);
					model_ins.addAttribute("result_unavailables", null);
					orderService.insertPluginStatusPorcessLog(model_ins);
				}
			}
		}else {
			error_code = 120;
			error_val = "postStatusModify > requestModifyBody error:"+gson.toJson(requestModifyBody);
		}
		
		return_val.setError_code(error_code);
		return_val.setError_val(error_val);
		return return_val;
		
	}
	

	/**
	 * @desc  
	 */
	public SetDomain.ControllerResultStatusModifyReturn postStatusModify(Model model, String connect_type, String seller_id
			, OrderBodyDomain.ModifyBodyList requestModifyBody, SellerIdInfoDomain sellerid_info, String seller_token) {
		
		SetDomain.ControllerResultStatusModifyReturn return_val = new SetDomain.ControllerResultStatusModifyReturn(); 
		int error_code = 0;
		String error_val = "";
		ReturnModifyResult modify_result = new ReturnModifyResult();
  		
		if(requestModifyBody.getModify_key()>0 && requestModifyBody.getModify_status()>0
				&& requestModifyBody.getData().size()>0 && error_code ==0) {
			
			if(requestModifyBody.getModify_key()==1) {// 새로 들어온 주문
				if(requestModifyBody.getModify_status()==2) {// 답변하기 : 가능 리스트로 보냄.
					
					List<OrderDomain.ReturnData> failures_empty = new ArrayList<OrderDomain.ReturnData>();
					List<OrderDomain.ReturnData> failures_list = new ArrayList<OrderDomain.ReturnData>();
					List<OrderDomain.ReturnData> success_empty = new ArrayList<OrderDomain.ReturnData>();
					List<OrderDomain.ReturnData> success_list = new ArrayList<OrderDomain.ReturnData>();
					List<OrderDomain.ReturnData> unavailables_empty = new ArrayList<OrderDomain.ReturnData>();
					List<OrderDomain.ReturnData> unavailables_list = new ArrayList<OrderDomain.ReturnData>();
					
					if(requestModifyBody.getData().size()>0) {
						for (int i = 0; i < requestModifyBody.getData().size(); i++) {
							logger.info("////////////////////////////////////////////////////////////////////////////////start order_key");
							logger.info("order_key : purchaseId :"+requestModifyBody.getData().get(i).getOrder_key()+"//"+seller_id);
							
							OrderGetDomain.GetData order_moim_get_domain = new OrderGetDomain.GetData();
							List<StatusGroupIds> purchase_ids = new ArrayList<StatusGroupIds>();
							List<StatusGroupIds> purchase_ids_pass = new ArrayList<StatusGroupIds>();
							List<String> purchase_ids_array = new ArrayList<String>();
							String deliveries_body_single = "";
							
							List<String> productid_list = new ArrayList<String>();
							for(int ii = 0; ii < requestModifyBody.getData().get(i).getProduct_key().size(); ii++) {
								productid_list.add(requestModifyBody.getData().get(i).getProduct_key().get(ii));	
							}
							
							if(productid_list.size()>0) {
								
								List<OrderDomain.PluginOrderListDomain> select_plugin_order_single = new ArrayList<OrderDomain.PluginOrderListDomain>();
								List<String> ids_moim_send = new ArrayList<String>();
								Model model_get1 = new ExtendedModelMap();
								model_get1.addAttribute("get_type", 1);
								model_get1.addAttribute("purchaseId", requestModifyBody.getData().get(i).getOrder_key());
								model_get1.addAttribute("productId_list", productid_list);//productId list
								model_get1.addAttribute("seller_id", seller_id);
								select_plugin_order_single = orderService.getPluginOrderSingle(model_get1);
								logger.info("select_plugin_order_single:"+select_plugin_order_single+"//"+seller_id);
								
								if(EmptyUtils.isEmpty(select_plugin_order_single)==false && select_plugin_order_single.size()>0) {
									for (int j = 0; j < select_plugin_order_single.size(); j++) {
										logger.info("data_id :"+select_plugin_order_single.get(j).getData_id()+"//"+seller_id);
										ids_moim_send.add(select_plugin_order_single.get(j).getData_id());
									}
								}else {
									logger.info("존재 하지 않는 주문입니다."+"//"+seller_id);
								}
								
								for (int j = 0; j < ids_moim_send.size(); j++) {
									logger.info("ids_moim_send :"+ids_moim_send.get(j)+"//"+seller_id);
								}
								if(error_code ==0) {
									logger.info(requestModifyBody.getData().get(i).getOrder_key()+":"
											+ "requestModifyBody.getData().get(i).getOrder_key()"+"//"+seller_id);
									String get_order_single = "";
									get_order_single = moimApiService.OrderSingleGet(connect_type, seller_id, ids_moim_send);
									
									logger.info("get_order_single:"+get_order_single+"//"+seller_id);
									order_moim_get_domain = gson.fromJson(get_order_single, order_moim_get_domain.getClass());
									if(EmptyUtils.isEmpty(order_moim_get_domain)==false) {
										if(EmptyUtils.isEmpty(order_moim_get_domain.getData())==false) {
											try {
												logger.info(order_moim_get_domain.getData().get(0).getStatus()+"====>getStatus"+"//"+seller_id);
												
												if(order_moim_get_domain.getData().get(0).getStatus().equals("paid")==true) {
													logger.info("paid:"+"//"+seller_id);
													StatusGroupIds single_group = new StatusGroupIds();
													single_group.setPurchase_id(order_moim_get_domain.getData().get(0).getPurchaseId());
													single_group.setOrder_status(order_moim_get_domain.getData().get(0).getStatus());
													single_group.setProduct_id(order_moim_get_domain.getData().get(0).getProductId());
													List<String> data_id = new ArrayList<String>();
													for (int j0 = 0; j0 < order_moim_get_domain.getData().size(); j0++) {
														data_id.add(order_moim_get_domain.getData().get(j0).getId());
														logger.info(order_moim_get_domain.getData().get(j0).getId()+"//"+seller_id);													
													}
													single_group.setData_id(data_id);
													
													purchase_ids.add(single_group);				
													
												}if(order_moim_get_domain.getData().get(0).getStatus().equals("preparingForDelivery")==true) {
													logger.info("preparingForDelivery:"+"//"+seller_id);
													StatusGroupIds single_group = new StatusGroupIds();
													single_group.setPurchase_id(order_moim_get_domain.getData().get(0).getPurchaseId());
													single_group.setOrder_status(order_moim_get_domain.getData().get(0).getStatus());
													single_group.setProduct_id(order_moim_get_domain.getData().get(0).getProductId());
													List<String> data_id = new ArrayList<String>();
													for (int j0 = 0; j0 < order_moim_get_domain.getData().size(); j0++) {
														data_id.add(order_moim_get_domain.getData().get(j0).getId());
														logger.info(order_moim_get_domain.getData().get(j0).getId()+"//"+seller_id);													
													}
													single_group.setData_id(data_id);
													purchase_ids_pass.add(single_group);		
												}else if(order_moim_get_domain.getData().get(0).getStatus().equals("paid")==false 
														&& order_moim_get_domain.getData().get(0).getStatus().equals("preparingForDelivery")==false){
													logger.info("etc:"+"//"+seller_id);
												}
											} catch (Exception e) {
												error_code = 116;
												error_val = "moimApiService.OrderSingleGet moim api 오류 e:"+e;
											}
										}
									}
								}
							
								logger.info("error_code:"+error_code);
								logger.info("purchase_ids_pass.size():"+purchase_ids_pass.size());
								
								if(error_code ==0) {
									for (int j1 = 0; j1 < purchase_ids.size(); j1++) {
										if(purchase_ids.get(j1).getOrder_status().equals("paid")){
											purchase_ids_array.add(purchase_ids.get(j1).getPurchase_id());
										}
									}
									SetDomain.StatusModifyReturn  result_val = new SetDomain.StatusModifyReturn();
									if(purchase_ids_array.size()>0) {
										result_val = moimApiService.OrderStatusModifyPreparingFroDelivery(connect_type, seller_id, purchase_ids_array);
									}
									
									logger.info("purchase_ids_pass.size():"+purchase_ids_pass.size());
									StatusModifyResultReturn new_result = new StatusModifyResultReturn(); 
									ModifyResult result = new ModifyResult();
									if(purchase_ids_pass.size()>0) {
										for (int j2 = 0; j2 < purchase_ids_pass.size(); j2++) {
											List<String> failures = new ArrayList<String>();
											List<String> success = new ArrayList<String>();
											List<UnavailablesStatusModify> unavailables = new ArrayList<UnavailablesStatusModify>();
											logger.info(purchase_ids_pass.get(j2).getPurchase_id());
											success = purchase_ids_pass.get(j2).getData_id();
											result.setFailures(failures);
											result.setSuccess(success);
											result.setUnavailables(unavailables);
											
										}
										new_result.setResult(result);

									}
									
									if(EmptyUtils.isEmpty(result_val.getResult_return())==false) {
										
										logger.info("moimApiService.OrderStatusModifyPreparingFroDelivery -> ");
										logger.info("result_val.getResult_return():"+result_val.getResult_return());
										StatusModifyResultReturn result_domaim = new StatusModifyResultReturn();
										
										try {
											result_domaim = gson.fromJson(result_val.getResult_return(), StatusModifyResultReturn.class);
										} catch (Exception e) {
											logger.info("result_val.getResult_return() fromJson ->");
											logger.info("list_data error e :"+e);
											error_code = 117;
											error_val = "result_val.getResult_return() fromJson list_data e1:"+e;
										}
										
										try {
											if(EmptyUtils.isEmpty(new_result)==false) {
												if(EmptyUtils.isEmpty(new_result.getResult())==false) {
													result_domaim.getResult().setSuccess(new_result.getResult().getSuccess());
												}
											}
											try {
												if(result_domaim.getResult().getFailures().size()>0) {
													List<String> data_id_list = new ArrayList<String>();
													for (int j = 0; j < result_domaim.getResult().getFailures().size(); j++) {
														logger.info("getFailures ==> j:"+j+"//"+result_domaim.getResult().getFailures().get(j)); 
														data_id_list.add(result_domaim.getResult().getFailures().get(j));
													}
													
													List<OrderDomain.ReturnData> list_data = new ArrayList<OrderDomain.ReturnData>();
													list_data = OrderListMakeList(data_id_list, seller_id, 0, "가능으로 변경에 실패했습니다. 관리자에게 문의하시기 바랍니다.", 1);
													
													logger.info("getFailures list_data:"+gson.toJson(list_data));
													
													if(list_data.size()>0) {
														for (int j = 0; j < list_data.size(); j++) {
															failures_list.add(list_data.get(j));
														}
													}else {
														error_code = 117;
														error_val = "getFailures OrderListMakeList list_data empty";
													}
												}
											} catch (Exception e1) {
												logger.info("getFailures OrderListMakeList ->");
												logger.info("list_data error e :"+e1);
												error_code = 117;
												error_val = "getFailures OrderListMakeList list_data e1:"+e1;
											}
											try {
												if(result_domaim.getResult().getSuccess().size()>0) {
													
													String delivery_reception = ""; //failures, success, unavailables
													
													List<String> data_id_list = new ArrayList<String>();
													for (int j = 0; j < result_domaim.getResult().getSuccess().size(); j++) {
														logger.info("getSuccess ==> j:"+j+"//"+result_domaim.getResult().getSuccess().get(j));// CI:.. data_id 
														data_id_list.add(result_domaim.getResult().getSuccess().get(j));
													}
													
													if(order_moim_get_domain.getData().get(0).getStatus().equals("paid")==true
															|| order_moim_get_domain.getData().get(0).getStatus().equals("preparingForDelivery")==true) { 
														deliveries_body_single = "{\"deliveries\": {\""+order_moim_get_domain.getData().get(0).getPurchaseId()+"\" : {";
														deliveries_body_single = deliveries_body_single + "\"purchaseItemIds\": [";
														String last_id = "";
														for (int jj = 0; jj < result_domaim.getResult().getSuccess().size(); jj++) {
															for (int j = 0; j < order_moim_get_domain.getData().size(); j++) {
																if(result_domaim.getResult().getSuccess().get(jj).equals(order_moim_get_domain.getData().get(j).getId())==true) {
																	if(last_id.equals("")==true) {
																		deliveries_body_single = deliveries_body_single + ",";
																	}
																	deliveries_body_single = deliveries_body_single + "\""+order_moim_get_domain.getData().get(j).getId()+"\"";
																	last_id = order_moim_get_domain.getData().get(j).getId();
																}
															}	
														}
														deliveries_body_single = deliveries_body_single + "]";
														if(EmptyUtils.isEmpty(requestModifyBody.getData().get(i).getCourier_id())==false) {
															deliveries_body_single = deliveries_body_single + " ,\"deliveryContractId\" :  \""+requestModifyBody.getData().get(i).getCourier_id()+"\"";
														}
														deliveries_body_single = deliveries_body_single + "}}}";	
														if(EmptyUtils.isEmpty(last_id)==true) {
															deliveries_body_single = "";
														}
														logger.info("deliveries_body_single:"+deliveries_body_single);
			if(error_code ==0 && EmptyUtils.isEmpty(deliveries_body_single)==false){
				JSONParser parser = new JSONParser();
				try {
					Object obj = parser.parse(deliveries_body_single);
					JSONObject jsonObj = (JSONObject) obj;
					logger.info("deliveries_body_single->send body:"+gson.toJson(jsonObj));

				} catch (ParseException e) {
					error_code = 118;
					error_val = "deliveries_body_single->JSONParser error : "+e;
				}
				SetDomain.StatusModifyReturn  delivery_reception_result_val = new SetDomain.StatusModifyReturn();
				delivery_reception_result_val = moimApiService.OrderDeliveryReception(connect_type, seller_id, deliveries_body_single);
				logger.info("result_val OrderStatusModifydeliveryReception:"+delivery_reception_result_val.getResult_return());
				
				if(delivery_reception_result_val.getError_code()>0) {
					error_code = result_val.getError_code();
					error_val = result_val.getError_val();
				}else {
					if(EmptyUtils.isEmpty(delivery_reception_result_val.getResult_return())==false) {
						StatusModifyResultReturnDelivery delivery_reception_result_domaim = new StatusModifyResultReturnDelivery();
						delivery_reception_result_domaim = gson.fromJson(delivery_reception_result_val.getResult_return(), StatusModifyResultReturnDelivery.class);
						if(EmptyUtils.isEmpty(delivery_reception_result_domaim.getResult())==false) {
							
							if(delivery_reception_result_domaim.getResult().getV2().getFailures().size()>0){
								for (int j = 0; j < delivery_reception_result_domaim.getResult().getV2().getFailures().size(); j++) {
									try {
										for (int jj = 0; jj < delivery_reception_result_domaim.getResult().getV2().getFailures().get(j).getData().getItems().size(); jj++) {
											logger.info("getFailures ==> j:"+j+"//"+delivery_reception_result_domaim.getResult().getV2().getFailures().get(j).getData().getItems().get(jj));	
										}
									} catch (Exception e) {
										logger.info("getFailures ==> j: e:"+e);					
									}
									
								}
								
								delivery_reception = "failures";
								
							}else if(delivery_reception_result_domaim.getResult().getV2().getSuccess().size()>0) {
								for (int j = 0; j < delivery_reception_result_domaim.getResult().getV2().getSuccess().size(); j++) {
									try {
										for (int jj = 0; jj < delivery_reception_result_domaim.getResult().getV2().getSuccess().get(j).getData().getItems().size(); jj++) {
											logger.info("getSuccess ==> j:"+j+"//"+delivery_reception_result_domaim.getResult().getV2().getSuccess().get(j).getData().getItems().get(jj));	
										}
									} catch (Exception e) {
										logger.info("getSuccess ==> j: e:"+e);					
									}
								}
								
								delivery_reception = "success";
								
							}else if(delivery_reception_result_domaim.getResult().getV2().getUnavailables().size()>0) {
								for (int j = 0; j < delivery_reception_result_domaim.getResult().getV2().getUnavailables().size(); j++) {
									try {
										for (int jj = 0; jj < delivery_reception_result_domaim.getResult().getV2().getUnavailables().get(j).getData().getItems().size(); jj++) {
											logger.info("getUnavailables ==> j:"+j+"//"+delivery_reception_result_domaim.getResult().getV2().getUnavailables().get(j).getData().getItems().get(jj));	
										}
									} catch (Exception e) {
										logger.info("getUnavailables ==> j: e:"+e);					
									}
								}
								
								delivery_reception = "unavailables";
								
							}else {
								delivery_reception = "error";
							}
						}
					}
				}
			}
													} 
													List<OrderDomain.ReturnData> list_data = new ArrayList<OrderDomain.ReturnData>();
													list_data = OrderListMakeList(data_id_list, seller_id, 0, delivery_reception, 3);
													logger.info("getSuccess list_data:"+gson.toJson(list_data));
													if(list_data.size()>0) {
														for (int j = 0; j < list_data.size(); j++) {
															success_list.add(list_data.get(j));
														}
													}else {
														error_code = 117;
														error_val = "getSuccess OrderListMakeList list_data empty";
													}
												}
											} catch (Exception e1) {
												logger.info("getSuccess OrderListMakeList ->");
												logger.info("list_data error e :"+e1);
												error_code = 111;
												error_val = "getSuccess OrderListMakeList list_data e1:"+e1;
											}
											try {
												if(result_domaim.getResult().getUnavailables().size()>0) {
													List<String> data_id_list = new ArrayList<String>();
													for (int j = 0; j < result_domaim.getResult().getUnavailables().size(); j++) {
														logger.info("getUnavailables ==> j:"+j+"//"+result_domaim.getResult().getUnavailables().get(j)); 
														data_id_list.add(result_domaim.getResult().getUnavailables().get(j).getId());
													}
													
													List<OrderDomain.ReturnData> list_data = new ArrayList<OrderDomain.ReturnData>();
													list_data = OrderListMakeList(data_id_list, seller_id, 0, "변경할 수 없는 주문입니다.", 2);
													
													logger.info("getUnavailables list_data:"+gson.toJson(list_data));
													
													if(list_data.size()>0) {
														for (int j = 0; j < list_data.size(); j++) {
															unavailables_list.add(list_data.get(j));
														}
													}else {
														error_code = 117;
														error_val = "getUnavailables OrderListMakeList list_data empty";
													}
												}
											} catch (Exception e1) {
												logger.info("getUnavailables OrderListMakeList ->");
												logger.info("list_data error e :"+e1);
												error_code = 117;
												error_val = "getUnavailables OrderListMakeList list_data e1:"+e1;
											}
										} catch (Exception e) {
											logger.info("OrderStatusModifyPreparingFroDelivery ->");
											logger.info("StatusModifyResultReturn.class error e :"+e);
											error_code = 117;
											error_val = "StatusModifyResultReturn.class error e:"+e;
										}
									}
								}
							}
						}
					}
					
					if(failures_empty.size()>0) {
						modify_result.setFailures(failures_empty);	
					}else {
						modify_result.setFailures(failures_list);	
					}
					if(success_empty.size()>0) {
						modify_result.setSuccess(success_empty);	
					}else {
						if(success_list.size()>0) {
							modify_result.setSuccess(success_list);
							if(error_code ==0) {
								if(success_list.size()>0) {
									String product_name = "";
									String product_key = "";
									String order_key = "";
									for (int j = 0; j < success_list.size(); j++) {
										if(success_list.get(j).getProduct_list().size()>0) {
											// 상품별 발송
											for (int jj = 0; jj < success_list.get(j).getProduct_list().size(); jj++) {
												if(EmptyUtils.isEmpty(success_list.get(j).getProduct_list().get(jj).getProduct_name())==false) {
													if(jj==0) {
														product_name = success_list.get(j).getProduct_list().get(jj).getProduct_name();
														product_key = success_list.get(j).getProduct_list().get(jj).getProduct_key();
														order_key = success_list.get(j).getOrder_key();
													}
												}
											}
										}
									}
									if(EmptyUtils.isEmpty(product_name)==false) {
										
										String seller_app_domain = "";
										if(connect_type.equals("prod")==true) {
											seller_app_domain = "padobox-seller.moimplugin.com/app";
										}else {
											seller_app_domain = "padobox-seller-dev.moimplugin.com/app";
										}	
										String msgid = "";
										msgid = "PS:"+GetNumber.getRandomKey(10);
										logger.info("msgid:"+msgid);
										logger.info("getPhoneNumber:"+sellerid_info.getContactInformation().getPhoneNumber());
										
										String receiver_phone_num = "";
										logger.info("getPhoneNumber:"+sellerid_info.getContactInformation().getPhoneNumber());
										if(EmptyUtils.isEmpty(sellerid_info.getContactInformation())==false) {
											if(EmptyUtils.isEmpty(sellerid_info.getContactInformation().getPhoneNumber())==false) {
												receiver_phone_num = sellerid_info.getContactInformation().getPhoneNumber();
												logger.info("010: "+receiver_phone_num.substring(0, 3));
												String check_val = receiver_phone_num.substring(0, 3);
												
												if(check_val.equals("010")==true) {
													receiver_phone_num = receiver_phone_num.substring(1);
												}else {
													logger.info("phone number 010 error:"+sellerid_info.getContactInformation().getPhoneNumber());	
													receiver_phone_num = "";
												}
											}else {
												logger.info("phone number error:"+sellerid_info.getContactInformation().getPhoneNumber());
												receiver_phone_num = "";
											}
										}else {
											logger.info("sellerid_info phone number error:"+gson.toJson(sellerid_info));
											receiver_phone_num = "";
										}
										
	if(EmptyUtils.isEmpty(receiver_phone_num)==false) {
		String RandomKey = "";
		RandomKey = GetNumber.generateRandomKey(8);
		
	List<DefaultDomain.KakaoAlramTalkPostBody> post_body = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
	DefaultDomain.KakaoAlramTalkPostBody body_single = new DefaultDomain.KakaoAlramTalkPostBody();
	DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
	body_single.setMsgid(msgid);
	body_single.setMessage_type("AI");
	body_single.setProfile_key(defaultConfig.getKakaoProfileKey()); 
	body_single.setTemplate_code("newpadobox_fisher_processing_0");
	body_single.setReceiver_num("+82"+receiver_phone_num);
	body_single.setReserved_time("00000000000000");
	body_single.setMessage("[조업 중]"
	+ "\n\n어종명 : "+product_name+""
	+ "\n\n1~3일내 발송이 가능하다고 회원님께 전달하였습니다."
	+ "\n\n실제 택배발송이 완료되면 버튼을 클릭해서 “조업중”이라고 표시된 주문건을 “발송 완료”로 변경해 주세요.");

	button1.setName("조업 중 목록보기");
	button1.setType("WL"); 
	button1.setUrl_mobile("https://"+seller_app_domain+"/work/processing/"+RandomKey+"?token="+seller_token);
	button1.setUrl_pc("https://"+seller_app_domain+"/work/processing/"+RandomKey+"?token="+seller_token);
	body_single.setButton1(button1);
	post_body.add(0,body_single);

	logger.info("body_single:"+gson.toJson(body_single));

	String post_result = "";
	post_result = moimApiService.AlarmTalkPost(post_body);
	logger.info("post_result:"+post_result);

	if(EmptyUtils.isEmpty(post_result)==false) {
	DefaultDomain.KakaoAlramTalkPostResult[] array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
	List<DefaultDomain.KakaoAlramTalkPostResult> list = Arrays.asList(array);

	logger.info("list.size():"+list.size());

	for(int ii = 0; ii < list.size(); ii++) {
	logger.info("result:"+list.get(ii).getResult());
	Model model_ins_log = new ExtendedModelMap();
	model_ins_log.addAttribute("seller_id", seller_id);
	model_ins_log.addAttribute("send_target", "seller");
	model_ins_log.addAttribute("send_key", "answer");
	model_ins_log.addAttribute("purchaseId", order_key);
	model_ins_log.addAttribute("productId", product_key);
	model_ins_log.addAttribute("msgid", body_single.getMsgid());
	model_ins_log.addAttribute("message_type", body_single.getMessage_type());
	model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
	model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
	model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
	model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
	model_ins_log.addAttribute("message", body_single.getMessage());
	model_ins_log.addAttribute("button1", gson.toJson(button1));
	model_ins_log.addAttribute("post_result", list.get(ii).getResult());
	model_ins_log.addAttribute("post_sendtime", list.get(ii).getSendtime());
	model_ins_log.addAttribute("post_body", gson.toJson(post_body));
	model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
	orderService.insertKakaoAlarmTalkLog(model_ins_log);
	}
	}
	}
	
	
	
		if(defaultConfig.getKakaoPatnerSendYn().equals("on")==true) {
			
			List<AlarmTalkPartnerDomain.Data> getSingleData = new ArrayList<AlarmTalkPartnerDomain.Data>();
			Model model_get_1 = new ExtendedModelMap();
			model_get_1.addAttribute("get_type", null);
			model_get_1.addAttribute("seller_id", seller_id);
			getSingleData = alarmPartnerService.getKakaoAlarmTalkPartnerList(model_get_1);
			if(EmptyUtils.isEmpty(getSingleData)==false){
				if(getSingleData.size()>0){
					
for(int i = 0; i < getSingleData.size(); i++) {
					
	if(EmptyUtils.isEmpty(getSingleData.get(i).getPartner_phone())==false) {
		
		String patner_receiver_phone_num = "";
		logger.info("getPhoneNumber:"+getSingleData.get(i).getPartner_phone());
		patner_receiver_phone_num = getSingleData.get(i).getPartner_phone();
		logger.info("010: "+patner_receiver_phone_num.substring(0, 3));
		String check_val = patner_receiver_phone_num.substring(0, 3);
		
		if(check_val.equals("010")==true) {
			patner_receiver_phone_num = patner_receiver_phone_num.substring(1);
		}else {
			logger.info("phone number 010 error:"+getSingleData.get(i).getPartner_phone());	
			patner_receiver_phone_num = "";
		}
		
		if(EmptyUtils.isEmpty(patner_receiver_phone_num)==false) {
			List<DefaultDomain.KakaoAlramTalkPostBody> post_body = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
			DefaultDomain.KakaoAlramTalkPostBody body_single = new DefaultDomain.KakaoAlramTalkPostBody();
			DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
			body_single.setMsgid(msgid);
			body_single.setMessage_type("AI");
			body_single.setProfile_key(defaultConfig.getKakaoProfileKey()); 
			body_single.setTemplate_code("newpadobox_fisher_processing_0");
			body_single.setReceiver_num("+82"+patner_receiver_phone_num);
			body_single.setReserved_time("00000000000000");
			body_single.setMessage("[조업 중]"
			+ "\n\n어종명 : "+product_name+""
			+ "\n\n1~3일내 발송이 가능하다고 회원님께 전달하였습니다."
			+ "\n\n실제 택배발송이 완료되면 버튼을 클릭해서 “조업중”이라고 표시된 주문건을 “발송 완료”로 변경해 주세요.");
		
			button1.setName("조업 중 목록보기");
			button1.setType("WL"); 
			button1.setUrl_mobile("https://"+seller_app_domain+"/work/processing?token="+seller_token);
			button1.setUrl_pc("https://"+seller_app_domain+"/work/processing?token="+seller_token);
			body_single.setButton1(button1);
			post_body.add(0,body_single);
		
			logger.info("body_single:"+gson.toJson(body_single));
		
			String post_result = "";
			post_result = moimApiService.AlarmTalkPost(post_body);
			logger.info("post_result:"+post_result);
		
			if(EmptyUtils.isEmpty(post_result)==false) {
				DefaultDomain.KakaoAlramTalkPostResult[] array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
				List<DefaultDomain.KakaoAlramTalkPostResult> list = Arrays.asList(array);
			
				logger.info("list.size():"+list.size());
			
				for(int ii = 0; ii < list.size(); ii++) {
					logger.info("result:"+list.get(ii).getResult());
					Model model_ins_log = new ExtendedModelMap();
					model_ins_log.addAttribute("seller_id", seller_id);
					model_ins_log.addAttribute("send_target", "seller:"+getSingleData.get(i).getPartner_id());
					model_ins_log.addAttribute("send_key", "answer");
					model_ins_log.addAttribute("purchaseId", order_key);
					model_ins_log.addAttribute("productId", product_key);
					model_ins_log.addAttribute("msgid", body_single.getMsgid());
					model_ins_log.addAttribute("message_type", body_single.getMessage_type());
					model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
					model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
					model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
					model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
					model_ins_log.addAttribute("message", body_single.getMessage());
					model_ins_log.addAttribute("button1", gson.toJson(button1));
					model_ins_log.addAttribute("post_result", list.get(ii).getResult());
					model_ins_log.addAttribute("post_sendtime", list.get(ii).getSendtime());
					model_ins_log.addAttribute("post_body", gson.toJson(post_body));
					model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
					orderService.insertKakaoAlarmTalkLog(model_ins_log);
				}
			}
		}
	}
}
				}
			}
		}
	
									}
								}
							}
						}else {
							modify_result.setSuccess(success_empty); 
						}
					}
					if(unavailables_empty.size()>0) {
						modify_result.setUnavailables(unavailables_empty);	
					}else {
						modify_result.setUnavailables(unavailables_list);	
					}
					return_val.setResult_return(modify_result);
				}else if(requestModifyBody.getModify_status()==1 || requestModifyBody.getModify_status()==11) {
					if(requestModifyBody.getModify_status()==1 ) {
						for(int i = 0; i < requestModifyBody.getData().size(); i++) {
							
							for(int j = 0; j < requestModifyBody.getData().get(i).getProduct_key().size(); j++) {
								String product_key = requestModifyBody.getData().get(i).getProduct_key().get(j);
								String order_key = requestModifyBody.getData().get(i).getOrder_key();
								List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
								Model model_get_1 = new ExtendedModelMap();
								model_get_1.addAttribute("seller_id", seller_id);
								model_get_1.addAttribute("productId", product_key);
								model_get_1.addAttribute("purchaseId", order_key);
								model_get_1.addAttribute("sort", "asc");
								
								getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
								
								logger.info("getPluginOrderList.size() ->"+getPluginOrderList.size());
								
								String buyer_phone = "";
								String buyer_name = "";
								String order_number = "";
								String purchaseId = "";
								String productId = "";
								
								for(int jj = 0; jj < getPluginOrderList.size(); jj++) {
									logger.info("getPluginOrderList ->"+ getPluginOrderList.get(jj).getBuyer_name());
									if(jj==0) {
										buyer_phone = getPluginOrderList.get(jj).getBuyer_phone();
										buyer_name = getPluginOrderList.get(jj).getBuyer_name();
										order_number = getPluginOrderList.get(jj).getPaymentId();
										purchaseId = getPluginOrderList.get(jj).getPurchaseId();
										productId = getPluginOrderList.get(jj).getProductId();	
									}
								}
								
								logger.info("buyer_phone:"+buyer_phone);
								logger.info("buyer_name:"+buyer_name);
								logger.info("order_number:"+order_number);
								logger.info("purchaseId:"+purchaseId);						
								logger.info("productId:"+productId);
								
								
								int iRow = 0;
								Model model_up = new ExtendedModelMap();
								model_up.addAttribute("list_type", 1);
								model_up.addAttribute("product_id", product_key);
								model_up.addAttribute("purchase_id", order_key);
								model_up.addAttribute("seller_id", seller_id);
								model_up.addAttribute("order_sub_status", 1);
								iRow = orderService.updateOrderStatus(model_up);
								if(iRow==0) {
									error_code = 119;
									error_val = "orderService.updateOrderStatus 1 1 error ";
									
								}else {
									
									logger.info("buyer_name ->"+ buyer_name);
									logger.info("order_number ->"+ order_number);
									logger.info("buyer_phone ->"+ buyer_phone);
									String receiver_phone_num = buyer_phone;
									
									logger.info("010: "+receiver_phone_num.substring(0, 3));
									String check_val = receiver_phone_num.substring(0, 3);
									if(check_val.equals("010")==true){
										receiver_phone_num = receiver_phone_num.substring(1);
									}else {
										receiver_phone_num = "";
									}
									
									String button_url = "";
									if(connect_type.equals("prod")==true) {
										button_url = "padobox.kr";
									}else {
										button_url = "dev-padobox.vingle.network";
									}	

if(EmptyUtils.isEmpty(receiver_phone_num)==false) {
	
	if(EmptyUtils.isEmpty(buyer_name)==true) {
		buyer_name = "구매 고객님";
	}
	
	String msgid = "";
	msgid = "PU:"+GetNumber.getRandomKey(10);
	logger.info("msgid:"+msgid);
	logger.info("receiver_phone_num:"+receiver_phone_num);
	
	List<DefaultDomain.KakaoAlramTalkPostBody> post_body = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
	DefaultDomain.KakaoAlramTalkPostBody body_single = new DefaultDomain.KakaoAlramTalkPostBody();
	DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
	DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button2 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
	
	body_single.setMsgid(msgid);
	body_single.setMessage_type("AI");
	body_single.setProfile_key(defaultConfig.getKakaoProfileKey()); 
	body_single.setTemplate_code("newpadobox_fisher_observ_0"); 
	body_single.setReceiver_num("+82"+receiver_phone_num); 
	body_single.setReserved_time("00000000000000");
	body_single.setMessage("[조업 관망 중]\r\n"
			+ "\r\n"
			+ ""+buyer_name+" 님\r\n"
			+ "현재 어부가 조업 상황을 지켜보는 중입니다.\r\n"
			+ "조업 환경이 나아지면 조업 후 발송하겠습니다.\r\n"
			+ "\r\n"
			+ "[관망 주요 사유]\r\n"
			+ "조업량 부족, 입항 지연, 기상 악화, 어장 사고\r\n"
			+ "\r\n"
			+ "관망 중 메세지는 잡자 마자 보낸다는 파도상자만의 약속입니다.\r\n"
			+ "\r\n"
			+ "주문 번호 : "+order_number+"\r\n"
			+ "\r\n"
			+ "더 궁금하신 점이 있으실 경우\r\n"
			+ "채팅 문의를 통하여 도움을 드리겠습니다.\r\n"
			+ "\r\n"
			+ "조업을 기다리면, 더 신선해진다\r\n"
			+ "파도상자");
	
	button1.setName("주문 내역 확인");
	button1.setType("WL"); 
	button1.setUrl_mobile("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	button1.setUrl_pc("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	
	button2.setName("채팅 문의하기");
	button2.setType("WL"); 
	button2.setUrl_mobile("https://cand-padobox-static.s3.us-east-1.amazonaws.com/hellotalk-redirector/index.html?ticket=eyJhbG[…]0.A6Z6FABMyO-EwzmTSs15h6KPRhjDGWhloDfxWU8kPik");
	
	body_single.setButton1(button1);
	body_single.setButton2(button2);
	post_body.add(0,body_single);
	
	logger.info("body_single:"+gson.toJson(body_single));
	
	String post_result = "";
	post_result = moimApiService.AlarmTalkPost(post_body);
	logger.info("kakao_send_yn:"+defaultConfig.getKakaoSendYn());
	logger.info("post_result:"+post_result);
	
	if(EmptyUtils.isEmpty(post_result)==false) {
		DefaultDomain.KakaoAlramTalkPostResult[] array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
        List<DefaultDomain.KakaoAlramTalkPostResult> list = Arrays.asList(array);
        
        logger.info("list.size():"+list.size());
        
        for(int ii = 0; ii < list.size(); ii++) {
        	logger.info("result:"+list.get(ii).getResult());
        	Model model_ins_log = new ExtendedModelMap();
	        model_ins_log.addAttribute("seller_id", seller_id);
	        model_ins_log.addAttribute("send_target", "user");
	        model_ins_log.addAttribute("send_key", "1_observ");
	        model_ins_log.addAttribute("purchaseId", purchaseId);
	        model_ins_log.addAttribute("productId", productId);
	        model_ins_log.addAttribute("msgid", body_single.getMsgid());
	        model_ins_log.addAttribute("message_type", body_single.getMessage_type());
	        model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
	        model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
	        model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
	        model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
	        model_ins_log.addAttribute("message", body_single.getMessage());
	        model_ins_log.addAttribute("button1", gson.toJson(button1));
	        model_ins_log.addAttribute("button2", gson.toJson(button2));
	        model_ins_log.addAttribute("post_result", list.get(ii).getResult());
	        model_ins_log.addAttribute("post_sendtime", list.get(ii).getSendtime());
	        model_ins_log.addAttribute("post_body", gson.toJson(post_body));
	        model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
	        orderService.insertKakaoAlarmTalkLog(model_ins_log);
        }
	}
}
								}
							}
						}
					}else if(requestModifyBody.getModify_status()==11 ) {
						for(int i = 0; i < requestModifyBody.getData().size(); i++) {
							for(int j = 0; j < requestModifyBody.getData().get(i).getProduct_key().size(); j++) {
								String product_key = requestModifyBody.getData().get(i).getProduct_key().get(j);
								String order_key = requestModifyBody.getData().get(i).getOrder_key();
								List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
								Model model_get_1 = new ExtendedModelMap();
								model_get_1.addAttribute("seller_id", seller_id);
								model_get_1.addAttribute("productId", product_key);
								model_get_1.addAttribute("purchaseId", order_key);
								model_get_1.addAttribute("sort", "asc");
								
								getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
								
								logger.info("getPluginOrderList.size() ->"+getPluginOrderList.size());
								
								String buyer_phone = "";
								String buyer_name = "";
								String order_number = "";
								String purchaseId = "";
								String productId = "";
								
								for(int jj = 0; jj < getPluginOrderList.size(); jj++) {
									logger.info("getPluginOrderList ->"+ getPluginOrderList.get(jj).getBuyer_name());	
									if(jj==0) {
										buyer_phone = getPluginOrderList.get(jj).getBuyer_phone();
										buyer_name = getPluginOrderList.get(jj).getBuyer_name();
										order_number = getPluginOrderList.get(jj).getPaymentId();
										purchaseId = getPluginOrderList.get(jj).getPurchaseId();
										productId = getPluginOrderList.get(jj).getProductId();	
									}
								}
								
								logger.info("buyer_phone:"+buyer_phone);
								logger.info("buyer_name:"+buyer_name);
								logger.info("order_number:"+order_number);
								logger.info("purchaseId:"+purchaseId);						
								logger.info("productId:"+productId);
								
								int iRow = 0;
								Model model_up = new ExtendedModelMap();
								model_up.addAttribute("list_type", 1);
								model_up.addAttribute("product_id", requestModifyBody.getData().get(i).getProduct_key().get(j));
								model_up.addAttribute("purchase_id", requestModifyBody.getData().get(i).getOrder_key());
								model_up.addAttribute("seller_id", seller_id);
								model_up.addAttribute("order_sub_status", 0);
								if(iRow==0) {
									error_code = 119;
									error_val = "orderService.updateOrderStatus 1 0 error ";
								}else {
									logger.info("buyer_name ->"+ buyer_name);
									logger.info("order_number ->"+ order_number);
									logger.info("buyer_phone ->"+ buyer_phone);
									String receiver_phone_num = buyer_phone;
									logger.info("010: "+receiver_phone_num.substring(0, 3));
									String check_val = receiver_phone_num.substring(0, 3);
									if(check_val.equals("010")==true){
										receiver_phone_num = receiver_phone_num.substring(1);
									}else {
										receiver_phone_num = "";
									}
									
									String button_url = "";
									if(connect_type.equals("prod")==true) {
										button_url = "padobox.kr";
									}else {
										button_url = "dev-padobox.vingle.network";
									}	

if(EmptyUtils.isEmpty(receiver_phone_num)==false) {
	
	if(EmptyUtils.isEmpty(buyer_name)==true) {
		buyer_name = "구매 고객님";
	}
	
	String msgid = "";
	msgid = "PU:"+GetNumber.getRandomKey(10);
	logger.info("msgid:"+msgid);
	logger.info("receiver_phone_num:"+receiver_phone_num);
	
	List<DefaultDomain.KakaoAlramTalkPostBody> post_body = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
	DefaultDomain.KakaoAlramTalkPostBody body_single = new DefaultDomain.KakaoAlramTalkPostBody();
	DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
	
	body_single.setMsgid(msgid);
	body_single.setMessage_type("AI"); 
	body_single.setProfile_key(defaultConfig.getKakaoProfileKey());
	body_single.setTemplate_code("newpadobox_fisher_observ_end_0"); 
	body_single.setReceiver_num("+82"+receiver_phone_num);
	body_single.setReserved_time("00000000000000");
	body_single.setMessage("[조업 성공]\r\n"
			+ "\r\n"
			+ "안녕하세요. "+buyer_name+" 님\r\n"
			+ "\r\n"
			+ "용왕님의 허락으로\r\n"
			+ "조업이 성공되어\r\n"
			+ "지금 배송 준비 중 입니다\r\n"
			+ "\r\n"
			+ "주문내역 : "+order_number+"\r\n"
			+ "\r\n"
			+ "조업을 기다리면, 더 신선해진다\r\n"
			+ "파도상자");
	
	button1.setName("주문 내역 확인");
	button1.setType("WL"); 
	button1.setUrl_mobile("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	button1.setUrl_pc("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	
	body_single.setButton1(button1);
	
	post_body.add(0,body_single);
	
	logger.info("body_single:"+gson.toJson(body_single));
	
	String post_result = "";
	post_result = moimApiService.AlarmTalkPost(post_body);
	logger.info("post_result:"+post_result);
	
	if(EmptyUtils.isEmpty(post_result)==false) {
		DefaultDomain.KakaoAlramTalkPostResult[] array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
        List<DefaultDomain.KakaoAlramTalkPostResult> list = Arrays.asList(array);
        
        logger.info("list.size():"+list.size());
        
        for(int ii = 0; ii < list.size(); ii++) {
        	logger.info("result:"+list.get(ii).getResult());
        	Model model_ins_log = new ExtendedModelMap();
	        model_ins_log.addAttribute("seller_id", seller_id);
	        model_ins_log.addAttribute("send_target", "user");
	        model_ins_log.addAttribute("send_key", "1_c_observ");
	        model_ins_log.addAttribute("purchaseId", purchaseId);
	        model_ins_log.addAttribute("productId", productId);
	        model_ins_log.addAttribute("msgid", body_single.getMsgid());
	        model_ins_log.addAttribute("message_type", body_single.getMessage_type());
	        model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
	        model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
	        model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
	        model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
	        model_ins_log.addAttribute("message", body_single.getMessage());
	        model_ins_log.addAttribute("button1", gson.toJson(button1));
	        model_ins_log.addAttribute("post_result", list.get(ii).getResult());
	        model_ins_log.addAttribute("post_sendtime", list.get(ii).getSendtime());
	        model_ins_log.addAttribute("post_body", gson.toJson(post_body));
	        model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
	        orderService.insertKakaoAlarmTalkLog(model_ins_log);
        }	
	}
}
								}
							}
						}
					}
				}
					
			}else if(requestModifyBody.getModify_key()==2) {
				if(requestModifyBody.getModify_status()==1 ) {
					for(int i = 0; i < requestModifyBody.getData().size(); i++) {
						for(int j = 0; j < requestModifyBody.getData().get(i).getProduct_key().size(); j++) {
							String product_key = requestModifyBody.getData().get(i).getProduct_key().get(j);
							String order_key = requestModifyBody.getData().get(i).getOrder_key();
							List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
							Model model_get_1 = new ExtendedModelMap();
							model_get_1.addAttribute("seller_id", seller_id);
							model_get_1.addAttribute("productId", product_key);
							model_get_1.addAttribute("purchaseId", order_key);
							model_get_1.addAttribute("sort", "asc");
							
							getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
							
							logger.info("getPluginOrderList.size() ->"+getPluginOrderList.size());
							
							String buyer_phone = "";
							String buyer_name = "";
							String order_number = "";
							String purchaseId = "";
							String productId = "";
							
							for(int jj = 0; jj < getPluginOrderList.size(); jj++) {
								logger.info("getPluginOrderList ->"+ getPluginOrderList.get(jj).getBuyer_name());	
								if(jj==0) {
									buyer_phone = getPluginOrderList.get(jj).getBuyer_phone();
									buyer_name = getPluginOrderList.get(jj).getBuyer_name();
									order_number = getPluginOrderList.get(jj).getPaymentId();
									purchaseId = getPluginOrderList.get(jj).getPurchaseId();
									productId = getPluginOrderList.get(jj).getProductId();	
								}
							}
							
							logger.info("buyer_phone:"+buyer_phone);
							logger.info("buyer_name:"+buyer_name);
							logger.info("order_number:"+order_number);
							logger.info("purchaseId:"+purchaseId);						
							logger.info("productId:"+productId);
							
							
							int iRow = 0;
							Model model_up = new ExtendedModelMap();
							model_up.addAttribute("list_type", 1);
							model_up.addAttribute("product_id", requestModifyBody.getData().get(i).getProduct_key().get(j));
							model_up.addAttribute("purchase_id", requestModifyBody.getData().get(i).getOrder_key());
							model_up.addAttribute("seller_id", seller_id);
							model_up.addAttribute("order_sub_status", 1);
							iRow = orderService.updateOrderStatus(model_up);
							if(iRow==0) {
								error_code = 119;
								error_val = "Modify_key()==2 orderService.updateOrderStatus 1 1 error ";
								
							}else {
								logger.info("buyer_name ->"+ buyer_name);
								logger.info("order_number ->"+ order_number);
								logger.info("buyer_phone ->"+ buyer_phone);
								
								String receiver_phone_num = buyer_phone;
								
								logger.info("010: "+receiver_phone_num.substring(0, 3));
								String check_val = receiver_phone_num.substring(0, 3);
								if(check_val.equals("010")==true){
									receiver_phone_num = receiver_phone_num.substring(1);
								}else {
									receiver_phone_num = "";
								}
								
								String button_url = "";
								if(connect_type.equals("prod")==true) {
									button_url = "padobox.kr";
								}else {
									button_url = "dev-padobox.vingle.network";
								}	

if(EmptyUtils.isEmpty(receiver_phone_num)==false) {
	
	if(EmptyUtils.isEmpty(buyer_name)==true) {
		buyer_name = "구매 고객님";
	}
	
	String msgid = "";
	msgid = "PU:"+GetNumber.getRandomKey(10);
	logger.info("msgid:"+msgid);
	logger.info("receiver_phone_num:"+receiver_phone_num);
	
	List<DefaultDomain.KakaoAlramTalkPostBody> post_body = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
	DefaultDomain.KakaoAlramTalkPostBody body_single = new DefaultDomain.KakaoAlramTalkPostBody();
	DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
	DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button2 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
	
	body_single.setMsgid(msgid);
	body_single.setMessage_type("AI");
	body_single.setProfile_key(defaultConfig.getKakaoProfileKey());
	body_single.setTemplate_code("newpadobox_fisher_observ_0");
	body_single.setReceiver_num("+82"+receiver_phone_num);
	body_single.setReserved_time("00000000000000");
	body_single.setMessage("[조업 관망 중]\r\n"
			+ "\r\n"
			+ ""+buyer_name+" 님\r\n"
			+ "현재 어부가 조업 상황을 지켜보는 중입니다.\r\n"
			+ "조업 환경이 나아지면 조업 후 발송하겠습니다.\r\n"
			+ "\r\n"
			+ "[관망 주요 사유]\r\n"
			+ "조업량 부족, 입항 지연, 기상 악화, 어장 사고\r\n"
			+ "\r\n"
			+ "관망 중 메세지는 잡자 마자 보낸다는 파도상자만의 약속입니다.\r\n"
			+ "\r\n"
			+ "주문 번호 : "+order_number+"\r\n"
			+ "\r\n"
			+ "더 궁금하신 점이 있으실 경우\r\n"
			+ "채팅 문의를 통하여 도움을 드리겠습니다.\r\n"
			+ "\r\n"
			+ "조업을 기다리면, 더 신선해진다\r\n"
			+ "파도상자");
	
	button1.setName("주문 내역 확인");
	button1.setType("WL"); 
	button1.setUrl_mobile("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	button1.setUrl_pc("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	
	button2.setName("채팅 문의하기");
	button2.setType("WL"); 
	button2.setUrl_mobile("https://cand-padobox-static.s3.us-east-1.amazonaws.com/hellotalk-redirector/index.html?ticket=eyJhbG[…]0.A6Z6FABMyO-EwzmTSs15h6KPRhjDGWhloDfxWU8kPik");
	
	body_single.setButton1(button1);
	body_single.setButton2(button2);
	post_body.add(0,body_single);
	
	logger.info("body_single:"+gson.toJson(body_single));
	
	String post_result = "";
	post_result = moimApiService.AlarmTalkPost(post_body);
	logger.info("post_result:"+post_result);
	
	if(EmptyUtils.isEmpty(post_result)==false) {
		DefaultDomain.KakaoAlramTalkPostResult[] array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
        List<DefaultDomain.KakaoAlramTalkPostResult> list = Arrays.asList(array);
        
        logger.info("list.size():"+list.size());
        
        for(int ii = 0; ii < list.size(); ii++) {
        	logger.info("result:"+list.get(ii).getResult());
        	Model model_ins_log = new ExtendedModelMap();
	        model_ins_log.addAttribute("seller_id", seller_id);
	        model_ins_log.addAttribute("send_target", "user");
	        model_ins_log.addAttribute("send_key", "2_observ");
	        model_ins_log.addAttribute("purchaseId", purchaseId);
	        model_ins_log.addAttribute("productId", productId);
	        model_ins_log.addAttribute("msgid", body_single.getMsgid());
	        model_ins_log.addAttribute("message_type", body_single.getMessage_type());
	        model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
	        model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
	        model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
	        model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
	        model_ins_log.addAttribute("message", body_single.getMessage());
	        model_ins_log.addAttribute("button1", gson.toJson(button1));
	        model_ins_log.addAttribute("button2", gson.toJson(button2));
	        model_ins_log.addAttribute("post_result", list.get(ii).getResult());
	        model_ins_log.addAttribute("post_sendtime", list.get(ii).getSendtime());
	        model_ins_log.addAttribute("post_body", gson.toJson(post_body));
	        model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
	        orderService.insertKakaoAlarmTalkLog(model_ins_log);
        }
	}
}
							}
						}
						
					}
				}else if(requestModifyBody.getModify_status()==11 ) {
					for(int i = 0; i < requestModifyBody.getData().size(); i++) {
						for(int j = 0; j < requestModifyBody.getData().get(i).getProduct_key().size(); j++) {
							String product_key = requestModifyBody.getData().get(i).getProduct_key().get(j);
							String order_key = requestModifyBody.getData().get(i).getOrder_key();
							List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
							Model model_get_1 = new ExtendedModelMap();
							model_get_1.addAttribute("seller_id", seller_id);
							model_get_1.addAttribute("productId", product_key);
							model_get_1.addAttribute("purchaseId", order_key);
							model_get_1.addAttribute("sort", "asc");
							getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
							
							logger.info("getPluginOrderList.size() ->"+getPluginOrderList.size());
							
							String buyer_phone = "";
							String buyer_name = "";
							String order_number = "";
							String purchaseId = "";
							String productId = "";
							
							for(int jj = 0; jj < getPluginOrderList.size(); jj++) {
								logger.info("getPluginOrderList ->"+ getPluginOrderList.get(jj).getBuyer_name());	
								if(jj==0) {
									buyer_phone = getPluginOrderList.get(jj).getBuyer_phone();
									buyer_name = getPluginOrderList.get(jj).getBuyer_name();
									order_number = getPluginOrderList.get(jj).getPaymentId();
									purchaseId = getPluginOrderList.get(jj).getPurchaseId();
									productId = getPluginOrderList.get(jj).getProductId();	
								}
							}
							
							logger.info("buyer_phone:"+buyer_phone);
							logger.info("buyer_name:"+buyer_name);
							logger.info("order_number:"+order_number);
							logger.info("purchaseId:"+purchaseId);						
							logger.info("productId:"+productId);
														
							int iRow = 0;
							Model model_up = new ExtendedModelMap();
							model_up.addAttribute("list_type", 1);
							model_up.addAttribute("product_id", requestModifyBody.getData().get(i).getProduct_key().get(j));
							model_up.addAttribute("purchase_id", requestModifyBody.getData().get(i).getOrder_key());
							model_up.addAttribute("seller_id", seller_id);
							model_up.addAttribute("order_sub_status", 0);
							iRow = orderService.updateOrderStatus(model_up);
							if(iRow==0) {
								error_code = 119;
								error_val = "Modify_key()==2 orderService.updateOrderStatus 1 0 error ";
								
							}else {
								logger.info("buyer_name ->"+ buyer_name);
								logger.info("order_number ->"+ order_number);
								logger.info("buyer_phone ->"+ buyer_phone);
								
								String receiver_phone_num = buyer_phone;
								
								logger.info("010: "+receiver_phone_num.substring(0, 3));
								String check_val = receiver_phone_num.substring(0, 3);
								if(check_val.equals("010")==true){
									receiver_phone_num = receiver_phone_num.substring(1);
								}else {
									receiver_phone_num = "";
								}
								
								String button_url = "";
								if(connect_type.equals("prod")==true) {
									button_url = "padobox.kr";
								}else {
									button_url = "dev-padobox.vingle.network";
								}	

if(EmptyUtils.isEmpty(receiver_phone_num)==false) {
	
	if(EmptyUtils.isEmpty(buyer_name)==true) {
		buyer_name = "구매 고객님";
	}
	
	String msgid = "";
	msgid = "PU:"+GetNumber.getRandomKey(10);
	logger.info("msgid:"+msgid);
	logger.info("receiver_phone_num:"+receiver_phone_num);
	
	List<DefaultDomain.KakaoAlramTalkPostBody> post_body = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
	DefaultDomain.KakaoAlramTalkPostBody body_single = new DefaultDomain.KakaoAlramTalkPostBody();
	DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
	
	body_single.setMsgid(msgid);
	body_single.setMessage_type("AI"); 
	body_single.setProfile_key(defaultConfig.getKakaoProfileKey());
	body_single.setTemplate_code("newpadobox_fisher_observ_end_0");
	body_single.setReceiver_num("+82"+receiver_phone_num); 
	body_single.setReserved_time("00000000000000");
	body_single.setMessage("[조업 성공]\r\n"
			+ "\r\n"
			+ "안녕하세요. "+buyer_name+" 님\r\n"
			+ "\r\n"
			+ "용왕님의 허락으로\r\n"
			+ "조업이 성공되어\r\n"
			+ "지금 배송 준비 중 입니다\r\n"
			+ "\r\n"
			+ "주문내역 : "+order_number+"\r\n"
			+ "\r\n"
			+ "조업을 기다리면, 더 신선해진다\r\n"
			+ "파도상자");
	
	button1.setName("주문 내역 확인");
	button1.setType("WL"); 
	button1.setUrl_mobile("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	button1.setUrl_pc("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	body_single.setButton1(button1);
	post_body.add(0,body_single);
	
	logger.info("body_single:"+gson.toJson(body_single));
	
	String post_result = "";
	post_result = moimApiService.AlarmTalkPost(post_body);
	logger.info("post_result:"+post_result);
	
	if(EmptyUtils.isEmpty(post_result)==false) {
		DefaultDomain.KakaoAlramTalkPostResult[] array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
        List<DefaultDomain.KakaoAlramTalkPostResult> list = Arrays.asList(array);
        
        logger.info("list.size():"+list.size());
        
        for(int ii = 0; ii < list.size(); ii++) {
        	logger.info("result:"+list.get(ii).getResult());
        	Model model_ins_log = new ExtendedModelMap();
	        model_ins_log.addAttribute("seller_id", seller_id);
	        model_ins_log.addAttribute("send_target", "user");
	        model_ins_log.addAttribute("send_key", "2_c_observ");
	        model_ins_log.addAttribute("purchaseId", purchaseId);
	        model_ins_log.addAttribute("productId", productId);
	        model_ins_log.addAttribute("msgid", body_single.getMsgid());
	        model_ins_log.addAttribute("message_type", body_single.getMessage_type());
	        model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
	        model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
	        model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
	        model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
	        model_ins_log.addAttribute("message", body_single.getMessage());
	        model_ins_log.addAttribute("button1", gson.toJson(button1));
//			model_ins_log.addAttribute("button2", gson.toJson(button2));
	        model_ins_log.addAttribute("post_result", list.get(ii).getResult());
	        model_ins_log.addAttribute("post_sendtime", list.get(ii).getSendtime());
	        model_ins_log.addAttribute("post_body", gson.toJson(post_body));
	        model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
	        orderService.insertKakaoAlarmTalkLog(model_ins_log);
        }
	}
	
}
								
							}
						}
					}
				}else {
					error_code = 120;
					error_val = "getModify_status()  error ";
				}
			}else if(requestModifyBody.getModify_key()==3) { 
				if(requestModifyBody.getModify_status()==1 ) {
					for(int i = 0; i < requestModifyBody.getData().size(); i++) {
						for(int j = 0; j < requestModifyBody.getData().get(i).getProduct_key().size(); j++) {
							String product_key = requestModifyBody.getData().get(i).getProduct_key().get(j);
							String order_key = requestModifyBody.getData().get(i).getOrder_key();
							List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
							Model model_get_1 = new ExtendedModelMap();
							model_get_1.addAttribute("seller_id", seller_id);
							model_get_1.addAttribute("productId", product_key);
							model_get_1.addAttribute("purchaseId", order_key);
							model_get_1.addAttribute("sort", "asc");
							getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
							
							logger.info("getPluginOrderList.size() ->"+getPluginOrderList.size());
							
							String buyer_phone = "";
							String buyer_name = "";
							String order_number = "";
							String purchaseId = "";
							String productId = "";
							
							for(int jj = 0; jj < getPluginOrderList.size(); jj++) {
								logger.info("getPluginOrderList ->"+ getPluginOrderList.get(jj).getBuyer_name());
								if(jj==0) {
									buyer_phone = getPluginOrderList.get(jj).getBuyer_phone();
									buyer_name = getPluginOrderList.get(jj).getBuyer_name();
									order_number = getPluginOrderList.get(jj).getPaymentId();
									purchaseId = getPluginOrderList.get(jj).getPurchaseId();
									productId = getPluginOrderList.get(jj).getProductId();	
								}
							}
							
							logger.info("buyer_phone:"+buyer_phone);
							logger.info("buyer_name:"+buyer_name);
							logger.info("order_number:"+order_number);
							logger.info("purchaseId:"+purchaseId);						
							logger.info("productId:"+productId);
							
							int iRow = 0;
							Model model_up = new ExtendedModelMap();
							model_up.addAttribute("list_type", 1);
							model_up.addAttribute("product_id", requestModifyBody.getData().get(i).getProduct_key().get(j));
							model_up.addAttribute("purchase_id", requestModifyBody.getData().get(i).getOrder_key());
							model_up.addAttribute("seller_id", seller_id);
							model_up.addAttribute("order_sub_status", 1);
							iRow = orderService.updateOrderStatus(model_up);
							if(iRow==0) {
								error_code = 119;
								error_val = "Modify_key()==0 orderService.updateOrderStatus 1 1 error ";
								
							}else {
								logger.info("buyer_name ->"+ buyer_name);
								logger.info("order_number ->"+ order_number);
								logger.info("buyer_phone ->"+ buyer_phone);
								String receiver_phone_num = buyer_phone;
								
								logger.info("010: "+receiver_phone_num.substring(0, 3));
								String check_val = receiver_phone_num.substring(0, 3);
								if(check_val.equals("010")==true){
									receiver_phone_num = receiver_phone_num.substring(1);
								}else {
									receiver_phone_num = "";
								}
								
								String button_url = "";
								if(connect_type.equals("prod")==true) {
									button_url = "padobox.kr";
								}else {
									button_url = "dev-padobox.vingle.network";
								}	

if(EmptyUtils.isEmpty(receiver_phone_num)==false) {
	
	if(EmptyUtils.isEmpty(buyer_name)==true) {
		buyer_name = "구매 고객님";
	}
	
	String msgid = "";
	msgid = "PU:"+GetNumber.getRandomKey(10);
	logger.info("msgid:"+msgid);
	logger.info("receiver_phone_num:"+receiver_phone_num);
	
	List<DefaultDomain.KakaoAlramTalkPostBody> post_body = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
	DefaultDomain.KakaoAlramTalkPostBody body_single = new DefaultDomain.KakaoAlramTalkPostBody();
	DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
	DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button2 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
	
	body_single.setMsgid(msgid);
	body_single.setMessage_type("AI"); 
	body_single.setProfile_key(defaultConfig.getKakaoProfileKey());
	body_single.setTemplate_code("newpadobox_fisher_observ_0");
	body_single.setReceiver_num("+82"+receiver_phone_num);
	body_single.setReserved_time("00000000000000");
	body_single.setMessage("[조업 관망 중]\r\n"
			+ "\r\n"
			+ ""+buyer_name+" 님\r\n"
			+ "현재 어부가 조업 상황을 지켜보는 중입니다.\r\n"
			+ "조업 환경이 나아지면 조업 후 발송하겠습니다.\r\n"
			+ "\r\n"
			+ "[관망 주요 사유]\r\n"
			+ "조업량 부족, 입항 지연, 기상 악화, 어장 사고\r\n"
			+ "\r\n"
			+ "관망 중 메세지는 잡자 마자 보낸다는 파도상자만의 약속입니다.\r\n"
			+ "\r\n"
			+ "주문 번호 : "+order_number+"\r\n"
			+ "\r\n"
			+ "더 궁금하신 점이 있으실 경우\r\n"
			+ "채팅 문의를 통하여 도움을 드리겠습니다.\r\n"
			+ "\r\n"
			+ "조업을 기다리면, 더 신선해진다\r\n"
			+ "파도상자");
	
	button1.setName("주문 내역 확인");
	button1.setType("WL"); 
	button1.setUrl_mobile("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	button1.setUrl_pc("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	
	button2.setName("채팅 문의하기");
	button2.setType("WL"); 
	button2.setUrl_mobile("https://cand-padobox-static.s3.us-east-1.amazonaws.com/hellotalk-redirector/index.html?ticket=eyJhbG[…]0.A6Z6FABMyO-EwzmTSs15h6KPRhjDGWhloDfxWU8kPik");
	
	body_single.setButton1(button1);
	body_single.setButton2(button2);
	post_body.add(0,body_single);
	
	logger.info("body_single:"+gson.toJson(body_single));
	
	String post_result = "";
	post_result = moimApiService.AlarmTalkPost(post_body);
	logger.info("post_result:"+post_result);
	
	if(EmptyUtils.isEmpty(post_result)==false) {
		DefaultDomain.KakaoAlramTalkPostResult[] array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
        List<DefaultDomain.KakaoAlramTalkPostResult> list = Arrays.asList(array);
        
        logger.info("list.size():"+list.size());
        
        for(int ii = 0; ii < list.size(); ii++) {
        	logger.info("result:"+list.get(ii).getResult());
        	Model model_ins_log = new ExtendedModelMap();
	        model_ins_log.addAttribute("seller_id", seller_id);
	        model_ins_log.addAttribute("send_target", "user");
	        model_ins_log.addAttribute("send_key", "3_observ");
	        model_ins_log.addAttribute("purchaseId", purchaseId);
	        model_ins_log.addAttribute("productId", productId);
	        model_ins_log.addAttribute("msgid", body_single.getMsgid());
	        model_ins_log.addAttribute("message_type", body_single.getMessage_type());
	        model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
	        model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
	        model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
	        model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
	        model_ins_log.addAttribute("message", body_single.getMessage());
	        model_ins_log.addAttribute("button1", gson.toJson(button1));
	        model_ins_log.addAttribute("button2", gson.toJson(button2));
	        model_ins_log.addAttribute("post_result", list.get(ii).getResult());
	        model_ins_log.addAttribute("post_sendtime", list.get(ii).getSendtime());
	        model_ins_log.addAttribute("post_body", gson.toJson(post_body));
	        model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
	        orderService.insertKakaoAlarmTalkLog(model_ins_log);
        }
	}
}
							}
						}
					}
				}else if(requestModifyBody.getModify_status()==11 ) {
					for(int i = 0; i < requestModifyBody.getData().size(); i++) {
						for(int j = 0; j < requestModifyBody.getData().get(i).getProduct_key().size(); j++) {
							String product_key = requestModifyBody.getData().get(i).getProduct_key().get(j);
							String order_key = requestModifyBody.getData().get(i).getOrder_key();
							// 주문 정보 가져오기
							List<OrderDomain.PluginOrderListDomain> getPluginOrderList = new ArrayList<OrderDomain.PluginOrderListDomain>();
							Model model_get_1 = new ExtendedModelMap();
							model_get_1.addAttribute("seller_id", seller_id);
							model_get_1.addAttribute("productId", product_key);
							model_get_1.addAttribute("purchaseId", order_key);
							model_get_1.addAttribute("sort", "asc");
							
							getPluginOrderList = orderService.getPluginOrderProductList(model_get_1);
							
							logger.info("getPluginOrderList.size() ->"+getPluginOrderList.size());
							
							String buyer_phone = "";
							String buyer_name = "";
							String order_number = "";
							String purchaseId = "";
							String productId = "";
							
							for(int jj = 0; jj < getPluginOrderList.size(); jj++) {
								logger.info("getPluginOrderList ->"+ getPluginOrderList.get(jj).getBuyer_name());	
								if(jj==0) {
									buyer_phone = getPluginOrderList.get(jj).getBuyer_phone();
									buyer_name = getPluginOrderList.get(jj).getBuyer_name();
									order_number = getPluginOrderList.get(jj).getPaymentId();
									purchaseId = getPluginOrderList.get(jj).getPurchaseId();
									productId = getPluginOrderList.get(jj).getProductId();	
								}
							}
							
							logger.info("buyer_phone:"+buyer_phone);
							logger.info("buyer_name:"+buyer_name);
							logger.info("order_number:"+order_number);
							logger.info("purchaseId:"+purchaseId);						
							logger.info("productId:"+productId);
														
							int iRow = 0;
							Model model_up = new ExtendedModelMap();
							model_up.addAttribute("list_type", 1);
							model_up.addAttribute("product_id", requestModifyBody.getData().get(i).getProduct_key().get(j));
							model_up.addAttribute("purchase_id", requestModifyBody.getData().get(i).getOrder_key());
							model_up.addAttribute("seller_id", seller_id);
							model_up.addAttribute("order_sub_status", 0);
							iRow = orderService.updateOrderStatus(model_up);
							if(iRow==0) {
								error_code = 119;
								error_val = "Modify_key()==0 orderService.updateOrderStatus 1 0 error ";
								
							}else {
								logger.info("buyer_name ->"+ buyer_name);
								logger.info("order_number ->"+ order_number);
								logger.info("buyer_phone ->"+ buyer_phone);
								String receiver_phone_num = buyer_phone;
								
								logger.info("010: "+receiver_phone_num.substring(0, 3));
								String check_val = receiver_phone_num.substring(0, 3);
								if(check_val.equals("010")==true){
									receiver_phone_num = receiver_phone_num.substring(1);
								}else {
									receiver_phone_num = "";
								}
								
								String button_url = "";
								if(connect_type.equals("prod")==true) {
									button_url = "padobox.kr";
								}else {
									button_url = "dev-padobox.vingle.network";
								}	

if(EmptyUtils.isEmpty(receiver_phone_num)==false) {
	
	if(EmptyUtils.isEmpty(buyer_name)==true) {
		buyer_name = "구매 고객님";
	}
	
	String msgid = "";
	msgid = "PU:"+GetNumber.getRandomKey(10);
	logger.info("msgid:"+msgid);
	logger.info("receiver_phone_num:"+receiver_phone_num);
	
	List<DefaultDomain.KakaoAlramTalkPostBody> post_body = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
	DefaultDomain.KakaoAlramTalkPostBody body_single = new DefaultDomain.KakaoAlramTalkPostBody();
	DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
	
	body_single.setMsgid(msgid);
	body_single.setMessage_type("AI"); 
	body_single.setProfile_key(defaultConfig.getKakaoProfileKey());
	body_single.setTemplate_code("newpadobox_fisher_observ_end_0");
	body_single.setReceiver_num("+82"+receiver_phone_num);
	body_single.setReserved_time("00000000000000");
	body_single.setMessage("[조업 성공]\r\n"
			+ "\r\n"
			+ "안녕하세요. "+buyer_name+" 님\r\n"
			+ "\r\n"
			+ "용왕님의 허락으로\r\n"
			+ "조업이 성공되어\r\n"
			+ "지금 배송 준비 중 입니다\r\n"
			+ "\r\n"
			+ "주문내역 : "+order_number+"\r\n"
			+ "\r\n"
			+ "조업을 기다리면, 더 신선해진다\r\n"
			+ "파도상자");
	
	button1.setName("주문 내역 확인");
	button1.setType("WL"); 
	button1.setUrl_mobile("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	button1.setUrl_pc("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
	
	body_single.setButton1(button1);
	
	post_body.add(0,body_single);
	
	logger.info("body_single:"+gson.toJson(body_single));
	
	String post_result = "";
	post_result = moimApiService.AlarmTalkPost(post_body);
	logger.info("post_result:"+post_result);
	
	if(EmptyUtils.isEmpty(post_result)==false) {
		DefaultDomain.KakaoAlramTalkPostResult[] array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
        List<DefaultDomain.KakaoAlramTalkPostResult> list = Arrays.asList(array);
        
        logger.info("list.size():"+list.size());
        
        for(int ii = 0; ii < list.size(); ii++) {
        	logger.info("result:"+list.get(ii).getResult());
        	Model model_ins_log = new ExtendedModelMap();
	        model_ins_log.addAttribute("seller_id", seller_id);
	        model_ins_log.addAttribute("send_target", "user");
	        model_ins_log.addAttribute("send_key", "3_c_observ");
	        model_ins_log.addAttribute("purchaseId", purchaseId);
	        model_ins_log.addAttribute("productId", productId);
	        model_ins_log.addAttribute("msgid", body_single.getMsgid());
	        model_ins_log.addAttribute("message_type", body_single.getMessage_type());
	        model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
	        model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
	        model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
	        model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
	        model_ins_log.addAttribute("message", body_single.getMessage());
	        model_ins_log.addAttribute("button1", gson.toJson(button1));
	        model_ins_log.addAttribute("post_result", list.get(ii).getResult());
	        model_ins_log.addAttribute("post_sendtime", list.get(ii).getSendtime());
	        model_ins_log.addAttribute("post_body", gson.toJson(post_body));
	        model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
	        orderService.insertKakaoAlarmTalkLog(model_ins_log);
        }
	}
}
							}
						}
					}
				}else {
					error_code = 120;
					error_val = "getModify_status()  error ";
				}
			}
			
		}else {
			error_code = 120;
			error_val = "postStatusModify > requestModifyBody error:"+gson.toJson(requestModifyBody);
		}
		
		if(error_code==0) {
			String data_val = "";
			String result_failures = "";
			String result_success = "";
			String result_unavailables = "";
			data_val = gson.toJson(requestModifyBody.getData());
			result_failures = gson.toJson(modify_result.getFailures());
			result_success = 	gson.toJson(modify_result.getSuccess());
			result_unavailables = gson.toJson(modify_result.getUnavailables());
			
			Model model_ins = new ExtendedModelMap();
			model_ins.addAttribute("modify_key", requestModifyBody.getModify_key());
			model_ins.addAttribute("modify_status", requestModifyBody.getModify_status());
			model_ins.addAttribute("data_val", data_val);
			model_ins.addAttribute("result_failures", result_failures);
			model_ins.addAttribute("result_success", result_success);
			model_ins.addAttribute("result_unavailables", result_unavailables);
			orderService.insertPluginStatusPorcessLog(model_ins);
		}
		return_val.setError_code(error_code);
		return_val.setError_val(error_val);
		return return_val;
	}
	

	/**
	 * @desc  
	 */
	public SetDomain.MainMenuCount getMainCntData(Model model, String connect_type, String seller_id) {
		SetDomain.MainMenuCount return_val = new SetDomain.MainMenuCount(); 
  		return_val.setNew_order_cnt(null);
  		return_val.setPossible_order_cnt(null);
  		return_val.setObserv_order_cnt(null);
  		return_val.setDelivery_order_cnt(null);
  		return_val.setCancel_order_cnt(null);  		
  		
  		return_val.setObserv_2_order_cnt(null);
  		return_val.setObserv_new_order_cnt(null);
  		return_val.setObserv_possible_order_cnt(null);
  		
		return return_val;
	}
	

	/**
	 * @desc  
	 */
	public SetDomain.MainMenuCount getMainCntDataV2(Model model, String connect_type, String seller_id) {
		SetDomain.MainMenuCount return_val = new SetDomain.MainMenuCount(); 
		
		SetDomain.MainMenuCount observ_return_val = new SetDomain.MainMenuCount(); 
		return_val = moimApiService.MainCntGet(connect_type, seller_id);
		
		LocalDate now = LocalDate.now();
        LocalDate sevenDaysAgo = now.minusDays(15);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String week_ago = sevenDaysAgo.format(formatter);
		model.addAttribute("seller_id", seller_id);
  		model.addAttribute("view_type", "v2");
  		model.addAttribute("week_ago", week_ago);
  		observ_return_val = orderService.getPluginMainCnt(model);
  		
  		if(observ_return_val.getObserv_new_order_cnt()>0 || observ_return_val.getObserv_possible_order_cnt()>0) {
  			int all_observ_count = 0;
  			all_observ_count = (int)observ_return_val.getObserv_new_order_cnt()+(int)observ_return_val.getObserv_possible_order_cnt();
  			observ_return_val.setObserv_2_order_cnt(all_observ_count);
  		}
  		
  		if(EmptyUtils.isEmpty(observ_return_val)==false) {
  			if(observ_return_val.getObserv_2_order_cnt()>0) {
  	  			return_val.setObserv_order_cnt(observ_return_val.getObserv_2_order_cnt());	
  	  		}else {
  	  			return_val.setObserv_order_cnt(0);
  	  		}
  		}else {
  			return_val.setObserv_order_cnt(0);
  		}
  		
  		List<StatusProcessAsyncLog> getAsyncLog = new ArrayList<StatusProcessAsyncLog>();
  		Model model_call = new ExtendedModelMap();
		model_call.addAttribute("seller_id", seller_id);
		getAsyncLog = asyncService.getStatusProcessAsyncLog(model_call);
		
		if(EmptyUtils.isEmpty(getAsyncLog)==false) {
			if(getAsyncLog.size()>0) {
				// limit 1로 바꿈
				if(getAsyncLog.get(0).getProcess_status().equals("READY")==true) {
					return_val.setAsync_count(1);
				}else {
					return_val.setAsync_count(0);
				}	
			}else {
				return_val.setAsync_count(0);
			}
		}else {
			return_val.setAsync_count(0);
		}
  		
  		return_val.setObserv_2_order_cnt(null);
  		return_val.setObserv_new_order_cnt(null);
  		return_val.setObserv_possible_order_cnt(null);
		
		return return_val;
	}
	

	/**
	 * @desc  
	 */
	public int getNewListCnt(Model model, String connect_type, String seller_id) {
		int cnt_val = 0;		
		cnt_val = moimApiService.NewListCntGet(connect_type, seller_id);
		return cnt_val;
	}
	

	/**
	 * @desc  
	 */
	public int getSellerIdSync(Model model, String connect_type, String seller_id) {
		int sync_yn = 0;
		
		DefaultDomain.CallLogBody call_log_body = new DefaultDomain.CallLogBody();
		Model model_call = new ExtendedModelMap();
		model_call.addAttribute("api_call_type", "order");
		model_call.addAttribute("seller_id", seller_id);
		call_log_body = orderService.getPluginApiCallLog(model_call);
		if(EmptyUtils.isEmpty(call_log_body)==false){
			if(EmptyUtils.isEmpty(call_log_body.getAfter_val())==false){
				sync_yn = 0;
			}else {
				sync_yn = 1;
			}
		}else {
			sync_yn = 1;
		}
		
		return sync_yn;
	}
	

	/**
	 * @desc  
	 */
	public SellerIdDeliveryDomain SellerIdDeliveryContractsV2(Model model, String connect_type
			, String seller_id, String seller_token, SellerIdInfoDomain sellerid_info) {
		SellerIdDeliveryDomain delivery_list = new SellerIdDeliveryDomain();
		try {
			delivery_list = moimApiService.SellerInfoDeliveryListGet(connect_type, seller_id);
			if(EmptyUtils.isEmpty(delivery_list)==false) {
				if(delivery_list.getData().size()>0){
					logger.info("SellerIdDeliveryContractsV2 delivery_list.getData().size():"+delivery_list.getData().size()+"//"+seller_id);		
				}
			}
		} catch (Exception e) {
			logger.info("SellerIdDeliveryContractsV2 insertPluginSellerInfo error : "+e+"//"+seller_id);
		}
		return delivery_list;
	}
	

	/**
	 * @desc  
	 */
	public SellerIdDeliveryDomain SellerIdDeliveryContracts(Model model, String connect_type
			, String seller_id, String seller_token, SellerIdInfoDomain sellerid_info) {
		SellerIdDeliveryDomain delivery_list = new SellerIdDeliveryDomain();
		try {
			delivery_list = moimApiService.SellerInfoDeliveryListGet(connect_type, seller_id);
			if(EmptyUtils.isEmpty(delivery_list)==false) {
				if(delivery_list.getData().size()>0){
					
					for(int i = 0; i < delivery_list.getData().size(); i++) {
						
						logger.info("SellerIdDeliveryContractsV2 delivery_list.getData().size():"+delivery_list.getData().size()+"//"+seller_id);		
						
						List<PluginSellerInfo> select_plugin_seller_info_list = new ArrayList<PluginSellerInfo>();
						Model model_get = new ExtendedModelMap();
						model_get.addAttribute("list_type", 1); 
						model_get.addAttribute("seller_id", seller_id); 
						select_plugin_seller_info_list = orderService.getPluginSellerInfo(model_get);  
						
						if(EmptyUtils.isEmpty(select_plugin_seller_info_list)==false
								&& select_plugin_seller_info_list.size()>0) {
							
							for(int j = 0; j < select_plugin_seller_info_list.size(); j++) {
								
								if(delivery_list.getData().get(i).getId().equals(select_plugin_seller_info_list.get(j).getDelivery_id())==true) {
									PluginSellerInfo up_plugin_seller_info = new PluginSellerInfo();
									up_plugin_seller_info.setList_type(2);// delivery info update
									up_plugin_seller_info.setSeller_id(seller_id);
									up_plugin_seller_info.setSeller_name(sellerid_info.getName());
									up_plugin_seller_info.setDelivery_id(select_plugin_seller_info_list.get(j).getDelivery_id());
									up_plugin_seller_info.setIdx(select_plugin_seller_info_list.get(j).getIdx());
									
									up_plugin_seller_info.setDelivery_name(delivery_list.getData().get(i).getName());
									up_plugin_seller_info.setDelivery_company_code(delivery_list.getData().get(i).getCompanyCode());
									if(EmptyUtils.isEmpty(delivery_list.getData().get(i).getName())==false){
										up_plugin_seller_info.setDelivery_contactInfo_name(delivery_list.getData().get(i).getName());	
									}else {
										up_plugin_seller_info.setDelivery_contactInfo_name("");
									}
									if(EmptyUtils.isEmpty(delivery_list.getData().get(i).getContactInformation().getEmail())==false){
										up_plugin_seller_info.setDelivery_contactInfo_email(delivery_list.getData().get(i).getContactInformation().getEmail());	
									}else {
										up_plugin_seller_info.setDelivery_contactInfo_email("");
									}
									if(EmptyUtils.isEmpty(delivery_list.getData().get(i).getContactInformation().getPhoneNumber())==false){
										up_plugin_seller_info.setDelivery_contactInfo_phone(delivery_list.getData().get(i).getContactInformation().getPhoneNumber());	
									}else {
										up_plugin_seller_info.setDelivery_contactInfo_phone("");
									}
									up_plugin_seller_info.setDelivery_updatedAt(delivery_list.getData().get(i).getUpdatedAt());
									
									orderService.updatePluginSellerInfo(up_plugin_seller_info);
								}else {
									PluginSellerInfo new_plugin_seller_info = new PluginSellerInfo();
									new_plugin_seller_info.setSeller_id(seller_id);
									new_plugin_seller_info.setSeller_name(sellerid_info.getName());
									new_plugin_seller_info.setList_type(1);
									new_plugin_seller_info.setToken_val(seller_token);
									new_plugin_seller_info.setDelivery_num(select_plugin_seller_info_list.size()+1);
									new_plugin_seller_info.setDelivery_id(delivery_list.getData().get(i).getId());
									new_plugin_seller_info.setDelivery_name(delivery_list.getData().get(i).getName());
									new_plugin_seller_info.setDelivery_company_code(delivery_list.getData().get(i).getCompanyCode());
									if(EmptyUtils.isEmpty(delivery_list.getData().get(i).getName())==false){
										new_plugin_seller_info.setDelivery_contactInfo_name(delivery_list.getData().get(i).getName());	
									}else {
										new_plugin_seller_info.setDelivery_contactInfo_name("");
									}
									if(EmptyUtils.isEmpty(delivery_list.getData().get(i).getContactInformation().getEmail())==false){
										new_plugin_seller_info.setDelivery_contactInfo_email(delivery_list.getData().get(i).getContactInformation().getEmail());	
									}else {
										new_plugin_seller_info.setDelivery_contactInfo_email("");
									}
									if(EmptyUtils.isEmpty(delivery_list.getData().get(i).getContactInformation().getPhoneNumber())==false){
										new_plugin_seller_info.setDelivery_contactInfo_phone(delivery_list.getData().get(i).getContactInformation().getPhoneNumber());	
									}else {
										new_plugin_seller_info.setDelivery_contactInfo_phone("");
									}
									new_plugin_seller_info.setDelivery_createdAt(delivery_list.getData().get(i).getCreatedAt());		
									new_plugin_seller_info.setDelivery_updatedAt(delivery_list.getData().get(i).getUpdatedAt());
									orderService.insertPluginSellerInfo(new_plugin_seller_info);
								}
							}
							
						}else { 
							
							PluginSellerInfo new_plugin_seller_info = new PluginSellerInfo();
							new_plugin_seller_info.setSeller_id(seller_id);
							new_plugin_seller_info.setSeller_name(sellerid_info.getName());
							new_plugin_seller_info.setList_type(1);
							new_plugin_seller_info.setToken_val(seller_token);
							new_plugin_seller_info.setDelivery_num(i+1);
							new_plugin_seller_info.setDelivery_id(delivery_list.getData().get(i).getId());
							new_plugin_seller_info.setDelivery_name(delivery_list.getData().get(i).getName());
							new_plugin_seller_info.setDelivery_company_code(delivery_list.getData().get(i).getCompanyCode());
							if(EmptyUtils.isEmpty(delivery_list.getData().get(i).getName())==false){
								new_plugin_seller_info.setDelivery_contactInfo_name(delivery_list.getData().get(i).getName());	
							}else {
								new_plugin_seller_info.setDelivery_contactInfo_name("");
							}
							if(EmptyUtils.isEmpty(delivery_list.getData().get(i).getContactInformation().getEmail())==false){
								new_plugin_seller_info.setDelivery_contactInfo_email(delivery_list.getData().get(i).getContactInformation().getEmail());	
							}else {
								new_plugin_seller_info.setDelivery_contactInfo_email("");
							}
							if(EmptyUtils.isEmpty(delivery_list.getData().get(i).getContactInformation().getPhoneNumber())==false){
								new_plugin_seller_info.setDelivery_contactInfo_phone(delivery_list.getData().get(i).getContactInformation().getPhoneNumber());	
							}else {
								new_plugin_seller_info.setDelivery_contactInfo_phone("");
							}
							new_plugin_seller_info.setDelivery_createdAt(delivery_list.getData().get(i).getCreatedAt());		
							new_plugin_seller_info.setDelivery_updatedAt(delivery_list.getData().get(i).getUpdatedAt());
							orderService.insertPluginSellerInfo(new_plugin_seller_info);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.info("SellerIdDeliveryContractsV2 insertPluginSellerInfo error : "+e+"//"+seller_id);
		}
		return delivery_list;
	}
	

	/**
	 * @desc  
	 */
	public DefaultDomain.ErrorCheck GetMoimSyncData(String connect_type, String seller_id
			, String sync_type) {
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		
		int error_code = 0;
		String error_val = "";
		
		OrderDomain order_list = new OrderDomain();
		OrderGetDomain.GetData order_get_id_domain = new OrderGetDomain.GetData();
		OrderGetDomain.GetData order_get_data_domain = new OrderGetDomain.GetData();
		String check = "";
		String after = "";
		List<String> get_data_id_list = new ArrayList<String>();
		String insert_last_after = "";
		
		DefaultDomain.CallLogBody call_log_body = new DefaultDomain.CallLogBody();
		Model model_call = new ExtendedModelMap();
		model_call.addAttribute("api_call_type", "order");
		model_call.addAttribute("seller_id", seller_id);
		model_call.addAttribute("sync_type", sync_type);
		call_log_body = orderService.getPluginApiCallLog(model_call);
		if(EmptyUtils.isEmpty(call_log_body)==false){
			if(EmptyUtils.isEmpty(call_log_body.getAfter_val())==false){
				after = call_log_body.getAfter_val();
			}
		}
		for(int i = 0; i < 10000; i++) { 
			check = "";
			try {
				check = moimApiService.SyncIdDataGet(connect_type, seller_id, after);
				logger.info("GetMoimSyncData > SyncIdDataGet check:"+check+": i :"+i+"//"+seller_id);
				if(check != "") {
					order_get_id_domain = gson.fromJson(check, order_get_id_domain.getClass());
					if(EmptyUtils.isEmpty(order_get_id_domain.getData())==false 
							&& EmptyUtils.isEmpty(order_get_id_domain.getPaging())==false){
						if(order_get_id_domain.getData().size()>0) {
							for(int j = 0; j < order_get_id_domain.getData().size(); j++) {
								get_data_id_list.add(order_get_id_domain.getData().get(j).getId());
								if(EmptyUtils.isEmpty(order_get_id_domain.getPaging().getAfter())==false) {
									after = order_get_id_domain.getPaging().getAfter();
									insert_last_after = order_get_id_domain.getPaging().getAfter();
								}
							}	
						}
					}else {
						break;
					}
				}else {
					break;
				}
			} catch (Exception e) {
				logger.info("GetMoimSyncData moimApiService.SyncIdDataGet : error : e"+e+"//"+seller_id);
			}
		
		}
		if(get_data_id_list.size()>0) {
			List<String> distinct_list = get_data_id_list.stream().distinct().collect(Collectors.toList());
			List<String> five_ids_list = new ArrayList<String>();
			int last_num = 0;
			for(int j = 0; j < distinct_list.size(); j++) {
				five_ids_list.add(distinct_list.get(j));			
				int num = j+1;
				if(num%5==0) {
					String json_val = "";
					json_val = gson.toJson(five_ids_list);				
					logger.info("GetMoimSyncData five_ids_list json_val:"+json_val+"//"+seller_id);
					
					String get_order_single = "";
					try {
						get_order_single = moimApiService.OrderSingleGet(connect_type, seller_id, five_ids_list);
						logger.info("GetMoimSyncData get_order_single:"+get_order_single+"//"+seller_id);
						order_get_data_domain = gson.fromJson(get_order_single, order_get_data_domain.getClass());
						error_status = RdsSyncProcess(order_get_data_domain);
						if(error_status.getError_code()>0) {
							error_code = error_status.getError_code();
							error_val = error_status.getError_val();	
							break;
						}else {
							
						}
					} catch (Exception e) {
						error_code = 300;
						error_val = "GetMoimSyncData moimApiService.OrderSingleGet error : "+e;
						logger.info("GetMoimSyncData insert_last_after:"+insert_last_after+"//"+seller_id);
						break;
					}

					five_ids_list = new ArrayList<String>();
					last_num = num;
				}else if(distinct_list.size()-last_num < 6) {// 5개 이하 id들
					if(distinct_list.size() == num) {
						String json_val = "";
						json_val = gson.toJson(five_ids_list);
						logger.info("GetMoimSyncData json_val:"+json_val+"//"+seller_id);	
						String get_order_single = "";
						try {
							get_order_single = moimApiService.OrderSingleGet(connect_type, seller_id, five_ids_list);
							logger.info("GetMoimSyncData five_ids_list : get_order_single:"+get_order_single+"//"+seller_id);
							order_get_data_domain = gson.fromJson(get_order_single, order_get_data_domain.getClass());
							error_status = RdsSyncProcess(order_get_data_domain);
							if(error_status.getError_code()>0) {
								error_code = error_status.getError_code();
								error_val = error_status.getError_val();	
								break;
							}else {
								
							}
						} catch (Exception e) {
							error_code = 300;
							error_val = "moimApiService.OrderSingleGet error : "+e;
							logger.info("GetMoimSyncData insert_last_after:"+insert_last_after+"//"+seller_id);
							break;
						}
					}
					last_num = num;
				}
			}
			logger.info("GetMoimSyncData insert_last_after:"+insert_last_after+"//"+seller_id);
			if(error_code==0) {
				if(EmptyUtils.isEmpty(insert_last_after)==false){
					Model model_ins = new ExtendedModelMap();
					model_ins.addAttribute("api_call_type", "order");
					model_ins.addAttribute("seller_id", seller_id);
					model_ins.addAttribute("after_val", insert_last_after);
					model_ins.addAttribute("at_val", null);
					model_ins.addAttribute("sync_type", sync_type);
					orderService.insertPluginApiCallLog(model_ins);	
				}
			}
		}
		order_list.setError_code(error_code);
		order_list.setError_val(error_val);
		
		return error_status;
	}
	

	/**
	 * @desc  
	 */
	public DefaultDomain.ErrorCheck RdsSyncProcess(OrderGetDomain.GetData order_get_domain) {
		
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		if(EmptyUtils.isEmpty(order_get_domain.getData())==false) {
			for(int i = 0; i < order_get_domain.getData().size(); i++) {
				
				logger.info("RdsSyncProcess data_id:"+order_get_domain.getData().get(i).getId());
				
				Model model_ins = new ExtendedModelMap();
				model_ins.addAttribute("data_id", order_get_domain.getData().get(i).getId());
				model_ins.addAttribute("paymentId", order_get_domain.getData().get(i).getPaymentId());
				model_ins.addAttribute("purchaseId", order_get_domain.getData().get(i).getPurchaseId());
				model_ins.addAttribute("userId", order_get_domain.getData().get(i).getUserId());
				model_ins.addAttribute("parentSellerId", order_get_domain.getData().get(i).getParentSellerId());
				model_ins.addAttribute("sellerId", order_get_domain.getData().get(i).getSellerId());
				model_ins.addAttribute("productId", order_get_domain.getData().get(i).getProductId());
				model_ins.addAttribute("settlementId", order_get_domain.getData().get(i).getSettlementId());
				model_ins.addAttribute("productName", order_get_domain.getData().get(i).getProductName());
				model_ins.addAttribute("productImageUrl", order_get_domain.getData().get(i).getImageUrl());
				model_ins.addAttribute("productVariantId", order_get_domain.getData().get(i).getProductVariantId());
				
				String var_val1 = "";
				String[] var_val_array1 = null;
				if(EmptyUtils.isEmpty(order_get_domain.getData().get(i).getProductVariantValue())==false) {
					var_val1 = order_get_domain.getData().get(i).getProductVariantValue().toString();
					var_val1 = var_val1.replaceAll("\\{", "");
					var_val1 = var_val1.replaceAll("\\}", "");
				}
				
				if(EmptyUtils.isEmpty(var_val1)==false) {
					var_val_array1 = var_val1.split("=");
					for(int ii = 0; ii < var_val_array1.length; ii++) {
						if(ii==2){
							if(EmptyUtils.isEmpty(var_val_array1[ii])==false) {
								var_val1 =	var_val_array1[ii];
							}
						}
					}
				}
				model_ins.addAttribute("productVariantValue", var_val1);					
				model_ins.addAttribute("status", order_get_domain.getData().get(i).getStatus());						
				model_ins.addAttribute("createdAt", order_get_domain.getData().get(i).getCreatedAt());
				model_ins.addAttribute("updatedAt", order_get_domain.getData().get(i).getUpdatedAt());
				model_ins.addAttribute("refundedAt", order_get_domain.getData().get(i).getRefundedAt());// 환불
				model_ins.addAttribute("paidAt", order_get_domain.getData().get(i).getPaidAt());
				model_ins.addAttribute("object_json", gson.toJson(order_get_domain.getData().get(i)).toString());
				
				logger.info("RdsSyncProcess json:"+gson.toJson(order_get_domain.getData().get(i)).toString());
				
				try {
					int iRow = 0;
					iRow = orderService.insertPurchaseItemGetLog(model_ins);
					logger.info("RdsSyncProcess insertPurchaseItemGetLog iRow:"+iRow);	
				} catch (Exception e) {
					error_status.setError_code(121);
					error_status.setError_val("insertPurchaseItemGetLog insert error : "+e);
					break;
				}
			
				if(error_status.getError_code() == 0) {
					String paid_date = "";
					if(EmptyUtils.isEmpty(order_get_domain.getData().get(i).getPaidAt())==false) {
						if(order_get_domain.getData().get(i).getPaidAt()>0) {
							paid_date = LocationTimeCal.TimeStamptoDate(order_get_domain.getData().get(i).getPaidAt());
						}
					}
					
					PluginOrderListDomain plugin_order_list_domain =  new PluginOrderListDomain();
					plugin_order_list_domain.setIdx(0);
					plugin_order_list_domain.setData_id(order_get_domain.getData().get(i).getId());
					plugin_order_list_domain.setSeller_id(order_get_domain.getData().get(i).getSellerId());;
					plugin_order_list_domain.setPaymentId(order_get_domain.getData().get(i).getPaymentId());
					plugin_order_list_domain.setPurchaseId(order_get_domain.getData().get(i).getPurchaseId());
					plugin_order_list_domain.setProductId(order_get_domain.getData().get(i).getProductId());
					plugin_order_list_domain.setSettlementId(order_get_domain.getData().get(i).getSettlementId());
					
					int order_status = 0;
					if(order_get_domain.getData().get(i).getStatus().equals("paid")==true 
							|| order_get_domain.getData().get(i).getStatus().equals("preparingForDelivery")==true) {
						
						//requested
						order_status= 0; // 주문
						
					}else if(order_get_domain.getData().get(i).getStatus().equals("waitingToBePickedUp")==true 
							|| order_get_domain.getData().get(i).getStatus().equals("waitingForDeliveryReception")==true
							|| order_get_domain.getData().get(i).getStatus().equals("deliveryReceptionFailed")==true
							) {
						//processing
						order_status= 1; // 가능
					
					}else if(order_get_domain.getData().get(i).getStatus().equals("inTransit")==true 
							|| order_get_domain.getData().get(i).getStatus().equals("deliveryCompleted")==true
							|| order_get_domain.getData().get(i).getStatus().equals("purchaseCompleted")==true
							) {
						//delivered
						order_status= 2; // 발송
						
					}else if(order_get_domain.getData().get(i).getStatus().equals("refunded")==true 
							|| order_get_domain.getData().get(i).getStatus().equals("cancelled")==true
							) {
						//cancelled
						order_status= 3; // 취소
						
					}else {
						order_status= 4; // 그 외
					}
					
					plugin_order_list_domain.setOrder_status(order_status);
					plugin_order_list_domain.setOrder_sub_status(0);// 초기 insert에서만 필요: 관망중
					plugin_order_list_domain.setMoim_status(order_get_domain.getData().get(i).getStatus());
					plugin_order_list_domain.setUser_id(order_get_domain.getData().get(i).getUserId());;
					plugin_order_list_domain.setBuyer_name(order_get_domain.getData().get(i).getPurchase().getBuyerName());
					plugin_order_list_domain.setPaid_date(paid_date);
					plugin_order_list_domain.setProduct_name(order_get_domain.getData().get(i).getProductName());
					
					if(EmptyUtils.isEmpty(order_get_domain.getData().get(i).getPurchase().getBuyerPhone())==false) {
					}
					plugin_order_list_domain.setRecipient_name(order_get_domain.getData().get(i).getPurchase().getRecipientName());
					if(EmptyUtils.isEmpty(order_get_domain.getData().get(i).getPurchase().getRecipientPhone())==false) {
					}
					plugin_order_list_domain.setZipcode(order_get_domain.getData().get(i).getPurchase().getZipcode());;
					
					plugin_order_list_domain.setAddress(order_get_domain.getData().get(i).getPurchase().getAddress());
					if(EmptyUtils.isEmpty(order_get_domain.getData().get(i).getPurchase().getMemo())==false) {
						plugin_order_list_domain.setMemo(order_get_domain.getData().get(i).getPurchase().getMemo());	
					}else {
						plugin_order_list_domain.setMemo("");
					}
					
					String updated_at = "";
					String refunded_at = "";
					String created_at = "";
					try {
						if(order_get_domain.getData().get(i).getUpdatedAt()>0) {
							updated_at = LocationTimeCal.TimeStamptoDate(order_get_domain.getData().get(i).getUpdatedAt());
						}
						if(order_get_domain.getData().get(i).getRefundedAt()>0) {
							refunded_at = LocationTimeCal.TimeStamptoDate(order_get_domain.getData().get(i).getRefundedAt());
						}
						if(order_get_domain.getData().get(i).getCreatedAt()>0) {
							created_at = LocationTimeCal.TimeStamptoDate(order_get_domain.getData().get(i).getCreatedAt());
						}
					} catch (Exception eTime) {
						logger.info("RdsSyncProcess LocationTimeCal.TimeStamptoDate : eTime:"+eTime);
					}
					plugin_order_list_domain.setPaid_date_unixtime(order_get_domain.getData().get(i).getPaidAt());
					plugin_order_list_domain.setUpdated_at_unixtime(order_get_domain.getData().get(i).getUpdatedAt());
					plugin_order_list_domain.setRefunded_at_unixtime(order_get_domain.getData().get(i).getRefundedAt());
					plugin_order_list_domain.setCreated_at_unixtime(order_get_domain.getData().get(i).getCreatedAt());
					
					plugin_order_list_domain.setUpdated_at(updated_at);
					plugin_order_list_domain.setRefunded_at(refunded_at);	
					plugin_order_list_domain.setCreated_at(created_at);
					
					if(order_status < 4) {  
						try {
							int iRow = 0;
							iRow = orderService.insertPluginOrderList(plugin_order_list_domain);
							logger.info("RdsSyncProcess insertPluginOrderList iRow:"+iRow);
							logger.info("RdsSyncProcess insertPluginOrderList idx:"+plugin_order_list_domain.getIdx());	
							if(iRow>0) {
								Model model_ins_3 = new ExtendedModelMap();
								model_ins_3.addAttribute("plugin_order_list_idx", plugin_order_list_domain.getIdx());			
								model_ins_3.addAttribute("data_id", order_get_domain.getData().get(i).getId());
								model_ins_3.addAttribute("seller_id", order_get_domain.getData().get(i).getSellerId());
								model_ins_3.addAttribute("paymentId", order_get_domain.getData().get(i).getPaymentId());
								model_ins_3.addAttribute("purchaseId", order_get_domain.getData().get(i).getPurchaseId());
								model_ins_3.addAttribute("productId", order_get_domain.getData().get(i).getProductId());
								model_ins_3.addAttribute("product_variant_id", order_get_domain.getData().get(i).getProductVariantId());
								String var_val2 = "";
								String[] var_val_array2 = null;
								if(EmptyUtils.isEmpty(order_get_domain.getData().get(i).getProductVariantValue())==false) {
									var_val2 = order_get_domain.getData().get(i).getProductVariantValue().toString();
									var_val2 = var_val2.replaceAll("\\{", "");
									var_val2 = var_val2.replaceAll("\\}", "");
								}
								
								if(EmptyUtils.isEmpty(var_val2)==false) {
									var_val_array2 = var_val2.split("=");
									for(int ii = 0; ii < var_val_array2.length; ii++) {
										logger.info("RdsSyncProcess bb:"+var_val_array2[ii]);
										
										if(ii==2){
											if(EmptyUtils.isEmpty(var_val_array2[ii])==false) {
												var_val2 =	var_val_array2[ii];
											}
										}
									}
								}
								model_ins_3.addAttribute("product_variant_name", var_val2);	
								model_ins_3.addAttribute("product_variant_quantity", order_get_domain.getData().get(i).getQuantity());
								model_ins_3.addAttribute("product_box_cnt", 1);
								
								if(iRow>0) {
									int iRow2 = 0;
									iRow2 = orderService.insertPluginOrderSubList(model_ins_3);
									if(iRow2==0) {
										logger.info("RdsSyncProcess insertPluginOrderSubList iRow2:"+iRow2);	
									}
								}else {
								}	
							}
						} catch (Exception e) {
							error_status.setError_code(121);
							error_status.setError_val("insertPluginOrderList insert error : "+e);
							break;
						}
					}
				}
			}// for  
		}else {
			error_status.setError_code(121);
			error_status.setError_val(" getData() empty!! ");
		}
		return error_status;
	}
	

	/**
	 * @desc  
	 */
	public DefaultDomain.ErrorCheck AccountsRdsSyncProcess(String connect_type
			, String seller_id, String sync_type ) {
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		
		return error_status;
	}
	

	/**
	 * @desc  
	 */
	public DefaultDomain.ErrorCheck AccountsRdsSyncProcessAll(String connect_type
			, String seller_id, String sync_type ) {
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		
		return error_status;
	}
	

	/**
	 * @desc  
	 */
	public List<ReturnDatav1> OrderListMakeListV2(String connect_type, 
			List<String> data_id_list, String seller_id
			, int out_type, String error_reason, int reason_type) {
		
		List<ReturnDatav1> datav2 = new ArrayList<ReturnDatav1>();
		OrderGetDomain.GetData orderinfo_moim_get = new OrderGetDomain.GetData();
		List<productVarinatSingle> moim_get_data_list = new ArrayList<productVarinatSingle>();

		try {
			try {
				String get_order_single = "";
				get_order_single = moimApiService.OrderSingleGet(connect_type, seller_id, data_id_list);
				logger.info("OrderListMakeListV2 get_order_single:"+get_order_single+"//"+seller_id);
				
				orderinfo_moim_get = gson.fromJson(get_order_single, orderinfo_moim_get.getClass());
			} catch (Exception e1) {
				logger.info("OrderListMakeListV2 moimApiService.OrderSingleGet order_moim_get_domain json e:"+e1+"//"+seller_id);
				datav2.add(null);
			}
			
			moim_get_data_list.addAll(orderinfo_moim_get.getData());
			if(EmptyUtils.isEmpty(moim_get_data_list)==false) {
			
				if(moim_get_data_list.size()>0) {
					moim_get_data_list.sort(Comparator.comparing(OrderGetDomain.productVarinatSingle::getPurchaseId, Collections.reverseOrder()));
					moim_get_data_list.sort(Comparator.comparing(OrderGetDomain.productVarinatSingle::getCreatedAt, Collections.reverseOrder()));
					
					String last_poduct_key = "";
					for(int j = 0; j < moim_get_data_list.size(); j++) {
						logger.info("OrderListMakeListV2 orderinfo_moim_get.getProductId"
								+ ":"+orderinfo_moim_get.getData().get(j).getProductId()+"//"+seller_id);
						logger.info("OrderListMakeListV2 orderinfo_moim_get.getPurchaseId"
								+ ":"+orderinfo_moim_get.getData().get(j).getPurchaseId()+"//"+seller_id);
						logger.info("OrderListMakeListV2 orderinfo_moim_get.getCreatedAt"
								+ ":"+orderinfo_moim_get.getData().get(j).getCreatedAt()+"//"+seller_id);
						try {
				  				String group_check = "";
			  	  				group_check = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
				  				if(last_poduct_key.equals(group_check)==false) {
				  					ReturnDatav1 date_single = new ReturnDatav1();
				  					List<OrderDomain.ProductVariantSingle> product_variant_single_list = new ArrayList<OrderDomain.ProductVariantSingle>();
				  					date_single = new ReturnDatav1();
			  						date_single.setOrder_key(moim_get_data_list.get(j).getPaymentId());
			  						date_single.setOrder_status(moim_get_data_list.get(j).getStatus());
			  						date_single.setUser_id(moim_get_data_list.get(j).getUserId());
			  						date_single.setUser_name(moim_get_data_list.get(j).getPurchase().getBuyerName());
			  						date_single.setRecipient_name(moim_get_data_list.get(j).getPurchase().getRecipientName());
			  						date_single.setProduct_key(moim_get_data_list.get(j).getProductId());
			  						date_single.setProduct_name(moim_get_data_list.get(j).getProductName());
			  						String pay_date = "";
					  	  			if(EmptyUtils.isEmpty(moim_get_data_list.get(j).getPaidAt())==false) {
										if(moim_get_data_list.get(j).getPaidAt()>0) {
											pay_date = LocationTimeCal.TimeStamptoDate(moim_get_data_list.get(j).getPaidAt());
										}
									}
				  	  				date_single.setPay_date(pay_date);
				  	  				
				  	  				logger.info("OrderListMakeListV2 error_reason : "+error_reason+"//"+seller_id);
				  	  				logger.info("OrderListMakeListV2 reason_type:"+reason_type+"//"+seller_id);
				  	  				
					  	  			if(EmptyUtils.isEmpty(error_reason)==false && reason_type == 1) {
					  	  				date_single.setFailures_reason(error_reason);
									}
									if(EmptyUtils.isEmpty(error_reason)==false && reason_type == 2) {
										date_single.setUnavailables_reason(error_reason);
									}
									if(EmptyUtils.isEmpty(error_reason)==false && reason_type == 3) {
										date_single.setDelivery_reception(error_reason);
									}
				  	  				
					  	  			for (int i2= 0; i2 < moim_get_data_list.size(); i2++) {
					  	  				if(moim_get_data_list.get(j).getPaymentId().equals(moim_get_data_list.get(i2).getPaymentId())==true
					  	  						&& moim_get_data_list.get(j).getProductId().equals(moim_get_data_list.get(i2).getProductId())==true 
					  	  						) {
							  	  			OrderDomain.ProductVariantSingle pruduct_variant_single = new OrderDomain.ProductVariantSingle();
							  	  			pruduct_variant_single.setProduct_variant_key(moim_get_data_list.get(i2).getId()); //CI:
					  						pruduct_variant_single.setProduct_variant_id(moim_get_data_list.get(i2).getProductVariantId());
					  						String var_val1 = "";
					  						String[] var_val_array1 = null;
					  						if(EmptyUtils.isEmpty(moim_get_data_list.get(i2).getProductVariantValue())==false) {
					  							var_val1 = moim_get_data_list.get(i2).getProductVariantValue().toString();
					  							var_val1 = var_val1.replaceAll("\\{", "");
					  							var_val1 = var_val1.replaceAll("\\}", "");
					  						}
					  						if(EmptyUtils.isEmpty(var_val1)==false) {
					  							var_val_array1 = var_val1.split("=");
					  							for(int ii = 0; ii < var_val_array1.length; ii++) {
					  								if(ii==2){
					  									if(EmptyUtils.isEmpty(var_val_array1[ii])==false) {
					  										var_val1 =	var_val_array1[ii];
					  									}
					  								}
					  							}
					  						}
											pruduct_variant_single.setProduct_variant_name(var_val1);
											pruduct_variant_single.setQuantity(moim_get_data_list.get(i2).getQuantity());
											product_variant_single_list.add(pruduct_variant_single);	
						  	  			}
					  	  			
					  	  			}
					  	  		date_single.setProduct_variant_list(product_variant_single_list);
			  					datav2.add(date_single);
				  				}
				  				last_poduct_key = moim_get_data_list.get(j).getPurchaseId()+moim_get_data_list.get(j).getProductId();
						} catch (Exception e1) {
							logger.info("OrderListMakeListV2 getOrderListNewOrderV2 "
									+ "data_id_hold.indexOf(moim_get_data_list.get(j).getId()) : e :"+e1+"//"+seller_id);
							datav2.add(null);
						}
					}
				}else {
					datav2.add(null);
				}
			}else {
				datav2.add(null);
			}
		} catch (Exception e) {
			logger.info("OrderListMakeListV2 > error e :"+e+"//"+seller_id);
			datav2.add(null);
		}
		return datav2;
	}
	

	/**
	 * @desc  
	 */
	public List<OrderDomain.ReturnData> OrderListMakeList(List<String> data_list
			, String seller_id, int out_type, String error_reason, int reason_type) {
		
		List<OrderDomain.ReturnData> list_data = new ArrayList<OrderDomain.ReturnData>();
		List<OrderDomain.PluginOrderStatusResult> plugin_order_status_result = new ArrayList<OrderDomain.PluginOrderStatusResult>();

		try {
			Model model_get2 = new ExtendedModelMap();
			model_get2.addAttribute("data_id_list", data_list);
			model_get2.addAttribute("seller_id", seller_id);
			plugin_order_status_result = orderService.getPluginOrderStatusResult(model_get2);
			
			if(EmptyUtils.isEmpty(plugin_order_status_result)==false) {
				
				if(plugin_order_status_result.size()>0) {

					String last_order_key = "";
					OrderDomain.ReturnData single_data = new OrderDomain.ReturnData();
					
					for(int k = 0; k < plugin_order_status_result.size(); k++) {
						logger.info("OrderListMakeList "+k+":last_order_key:"+last_order_key+"//"+seller_id);
						logger.info("OrderListMakeList "+k+":plugin_order_status_result.get(k).getPurchaseId()"
								+ ":"+plugin_order_status_result.get(k).getPurchaseId()+"//"+seller_id);
						if(k==0) {
							
						}else {
							
						}
						if(plugin_order_status_result.get(k).getPurchaseId().equals(last_order_key)==false) {
							logger.info("OrderListMakeList out_type:"+out_type+"//"+seller_id);
							
							single_data = new OrderDomain.ReturnData();
							single_data.setData_id(data_list);
							single_data.setOrder_key(plugin_order_status_result.get(k).getPurchaseId());
							if(EmptyUtils.isEmpty(error_reason)==false && reason_type == 1) {
								single_data.setFailures_reason(error_reason);
							}
							if(EmptyUtils.isEmpty(error_reason)==false && reason_type == 2) {
								single_data.setUnavailables_reason(error_reason);
							}
							if(EmptyUtils.isEmpty(error_reason)==false && reason_type == 3) {
								single_data.setDelivery_reception(error_reason);
							}
							if(out_type==0) {
								logger.info("OrderListMakeList out_type:"+out_type+"//"+seller_id);
								single_data.setUser_id(plugin_order_status_result.get(k).getUser_id());
								single_data.setUser_name(plugin_order_status_result.get(k).getUser_name());
								single_data.setPay_date(plugin_order_status_result.get(k).getPay_date());	
							}
							
							String last_pur_id = "";
							String last_pro_key = "";
							List<ProductSingle> product_list = new ArrayList<ProductSingle>();
							for(int kk = 0; kk < plugin_order_status_result.size(); kk++) {
								if(plugin_order_status_result.get(kk).getPurchaseId().equals(plugin_order_status_result.get(k).getPurchaseId())==true){
									logger.info("OrderListMakeList "+k+":last_pro_key:"+last_pro_key+"//"+seller_id);
									logger.info("OrderListMakeList "+k+":last_pur_id:"+last_pur_id+"//"+seller_id);
									if(plugin_order_status_result.get(kk).getProduct_key().equals(last_pro_key)==false) {
										OrderDomain.ProductSingle product_single = new OrderDomain.ProductSingle();
										product_single.setProduct_key(plugin_order_status_result.get(kk).getProduct_key());
										if(out_type==0) {
											product_single.setProduct_name(plugin_order_status_result.get(kk).getProduct_name());
										}
										logger.info("OrderListMakeList "+k+":setProduct_key"
												+ ":"+plugin_order_status_result.get(kk).getProduct_key()+"//"+seller_id);
										
										if(out_type==0) {
											List<ProductVariantSingle> product_variant_list = new ArrayList<ProductVariantSingle>();
											for(int jj = 0; jj < plugin_order_status_result.size(); jj++) {
												if(plugin_order_status_result.get(jj).getPurchaseId().equals(plugin_order_status_result.get(kk).getPurchaseId())==true){
													if(plugin_order_status_result.get(jj).getProduct_key().equals(plugin_order_status_result.get(kk).getProduct_key())==true) {
														logger.info("OrderListMakeList "+jj+":"
																+ "getProduct_variant_id:"+plugin_order_status_result.get(jj).getProduct_variant_id()+"//"+seller_id);
														OrderDomain.ProductVariantSingle product_variant_single = new OrderDomain.ProductVariantSingle();
														product_variant_single.setProduct_variant_id(plugin_order_status_result.get(jj).getProduct_variant_id());
														product_variant_single.setProduct_variant_value(plugin_order_status_result.get(jj).getProduct_variant_value());
														product_variant_single.setQuantity(plugin_order_status_result.get(jj).getQuantity());
														product_variant_list.add(product_variant_single);
													}
												}
											}
											product_single.setProduct_variant_list(product_variant_list);
										}
										product_list.add(product_single);
										last_pro_key = plugin_order_status_result.get(kk).getProduct_key();
									}
								}
								last_pur_id = plugin_order_status_result.get(kk).getPurchaseId();
							}
							single_data.setProduct_list(product_list);
						}else {
						}
						if(last_order_key.equals(plugin_order_status_result.get(k).getPurchaseId())==false) {
							list_data.add(single_data);	
						}
						last_order_key = plugin_order_status_result.get(k).getPurchaseId();
					} 
				}else {
					list_data.add(null);
				}
			}else {
				list_data.add(null);
			}
		} catch (Exception e) {
			logger.info("OrderListMakeList > getPluginOrderStatusResult > error e :"+e+"//"+seller_id);
			
			list_data.add(null);
		}
		return list_data;
	}
	

	/**
	 * @desc  
	 */
	public String AlarmTalkSeller(List<OrderDomain.ReturnDatav1> success_list
			, String connect_type, SellerIdInfoDomain sellerid_info, String seller_token ) {
		
			String alarn_talk_send_result = "";
			String product_name = ""; 
			String product_key = "";
			String order_key = "";
			List<DefaultDomain.KakaoAlramTalkPostBody> post_body = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
			DefaultDomain.KakaoAlramTalkPostBody body_single = new DefaultDomain.KakaoAlramTalkPostBody();
			DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
			try {
				if(success_list.size()>0) {
					
					if (success_list != null && !success_list.isEmpty()) {
					    OrderDomain.ReturnDatav1 first = success_list.get(0);
					    product_name = first.getProduct_name();
					    product_key  = first.getProduct_key();
					    order_key    = first.getOrder_key();
					    
					}
					
					if(EmptyUtils.isEmpty(product_name)==false) {
						String seller_app_domain = ""; 
						if(connect_type.equals("prod")==true) {
							seller_app_domain = "padobox-seller.moimplugin.com/app";
						}else {
							seller_app_domain = "padobox-seller-dev.moimplugin.com/app";
						}	
						String msgid = "";
						msgid = "PS:"+GetNumber.getRandomKey(10);
						logger.info("AlarmTalkSeller msgid:"+msgid+"//"+sellerid_info.getId());
						logger.info("AlarmTalkSeller getPhoneNumber:"+sellerid_info.getContactInformation().getPhoneNumber()+"//"+sellerid_info.getId());
						String receiver_phone_num = "";
						if(EmptyUtils.isEmpty(sellerid_info.getContactInformation())==false) {
							if(EmptyUtils.isEmpty(sellerid_info.getContactInformation().getPhoneNumber())==false) {
								receiver_phone_num = sellerid_info.getContactInformation().getPhoneNumber();
								logger.info("010: "+receiver_phone_num.substring(0, 3)+"//"+sellerid_info.getId());
								String check_val = receiver_phone_num.substring(0, 3);
								
								if(check_val.equals("010")==true) {
									receiver_phone_num = receiver_phone_num.substring(1);
								}else {
									logger.info("AlarmTalkSeller phone number 010 error"
											+ ":"+sellerid_info.getContactInformation().getPhoneNumber()+"//"+sellerid_info.getId());	
									receiver_phone_num = "";
								}
							}else {
								logger.info("AlarmTalkSeller phone number error"
										+ ":"+sellerid_info.getContactInformation().getPhoneNumber()+"//"+sellerid_info.getId());
								receiver_phone_num = "";
							}
						}else {
							logger.info("AlarmTalkSeller sellerid_info phone number error"
									+ ":"+gson.toJson(sellerid_info)+"//"+sellerid_info.getId());
							receiver_phone_num = "";
						}
						
	if(EmptyUtils.isEmpty(receiver_phone_num)==false) {
		body_single.setMsgid(msgid);
		body_single.setMessage_type("AI"); 
		body_single.setProfile_key(defaultConfig.getKakaoProfileKey());
		body_single.setTemplate_code("newpadobox_fisher_processing_0");
		body_single.setReceiver_num("+82"+receiver_phone_num);
		body_single.setReserved_time("00000000000000");
		body_single.setMessage("[조업 중]"
		+ "\n\n어종명 : "+product_name+""
		+ "\n\n1~3일내 발송이 가능하다고 회원님께 전달하였습니다."
		+ "\n\n실제 택배발송이 완료되면 버튼을 클릭해서 “조업중”이라고 표시된 주문건을 “발송 완료”로 변경해 주세요.");
		button1.setName("조업 중 목록보기");
		button1.setType("WL");  
		button1.setUrl_mobile("https://"+seller_app_domain+"/work/processing?token="+seller_token);
		button1.setUrl_pc("https://"+seller_app_domain+"/work/processing?token="+seller_token);
		body_single.setButton1(button1);
		post_body.add(0,body_single);
		logger.info("AlarmTalkSeller body_single:"+gson.toJson(body_single)+"//"+sellerid_info.getId());
		String post_result = "";
		post_result = moimApiService.AlarmTalkPost(post_body);
		logger.info("AlarmTalkSeller post_result:"+post_result+"//"+sellerid_info.getId());
		if(EmptyUtils.isEmpty(post_result)==false) {
		DefaultDomain.KakaoAlramTalkPostResult[] post_result_array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
		List<DefaultDomain.KakaoAlramTalkPostResult> post_result_list = Arrays.asList(post_result_array);
		logger.info("AlarmTalkSeller post_result_list.size():"+post_result_list.size()+"//"+sellerid_info.getId()); 
			for(int ii = 0; ii < post_result_list.size(); ii++) {
				logger.info("AlarmTalkSeller result:"+post_result_list.get(ii).getResult()+"//"+sellerid_info.getId());
				Model model_ins_log = new ExtendedModelMap();
				model_ins_log.addAttribute("seller_id", sellerid_info.getId());
				model_ins_log.addAttribute("send_target", "seller");
				model_ins_log.addAttribute("send_key", "answer");
				model_ins_log.addAttribute("purchaseId", order_key);
				model_ins_log.addAttribute("productId", product_key);
				model_ins_log.addAttribute("msgid", body_single.getMsgid());
				model_ins_log.addAttribute("message_type", body_single.getMessage_type());
				model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
				model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
				model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
				model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
				model_ins_log.addAttribute("message", body_single.getMessage());
				model_ins_log.addAttribute("button1", gson.toJson(button1));
				model_ins_log.addAttribute("post_result", post_result_list.get(ii).getResult());
				model_ins_log.addAttribute("post_sendtime", post_result_list.get(ii).getSendtime());
				model_ins_log.addAttribute("post_body", gson.toJson(post_body));
				model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
				orderService.insertKakaoAlarmTalkLog(model_ins_log);
			}
			alarn_talk_send_result = "succes";
		}
	}
					}
				}
				
			} catch (Exception e) {
				logger.info("AlarmTalkSeller sendtype 1 : AlarmTalkPost error:"+e+"//"+sellerid_info.getId());
				Model model_ins_log = new ExtendedModelMap();
				model_ins_log.addAttribute("seller_id", sellerid_info.getId());
				model_ins_log.addAttribute("send_target", "seller");
				model_ins_log.addAttribute("send_key", "answer");
				model_ins_log.addAttribute("purchaseId", order_key);
				model_ins_log.addAttribute("productId", product_key);
				model_ins_log.addAttribute("msgid", body_single.getMsgid());
				model_ins_log.addAttribute("message_type", body_single.getMessage_type());
				model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
				model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
				model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
				model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
				model_ins_log.addAttribute("message", body_single.getMessage());
				model_ins_log.addAttribute("button1", gson.toJson(button1));
				model_ins_log.addAttribute("post_result", "sendtype 1 error:"+e);
				model_ins_log.addAttribute("post_sendtime", "");
				model_ins_log.addAttribute("post_body", gson.toJson(post_body));
				model_ins_log.addAttribute("post_result_body", "");
				orderService.insertKakaoAlarmTalkLog(model_ins_log);
				alarn_talk_send_result = "failure";
			}
			
			logger.info("AlarmTalkSeller DefaultConfig.kakao_patner_send_yn:"+defaultConfig.getKakaoPatnerSendYn());
				if(defaultConfig.getKakaoPatnerSendYn().equals("on")==true) {
					
					List<AlarmTalkPartnerDomain.Data> getSingleData = new ArrayList<AlarmTalkPartnerDomain.Data>();
					Model model_get_1 = new ExtendedModelMap();
					model_get_1.addAttribute("get_type", null);
					model_get_1.addAttribute("seller_id", sellerid_info.getId());
					getSingleData = alarmPartnerService.getKakaoAlarmTalkPartnerList(model_get_1);
					if(EmptyUtils.isEmpty(getSingleData)==false){
						if(getSingleData.size()>0){
							
							
		for(int i = 0; i < getSingleData.size(); i++) {
							
			if(EmptyUtils.isEmpty(getSingleData.get(i).getPartner_phone())==false) {

				String seller_app_domain = ""; 
				if(connect_type.equals("prod")==true) {
					seller_app_domain = "padobox-seller.moimplugin.com/app";
				}else {
					seller_app_domain = "padobox-seller-dev.moimplugin.com/app";
				}	
				String msgid = "";
				msgid = "PS:"+GetNumber.getRandomKey(10);
				logger.info("AlarmTalkSeller msgid:"+msgid+"//"+sellerid_info.getId());
				logger.info("AlarmTalkSeller getPhoneNumber:"+sellerid_info.getContactInformation().getPhoneNumber());
				
				String patner_receiver_phone_num = "";
				logger.info("AlarmTalkSeller getPhoneNumber:"+getSingleData.get(i).getPartner_phone()+"//"+sellerid_info.getId());
				patner_receiver_phone_num = getSingleData.get(i).getPartner_phone();
				logger.info("AlarmTalkSeller 010: "+patner_receiver_phone_num.substring(0, 3)+"//"+sellerid_info.getId());
				String check_val = patner_receiver_phone_num.substring(0, 3);
				
				if(check_val.equals("010")==true) {
					patner_receiver_phone_num = patner_receiver_phone_num.substring(1);
				}else {
					logger.info("AlarmTalkSeller phone number 010 error:"+getSingleData.get(i).getPartner_phone()+"//"+sellerid_info.getId());	
					patner_receiver_phone_num = "";
				}
				
				if(EmptyUtils.isEmpty(patner_receiver_phone_num)==false) {
					List<DefaultDomain.KakaoAlramTalkPostBody> post_body_sub = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
					DefaultDomain.KakaoAlramTalkPostBody body_single_sub = new DefaultDomain.KakaoAlramTalkPostBody();
					DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1_sub = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
					body_single_sub.setMsgid(msgid);
					body_single_sub.setMessage_type("AI");
					body_single_sub.setProfile_key(defaultConfig.getKakaoProfileKey());
					body_single_sub.setTemplate_code("newpadobox_fisher_processing_0");
					body_single_sub.setReceiver_num("+82"+patner_receiver_phone_num);
					body_single_sub.setReserved_time("00000000000000");
					body_single_sub.setMessage("[조업 중]"
					+ "\n\n어종명 : "+product_name+""
					+ "\n\n1~3일내 발송이 가능하다고 회원님께 전달하였습니다."
					+ "\n\n실제 택배발송이 완료되면 버튼을 클릭해서 “조업중”이라고 표시된 주문건을 “발송 완료”로 변경해 주세요.");
					button1_sub.setName("조업 중 목록보기");
					button1_sub.setType("WL"); 
					button1_sub.setUrl_mobile("https://"+seller_app_domain+"/work/processing?token="+seller_token);
					button1_sub.setUrl_pc("https://"+seller_app_domain+"/work/processing?token="+seller_token);
					body_single_sub.setButton1(button1_sub);
					post_body_sub.add(0,body_single_sub);
					logger.info("AlarmTalkSeller body_single:"+gson.toJson(body_single_sub)+"//"+sellerid_info.getId());
					String post_result = "";
					post_result = moimApiService.AlarmTalkPost(post_body_sub);
					logger.info("AlarmTalkSeller post_result:"+post_result+"//"+sellerid_info.getId());
				
					if(EmptyUtils.isEmpty(post_result)==false) {
						DefaultDomain.KakaoAlramTalkPostResult[] array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
						List<DefaultDomain.KakaoAlramTalkPostResult> list = Arrays.asList(array);
						logger.info("AlarmTalkSeller list.size():"+list.size()+"//"+sellerid_info.getId());
						for(int ii = 0; ii < list.size(); ii++) {
							logger.info("AlarmTalkSeller result:"+list.get(ii).getResult()+"//"+sellerid_info.getId());
							Model model_ins_log = new ExtendedModelMap();
							model_ins_log.addAttribute("seller_id", sellerid_info.getId());
							model_ins_log.addAttribute("send_target", "seller:"+getSingleData.get(i).getPartner_id());
							model_ins_log.addAttribute("send_key", "answer");
							model_ins_log.addAttribute("purchaseId", order_key);
							model_ins_log.addAttribute("productId", product_key);
							model_ins_log.addAttribute("msgid", body_single_sub.getMsgid());
							model_ins_log.addAttribute("message_type", body_single_sub.getMessage_type());
							model_ins_log.addAttribute("profile_key", body_single_sub.getProfile_key());
							model_ins_log.addAttribute("template_code", body_single_sub.getTemplate_code());
							model_ins_log.addAttribute("receiver_num", body_single_sub.getReceiver_num());
							model_ins_log.addAttribute("reserved_time", body_single_sub.getReserved_time());
							model_ins_log.addAttribute("message", body_single_sub.getMessage());
							model_ins_log.addAttribute("button1", gson.toJson(button1_sub));
							model_ins_log.addAttribute("post_result", list.get(ii).getResult());
							model_ins_log.addAttribute("post_sendtime", list.get(ii).getSendtime());
							model_ins_log.addAttribute("post_body", gson.toJson(post_body));
							model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
							orderService.insertKakaoAlarmTalkLog(model_ins_log);
						}
					}
				}
			}
		}
						}
					}
				}
		
		return alarn_talk_send_result;
	}
	

	/**
	 * @desc  
	 */
	public String AlarmTalkBuyer(int send_type, String connect_type, String buyer_name
			, String receiver_phone_num, String order_number
			, String seller_id, String purchaseId, String productId, String send_key) {
		
		if(send_type==1) {
			logger.info("AlarmTalkBuyer buyer_name ->"+ buyer_name+"//"+seller_id);
			logger.info("AlarmTalkBuyer order_number ->"+ order_number+"//"+seller_id);
			logger.info("AlarmTalkBuyer receiver_phone_num ->"+ receiver_phone_num+"//"+seller_id);
			
			logger.info("AlarmTalkBuyer 010: "+receiver_phone_num.substring(0, 3)+"//"+seller_id);
			String check_val = receiver_phone_num.substring(0, 3);
			if(check_val.equals("010")==true){
				receiver_phone_num = receiver_phone_num.substring(1); 
			}else {
				receiver_phone_num = "";
			}
			String button_url = "";
			if(connect_type.equals("prod")==true) {
				button_url = "padobox.kr";
			}else {
				button_url = "dev-padobox.vingle.network";
			}	
			if(EmptyUtils.isEmpty(receiver_phone_num)==false) {
				if(EmptyUtils.isEmpty(buyer_name)==true) {
					buyer_name = "구매 고객님";
				}
				String msgid = "";
				msgid = "PU:"+GetNumber.getRandomKey(10);
				logger.info("AlarmTalkBuyer msgid:"+msgid+"//"+seller_id);
				logger.info("AlarmTalkBuyer receiver_phone_num:"+receiver_phone_num+"//"+seller_id);
				
				List<DefaultDomain.KakaoAlramTalkPostBody> post_body = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
				DefaultDomain.KakaoAlramTalkPostBody body_single = new DefaultDomain.KakaoAlramTalkPostBody();
				DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
				DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button2 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
				
				body_single.setMsgid(msgid);
				body_single.setMessage_type("AI");
				body_single.setProfile_key(defaultConfig.getKakaoProfileKey());
				body_single.setTemplate_code("newpadobox_fisher_observ_0");
				body_single.setReceiver_num("+82"+receiver_phone_num);
				body_single.setReserved_time("00000000000000");
				body_single.setMessage("[조업 관망 중]\r\n"
				+ "\r\n"
				+ ""+buyer_name+" 님\r\n"
				+ "현재 어부가 조업 상황을 지켜보는 중입니다.\r\n"
				+ "조업 환경이 나아지면 조업 후 발송하겠습니다.\r\n"
				+ "\r\n"
				+ "[관망 주요 사유]\r\n"
				+ "조업량 부족, 입항 지연, 기상 악화, 어장 사고\r\n"
				+ "\r\n"
				+ "관망 중 메세지는 잡자 마자 보낸다는 파도상자만의 약속입니다.\r\n"
				+ "\r\n"
				+ "주문 번호 : "+order_number+"\r\n"
				+ "\r\n"
				+ "더 궁금하신 점이 있으실 경우\r\n"
				+ "채팅 문의를 통하여 도움을 드리겠습니다.\r\n"
				+ "\r\n"
				+ "조업을 기다리면, 더 신선해진다\r\n"
				+ "파도상자");
				
				button1.setName("주문 내역 확인");
				button1.setType("WL"); 
				button1.setUrl_mobile("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
				button1.setUrl_pc("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
				
				button2.setName("채팅 문의하기");
				button2.setType("WL"); 
				button2.setUrl_mobile("https://cand-padobox-static.s3.us-east-1.amazonaws.com/hellotalk-redirector/index.html?ticket=eyJhbG[…]0.A6Z6FABMyO-EwzmTSs15h6KPRhjDGWhloDfxWU8kPik");
				
				body_single.setButton1(button1);
				body_single.setButton2(button2);
				post_body.add(0,body_single);
				
				logger.info("AlarmTalkBuyer body_single:"+gson.toJson(body_single)+"//"+seller_id);
				
				String post_result = "";
				post_result = moimApiService.AlarmTalkPost(post_body);
				logger.info("AlarmTalkBuyer kakao_send_yn:"+defaultConfig.getKakaoSendYn()+"//"+seller_id);
				logger.info("AlarmTalkBuyer post_result:"+post_result+"//"+seller_id);
				
				if(EmptyUtils.isEmpty(post_result)==false) {
					DefaultDomain.KakaoAlramTalkPostResult[] array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
					List<DefaultDomain.KakaoAlramTalkPostResult> list = Arrays.asList(array);
					
					logger.info("AlarmTalkBuyer list.size():"+list.size()+"//"+seller_id);
					
					for(int ii = 0; ii < list.size(); ii++) {
						logger.info("AlarmTalkBuyer result:"+list.get(ii).getResult()+"//"+seller_id);
						Model model_ins_log = new ExtendedModelMap();
						model_ins_log.addAttribute("seller_id", seller_id);
						model_ins_log.addAttribute("send_target", "user");
						model_ins_log.addAttribute("send_key", send_key); 
						model_ins_log.addAttribute("purchaseId", purchaseId);
						model_ins_log.addAttribute("productId", productId);
						model_ins_log.addAttribute("msgid", body_single.getMsgid());
						model_ins_log.addAttribute("message_type", body_single.getMessage_type());
						model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
						model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
						model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
						model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
						model_ins_log.addAttribute("message", body_single.getMessage());
						model_ins_log.addAttribute("button1", gson.toJson(button1));
						model_ins_log.addAttribute("button2", gson.toJson(button2));
						model_ins_log.addAttribute("post_result", list.get(ii).getResult());
						model_ins_log.addAttribute("post_sendtime", list.get(ii).getSendtime());
						model_ins_log.addAttribute("post_body", gson.toJson(post_body));
						model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
						orderService.insertKakaoAlarmTalkLog(model_ins_log);
					}
				}
			}
		}else if(send_type==2){
			logger.info("AlarmTalkBuyer buyer_name ->"+ buyer_name+"//"+seller_id);
			logger.info("AlarmTalkBuyer order_number ->"+ order_number+"//"+seller_id);
			logger.info("AlarmTalkBuyer receiver_phone_num ->"+ receiver_phone_num+"//"+seller_id);
			
			logger.info("AlarmTalkBuyer 010: "+receiver_phone_num.substring(0, 3)+"//"+seller_id);
			String check_val = receiver_phone_num.substring(0, 3);
			if(check_val.equals("010")==true){
				receiver_phone_num = receiver_phone_num.substring(1);
			}else {
				receiver_phone_num = "";
			}
			String button_url = "";
			if(connect_type.equals("prod")==true) {
				button_url = "padobox.kr";
			}else {
				button_url = "dev-padobox.vingle.network";
			}	
			if(EmptyUtils.isEmpty(receiver_phone_num)==false) {
			
				if(EmptyUtils.isEmpty(buyer_name)==true) {
					buyer_name = "구매 고객님";
				}
				String msgid = "";
				msgid = "PU:"+GetNumber.getRandomKey(10);
				logger.info("AlarmTalkBuyer msgid:"+msgid+"//"+seller_id);
				logger.info("AlarmTalkBuyer receiver_phone_num:"+receiver_phone_num+"//"+seller_id);
				
				List<DefaultDomain.KakaoAlramTalkPostBody> post_body = new ArrayList<DefaultDomain.KakaoAlramTalkPostBody>();
				DefaultDomain.KakaoAlramTalkPostBody body_single = new DefaultDomain.KakaoAlramTalkPostBody();
				DefaultDomain.KakaoAlramTalkPostBody.ButtonObject button1 = new DefaultDomain.KakaoAlramTalkPostBody.ButtonObject();
				
				body_single.setMsgid(msgid);
				body_single.setMessage_type("AI");
				body_single.setProfile_key(defaultConfig.getKakaoProfileKey());
				body_single.setTemplate_code("newpadobox_fisher_observ_end_0");
				body_single.setReceiver_num("+82"+receiver_phone_num);
				body_single.setReserved_time("00000000000000");
				body_single.setMessage("[조업 성공]\r\n"
				+ "\r\n"
				+ "안녕하세요. "+buyer_name+" 님\r\n"
				+ "\r\n"
				+ "용왕님의 허락으로\r\n"
				+ "조업이 성공되어\r\n"
				+ "지금 배송 준비 중 입니다\r\n"
				+ "\r\n"
				+ "주문내역 : "+order_number+"\r\n"
				+ "\r\n"
				+ "조업을 기다리면, 더 신선해진다\r\n"
				+ "파도상자");
				
				button1.setName("주문 내역 확인");
				button1.setType("WL"); 
				button1.setUrl_mobile("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
				button1.setUrl_pc("https://"+button_url+"/commerce/my-shopping/payments/"+order_number+"");
				body_single.setButton1(button1);
				post_body.add(0,body_single);
				
				logger.info("AlarmTalkBuyer body_single:"+gson.toJson(body_single)+"//"+seller_id);
				
				String post_result = "";
				post_result = moimApiService.AlarmTalkPost(post_body);
				logger.info("AlarmTalkBuyer post_result:"+post_result+"//"+seller_id);
				
				if(EmptyUtils.isEmpty(post_result)==false) {
					DefaultDomain.KakaoAlramTalkPostResult[] array = gson.fromJson(post_result, DefaultDomain.KakaoAlramTalkPostResult[].class);
					List<DefaultDomain.KakaoAlramTalkPostResult> list = Arrays.asList(array);
					
					logger.info("AlarmTalkBuyer list.size():"+list.size()+"//"+seller_id);
					
					for(int ii = 0; ii < list.size(); ii++) {
						logger.info("AlarmTalkBuyer result:"+list.get(ii).getResult()+"//"+seller_id);
						Model model_ins_log = new ExtendedModelMap();
						model_ins_log.addAttribute("seller_id", seller_id);
						model_ins_log.addAttribute("send_target", "user");
						model_ins_log.addAttribute("send_key", send_key);
						model_ins_log.addAttribute("purchaseId", purchaseId);
						model_ins_log.addAttribute("productId", productId);
						model_ins_log.addAttribute("msgid", body_single.getMsgid());
						model_ins_log.addAttribute("message_type", body_single.getMessage_type());
						model_ins_log.addAttribute("profile_key", body_single.getProfile_key());
						model_ins_log.addAttribute("template_code", body_single.getTemplate_code());
						model_ins_log.addAttribute("receiver_num", body_single.getReceiver_num());
						model_ins_log.addAttribute("reserved_time", body_single.getReserved_time());
						model_ins_log.addAttribute("message", body_single.getMessage());
						model_ins_log.addAttribute("button1", gson.toJson(button1));
						model_ins_log.addAttribute("post_result", list.get(ii).getResult());
						model_ins_log.addAttribute("post_sendtime", list.get(ii).getSendtime());
						model_ins_log.addAttribute("post_body", gson.toJson(post_body));
						model_ins_log.addAttribute("post_result_body", gson.toJson(post_result));
						orderService.insertKakaoAlarmTalkLog(model_ins_log);
					}	
				}
			}
		}

		return "";
	}
	

	/**
	 * @desc  
	 */
	public DefaultDomain.ErrorCheck Observ(OrderBodyDomain.ModifyBodyList requestModifyBody
			, String connect_type	, String seller_id) {
		
		DefaultDomain.ErrorCheck error_status = new DefaultDomain.ErrorCheck();
		int error_code = 0;
		String error_val = "";
		String send_key = "";
		
		if(requestModifyBody.getModify_status()==1) {// 관망중 처리
			
			if(requestModifyBody.getModify_key()==1) {// 새로 들어온 주문
				send_key = "1_observ";
			}else if(requestModifyBody.getModify_key()==2) {// 가능
				send_key = "2_observ";
			}else if(requestModifyBody.getModify_key()==3) {// 관망중 리스트
				send_key = "3_observ";
			}
			
			for(int i = 0; i < requestModifyBody.getData().size(); i++) {
				OrderGetDomain.GetData order_moim_get_domain = new OrderGetDomain.GetData();
				List<String> ids_moim_send = new ArrayList<String>();
				List<String> product_key_list = new ArrayList<String>();
				String get_order_single = "";
				String order_key = "";
				
				if(EmptyUtils.isEmpty(requestModifyBody.getData().get(i).getOrder_key())==false) {
					order_key = requestModifyBody.getData().get(i).getOrder_key();// CY:00000000000 로 바뀜
					logger.info("Observ order_key:"+order_key+"//"+seller_id);
					for(int j = 0; j < requestModifyBody.getData().get(i).getProduct_variant_key().size(); j++) {
						ids_moim_send.add(requestModifyBody.getData().get(i).getProduct_variant_key().get(j));
					}
					try {
						get_order_single = moimApiService.OrderSingleGet(connect_type, seller_id, ids_moim_send);
						logger.info("Observ get_order_single:"+get_order_single+"//"+seller_id);
						order_moim_get_domain = gson.fromJson(get_order_single, order_moim_get_domain.getClass());
						if(EmptyUtils.isEmpty(order_moim_get_domain)==false) {
							if(EmptyUtils.isEmpty(order_moim_get_domain.getData())==false) {
								for(int j = 0; j < order_moim_get_domain.getData().size(); j++) {
									product_key_list.add(order_moim_get_domain.getData().get(j).getProductId()); 
								}
								
								try {
									error_status = RdsSyncProcess(order_moim_get_domain);
									if(error_status.getError_code()>0) {
										error_code = error_status.getError_code();
										error_val = error_status.getError_val();	
									}
								} catch (Exception e) {
									error_code = 300;
									error_val = "Observ RdsSyncProcess error : "+e;
								}
								
								if(error_code==0) {
									if(product_key_list.size()>0) {
										int iRow = 0;
										Model model_up = new ExtendedModelMap();
										model_up.addAttribute("list_type", 2);
										model_up.addAttribute("data_id_list", ids_moim_send);
										model_up.addAttribute("seller_id", seller_id);
										model_up.addAttribute("order_sub_status", 1);
										iRow = orderService.updateOrderStatus(model_up);
										if(iRow==0) {
											error_code = 119;
											error_val = "orderService.updateOrderStatus 1 1 error ";
											break;
										}
										if(error_code==0) {
											String buyer_phone = "";
											String buyer_name = "";
											String order_number = "";
											String purchaseId = "";
											String productId = "";
											buyer_phone = order_moim_get_domain.getData().get(0).getPurchase().getBuyerPhone();												
											buyer_name = order_moim_get_domain.getData().get(0).getPurchase().getBuyerName();
											order_number = order_moim_get_domain.getData().get(0).getPaymentId();
											purchaseId = order_moim_get_domain.getData().get(0).getPurchaseId();
											productId = order_moim_get_domain.getData().get(0).getProductId();
											
											logger.info("Observ buyer_phone:"+buyer_phone+"//"+seller_id);
											logger.info("Observ buyer_name:"+buyer_name+"//"+seller_id);
											logger.info("Observ order_number:"+order_number+"//"+seller_id);
											logger.info("Observ purchaseId:"+purchaseId+"//"+seller_id);						
											logger.info("Observ productId:"+productId+"//"+seller_id);
											AlarmTalkBuyer(1, connect_type, buyer_name, buyer_phone, order_number
													, seller_id, purchaseId, productId, send_key);
										}
									}else {
										error_code = 119;
										error_val = "product_key_list > size 0 ";
									}
								}
							}
						}
					} catch (Exception e) {
						error_code = 119;
						error_val = "moimApiService.OrderSingleGet error : "+e;
					}
				}
			}
		}else if(requestModifyBody.getModify_status()==11) {
			
			if(requestModifyBody.getModify_key()==1) {// 새로 들어온 주문
				send_key = "1_c_observ";
			}else if(requestModifyBody.getModify_key()==2) {// 가능
				send_key = "2_c_observ";
			}else if(requestModifyBody.getModify_key()==3) {// 관망중 리스트
				send_key = "3_c_observ";
			}
			
			for(int i = 0; i < requestModifyBody.getData().size(); i++) {
				OrderGetDomain.GetData order_moim_get_domain = new OrderGetDomain.GetData();
				List<String> ids_moim_send = new ArrayList<String>();
				List<String> product_key_list = new ArrayList<String>();
				String get_order_single = "";
				String order_key = "";
				
				if(EmptyUtils.isEmpty(requestModifyBody.getData().get(i).getOrder_key())==false) {
					order_key = requestModifyBody.getData().get(i).getOrder_key();
					logger.info("Observ order_key:"+order_key+"//"+seller_id);
					for(int j = 0; j < requestModifyBody.getData().get(i).getProduct_variant_key().size(); j++) {
						ids_moim_send.add(requestModifyBody.getData().get(i).getProduct_variant_key().get(j));
					}
					try {
						get_order_single = moimApiService.OrderSingleGet(connect_type, seller_id, ids_moim_send);
						logger.info("Observ get_order_single:"+get_order_single+"//"+seller_id);
						// 위 옵션ID만 가져옴. 
						order_moim_get_domain = gson.fromJson(get_order_single, order_moim_get_domain.getClass());
						if(EmptyUtils.isEmpty(order_moim_get_domain)==false) {
							if(EmptyUtils.isEmpty(order_moim_get_domain.getData())==false) {
								for(int j = 0; j < order_moim_get_domain.getData().size(); j++) {
									product_key_list.add(order_moim_get_domain.getData().get(j).getProductId()); 
								}
								if(error_code==0) {
									if(product_key_list.size()>0) {
										int iRow = 0;
										Model model_up = new ExtendedModelMap();
										model_up.addAttribute("list_type", 2);
										model_up.addAttribute("data_id_list", ids_moim_send);
										model_up.addAttribute("seller_id", seller_id);
										model_up.addAttribute("order_sub_status", 0);
										iRow = orderService.updateOrderStatus(model_up);
										if(iRow==0) {
											error_code = 119;
											error_val = "orderService.updateOrderStatus 1 1 error ";
											break;
										}
										if(error_code==0) {
											String buyer_phone = "";
											String buyer_name = "";
											String order_number = "";
											String purchaseId = "";
											String productId = "";
											buyer_phone = order_moim_get_domain.getData().get(0).getPurchase().getBuyerPhone();												
											buyer_name = order_moim_get_domain.getData().get(0).getPurchase().getBuyerName();
											order_number = order_moim_get_domain.getData().get(0).getPaymentId();
											purchaseId = order_moim_get_domain.getData().get(0).getPurchaseId();
											productId = order_moim_get_domain.getData().get(0).getProductId();
											
											logger.info("Observ buyer_phone:"+buyer_phone+"//"+seller_id);
											logger.info("Observ buyer_name:"+buyer_name+"//"+seller_id);
											logger.info("Observ order_number:"+order_number+"//"+seller_id);
											logger.info("Observ purchaseId:"+purchaseId+"//"+seller_id);						
											logger.info("Observ productId:"+productId+"//"+seller_id);
											AlarmTalkBuyer(2, connect_type, buyer_name, buyer_phone, order_number
													, seller_id, purchaseId, productId, send_key);
										}
									}else {
										error_code = 119;
										error_val = "product_key_list > size 0 ";
									}
								}
							}
						}
					} catch (Exception e) {
						error_code = 119;
						error_val = "moimApiService.OrderSingleGet error : "+e;
					}
				}
			}
		}
		error_status.setError_code(error_code);
		error_status.setError_val(error_val);
		return error_status;
	}
	

	/**
	 * @desc  
	 */
	public List<GroupedModifyBody> groupByOrderKey(ModifyBodyList request) {
	    Map<String, GroupedModifyBody> groupedMap = new LinkedHashMap<>();

	    for (ModifyBody item : request.getData()) {
	        String orderKey = item.getOrder_key();
	        GroupedModifyBody grouped = groupedMap.computeIfAbsent(orderKey, k -> {
	            GroupedModifyBody g = new GroupedModifyBody();
	            g.setOrder_key(k);
	            g.setOrder_id(k);
	            g.setCourier_id(item.getCourier_id());
	            g.setStatus_val("");
	            g.setDelivery_val("");
	            return g;
	        });

	        if (item.getProduct_variant_key() != null) {
	            grouped.getProduct_variant_key().addAll(item.getProduct_variant_key());
	        }
	    }

	    return new ArrayList<>(groupedMap.values());
	}
	

	/**
	 * @desc  
	 */
	public static boolean containsAny(List<String> a, List<String> b) {
        return b.stream().anyMatch(a::contains);
	}
	

	/**
	 * @desc  
	 */
	public void sendMail(String request_id, String seller_id, String seller_name, String error_val) {
		final String username = defaultConfig.getFailSendGmailAddr();
		final String password = defaultConfig.getFailSendGmailPw(); // 앱 비밀번호
		Properties prop = new Properties();
		prop.put("mail.smtp.host", "smtp.gmail.com");
		prop.put("mail.smtp.port", "587");
		prop.put("mail.smtp.auth", "true");
		prop.put("mail.smtp.starttls.enable", "true");
		prop.put("mail.smtp.connectiontimeout", "5000");
		prop.put("mail.smtp.timeout", "5000");
		
		Session session = Session.getInstance(prop, new Authenticator() {
		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
			return new PasswordAuthentication(username, password);
			}
		});
		try {
			Message message = new MimeMessage(session);
			message.setFrom(new InternetAddress(username, "어부앱 배치 오류", "UTF-8"));
			message.setRecipients(
				Message.RecipientType.TO,
				InternetAddress.parse(defaultConfig.getFailReciverAddr())
			);
			message.setSubject(MimeUtility.encodeText("[배치 실패] "+seller_id+" 가능 변경 처리 오류", "UTF-8", "B"));
			message.setContent(
				seller_name + "배치 실행 중 오류 발생<br>" +
			    "request_id: "+request_id+"<br>"+
				"error : "+error_val+"<br>"+
				"시간: " + java.time.LocalDateTime.now(),
				"text/html; charset=UTF-8"
			);
			Transport.send(message);
			System.out.println("메일 발송 성공");
		} catch (Exception e) {
			System.err.println("메일 발송 실패");
			e.printStackTrace();
		}
	}

	
}

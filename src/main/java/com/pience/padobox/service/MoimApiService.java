package com.pience.padobox.service;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

//import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.google.gson.Gson;
import com.pience.padobox.config.DefaultConfig;
import com.pience.padobox.model.*;
import com.pience.padobox.model.DefaultDomain.*;
import com.pience.padobox.utility.EmptyUtils;

@Service
public class MoimApiService {
	
	private final DefaultConfig defaultConfig;
	
    public MoimApiService(DefaultConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());	
	
	Gson gson = new Gson();

	/**
	 * @desc  seller detail
	 */
	public SetDomain.StatusModifyReturn SellerIdCheck(String connect_type, String seller_id){
		
		SetDomain.StatusModifyReturn return_val = new SetDomain.StatusModifyReturn();
		String RestURL = "";
		String header_auth = "";
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer "+header_auth);
		
		String path = "sellers/"+seller_id;
		
		RestTemplate restTemplate = new RestTemplate();
  		HttpEntity<String> entity = new HttpEntity<String>(headers);
  		
  		try {
  			
  	        logger.info("SellerIdCheck : start: "+seller_id);
  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path);

  			URI targetUrl = uriComponentsBuilder.build().encode().toUri();
  			
  			logger.info("SellerIdCheck targetUrl:"+targetUrl.toString());

  			restTemplate.getMessageConverters()
  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
  			
  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
  			String return_result_val= "";
  			
  			if(results2.getStatusCode().toString().equals("200 OK")==false) {
  				return_val.setError_code(102);
  				return_val.setError_val("MoimApiService SellerIdCheck error : "+results2.getStatusCode().toString()+"//"+results2.toString());
  	      		return_result_val= "";
  			}else {
  				return_val.setError_code(0);
  				return_val.setError_val("");
  				return_result_val= results2.getBody().toString();	
  			}
	  		return_val.setResult_return(return_result_val);
  			logger.info("SellerIdCheck : end: "+seller_id);
      	}catch(Exception e){
      		logger.info("MoimApiService SellerIdCheck try e : "+e+"// RestURL:"+RestURL+"// path:"+path);
      		return_val.setError_code(102);
			return_val.setError_val("try error : "+"MoimApiService SellerIdCheck try e : "+e+"// RestURL:"+RestURL+"// path:"+path);
      		return_val.setResult_return("");
      	}
		return return_val;
	}
	

	/**
	 * @desc ment total cnt  
	 */
	public SetDomain.MainMenuCount MainCntGet(String connect_type, String seller_id){
		
		SetDomain.MainMenuCount return_val = new SetDomain.MainMenuCount(); 
		String RestURL = "";
		String header_auth = "";
		String group_id = "";
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
			group_id = defaultConfig.getGroupIdProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
			group_id = defaultConfig.getGroupIdStage();
		}
		for(int i = 0; i < 5; i++) {
	  		DefaultDomain.OrderListPostBody order_list_post_body = new DefaultDomain.OrderListPostBody();
	  		List<String> status_list = new ArrayList<String>();
	  		if(i==0) {
	  			status_list.add("paid");
//		  		status_list.add("preparingForDelivery");
	  		}else if(i==1) {
	  			status_list.add("preparingForDelivery");
	  			status_list.add("waitingToBePickedUp");
		  		status_list.add("waitingForDeliveryReception");
		  		status_list.add("deliveryReceptionFailed");
	  		}else if(i==2) {
	  			status_list.add("inTransit");
	  			status_list.add("deliveryCompleted");
	  			status_list.add("purchaseCompleted");
	  		}else if(i==3) {
	  			status_list.add("refunded");
	  			status_list.add("cancelled");
	  		}else if(i==4) {//20260306 추가
	  			status_list.add("preparingForDelivery");
	  		}

	  		List<SortSingle> sort_list = new ArrayList<SortSingle>();
	  		SortSingle sort_single = new SortSingle();
	  		CreatedAt create_at = new CreatedAt();
	  		create_at.setOrder("desc");
	  		sort_single.setCreatedAt(create_at);
	  		sort_list.add(sort_single);
	
	  		PaidAt paid_at = new PaidAt();
	  		paid_at.setGte(1L);
	  		
	  		RefundedAt refunded_at = new RefundedAt(); 
	  		refunded_at.setLte(0);
	  		
	  		order_list_post_body.setStatus(status_list);
	  		order_list_post_body.setLimit(1);
	  		order_list_post_body.setSorts(sort_list);
	  		order_list_post_body.setPaidAt(paid_at);	
	  		if(i != 3) {
	  			order_list_post_body.setRefundedAt(refunded_at);
	  		}
	  		logger.info("MainCntGet : body:"+gson.toJson(order_list_post_body)+": "+seller_id);
	  		
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-moim-group-id", group_id);
			headers.set("Authorization", "Bearer "+header_auth);
			
			String path = "/admin/sellers/"+seller_id+"/purchase_items/search";
			RestTemplate restTemplate = new RestTemplate();
	  		HttpEntity<String> entity = new HttpEntity<String>(gson.toJson(order_list_post_body), headers);
	  		
	  		try {
	  			
	  			logger.info("MainCntGet : start: "+seller_id);
	  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path);
	  			URI targetUrl = uriComponentsBuilder.build().encode().toUri();
	  			logger.info("MainCntGet : targetUrl:"+targetUrl.toString());
	
	  			restTemplate.getMessageConverters()
	  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
	  			
	  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
	  			OrderGetDomain.GetData order_get_domain = new OrderGetDomain.GetData();
	  			order_get_domain = gson.fromJson(results2.getBody().toString(), order_get_domain.getClass());
	  			
	  			logger.info("MainCntGet page>total:"+order_get_domain.getPaging().getTotal()+": "+seller_id);
	  			logger.info("MainCntGet end: "+seller_id);
	  			
	  			if(i==0) {
	  				return_val.setNew_order_cnt(order_get_domain.getPaging().getTotal());	
	  			}else if(i==1) {
	  				return_val.setPossible_order_cnt(order_get_domain.getPaging().getTotal());
	  			}else if(i==2) {
	  				return_val.setDelivery_order_cnt(order_get_domain.getPaging().getTotal());
	  			}else if(i==3) {
	  				return_val.setCancel_order_cnt(order_get_domain.getPaging().getTotal());
	  			}else if(i==4) {
	  				return_val.setReceiving_order_cnt(order_get_domain.getPaging().getTotal());
	  			}
	      	}catch(Exception e){
	      		logger.info("MoimApiService MainCntGet try e : "+e+"// RestURL:"+RestURL+"// path:"+path);
	      		return_val = null;
	      	}
		}
		return return_val;
	}
	

	/**
	 * @desc  new list
	 */
	public int NewListCntGet(String connect_type, String seller_id){
		
		int cnt_val = 0;		 

		String RestURL = "";
		String header_auth = "";
		String group_id = "";
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
			group_id = defaultConfig.getGroupIdProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
			group_id = defaultConfig.getGroupIdStage();
		}
		
	  		DefaultDomain.OrderListPostBody order_list_post_body = new DefaultDomain.OrderListPostBody();
	  		List<String> status_list = new ArrayList<String>();
  			status_list.add("paid");
	  		List<SortSingle> sort_list = new ArrayList<SortSingle>();
	  		SortSingle sort_single = new SortSingle();
	  		CreatedAt create_at = new CreatedAt();
	  		create_at.setOrder("desc");
	  		sort_single.setCreatedAt(create_at);
	  		sort_list.add(sort_single);
	
	  		PaidAt paid_at = new PaidAt();
	  		paid_at.setGte(1L);
	  		
	  		RefundedAt refunded_at = new RefundedAt(); 
	  		refunded_at.setLte(0);
	  		
	  		order_list_post_body.setStatus(status_list);
	  		order_list_post_body.setLimit(1);
	  		order_list_post_body.setSorts(sort_list);
	  		order_list_post_body.setPaidAt(paid_at);	
	  		
	  		logger.info("NewListCntGet: body:"+gson.toJson(order_list_post_body)+": "+seller_id);
	  		
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("x-moim-group-id", group_id);
			headers.set("Authorization", "Bearer "+header_auth);
			
			String path = "/admin/sellers/"+seller_id+"/purchase_items/search";
			RestTemplate restTemplate = new RestTemplate();
	  		HttpEntity<String> entity = new HttpEntity<String>(gson.toJson(order_list_post_body), headers);
	  		
	  		try {
	  			
	  	        logger.info("NewListCntGet : start: "+seller_id);
	  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path);
	  			URI targetUrl = uriComponentsBuilder.build().encode().toUri();
	  			logger.info("NewListCntGet : targetUrl:"+targetUrl.toString()+": "+seller_id);
	
	  			restTemplate.getMessageConverters()
	  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
	  			
	  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
	  			OrderGetDomain.GetData order_get_domain = new OrderGetDomain.GetData();
	  			order_get_domain = gson.fromJson(results2.getBody().toString(), order_get_domain.getClass());
	  			
	  			logger.info("NewListCntGet : page>total:"+order_get_domain.getPaging().getTotal()+": "+seller_id);
	  			logger.info("NewListCntGet : end: "+seller_id);
	  			cnt_val = order_get_domain.getPaging().getTotal();
	  			
	      	}catch(Exception e){
	      		logger.info("MoimApiService NewListCntGet try e : "+e+"// RestURL:"+RestURL+"// path:"+path);
	      		cnt_val = 0;
	      	}
		return cnt_val;
	}
	

	/**
	 * @desc  order list 
	 */
	public String OrderListGet(String order_type, String connect_type, String seller_id, String after, Long gte, Long lte){
		String return_val = "";
		String RestURL = "";
		String header_auth = "";
		String group_id = "";
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
			group_id = defaultConfig.getGroupIdProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
			group_id = defaultConfig.getGroupIdStage();
		}

		DefaultDomain.OrderListPostBody order_list_post_body = new DefaultDomain.OrderListPostBody();
  		
  		List<String> status_list = new ArrayList<String>();
  		if(order_type.equals("requested")==true) {
  			status_list.add("paid");
//  	  		status_list.add("preparingForDelivery");
  		}else if(order_type.equals("receiveingDilivery")==true) {
//  			status_list.add("preparingForDelivery");
  		}else if(order_type.equals("processing")==true) {
  			status_list.add("preparingForDelivery");
  			status_list.add("waitingToBePickedUp");
  	  		status_list.add("waitingForDeliveryReception");
  	  		status_list.add("deliveryReceptionFailed");
  		}else if(order_type.equals("delivered")==true) {
  			status_list.add("inTransit");
  	  		status_list.add("deliveryCompleted");
  	  		status_list.add("purchaseCompleted");
  		}else if(order_type.equals("cancelled")==true) {
  			status_list.add("refunded");
  	  		status_list.add("cancelled");
  		}

  		List<SortSingle> sort_list = new ArrayList<SortSingle>();
  		SortSingle sort_single = new SortSingle();
  		CreatedAt create_at = new CreatedAt();
  		
  		if(order_type.equals("delivered")==true || order_type.equals("cancelled")==true) {
  			create_at.setOrder("desc");
  		}else {
  			create_at.setOrder("asc");
  		}
  		
  		sort_single.setCreatedAt(create_at);
  		sort_list.add(sort_single);

  		PaidAt paid_at = new PaidAt();
  		if(gte>0) {
  			paid_at.setGte(gte);
  			paid_at.setLte(lte);
  		}else {
  			paid_at.setGte(1L);
  		}
  		
		RefundedAt refunded_at = new RefundedAt(); 
  		refunded_at.setLte(0);	
  		
  		order_list_post_body.setStatus(status_list);
  		order_list_post_body.setLimit(defaultConfig.getMoimApiOrderListLimit()); 
  		order_list_post_body.setSorts(sort_list);
		order_list_post_body.setPaidAt(paid_at);	 
  		if(order_type.equals("cancelled")==false) {
  			order_list_post_body.setRefundedAt(refunded_at);
  		}
  		
  		if(EmptyUtils.isEmpty(after)==false) {
  			order_list_post_body.setAfter(after);
  		}
  		
  		logger.info("OrderListGet : body:"+gson.toJson(order_list_post_body)+": "+seller_id);
  		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("x-moim-group-id", group_id);
		headers.set("Authorization", "Bearer "+header_auth);
		
		String path = "/admin/sellers/"+seller_id+"/purchase_items/search";
		
		RestTemplate restTemplate = new RestTemplate();
  		HttpEntity<String> entity = new HttpEntity<String>(gson.toJson(order_list_post_body), headers);
  		
  		try {
  			
  	        logger.info("OrderListGet start : purchase_items/search: "+seller_id);
  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path);
  			URI targetUrl = uriComponentsBuilder.build().encode().toUri();
  			logger.info("OrderListGet targetUrl:"+targetUrl.toString()+": "+seller_id);

  			restTemplate.getMessageConverters()
  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
  			
  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
  			logger.info("OrderListGet end: purchase_items/search" +seller_id);
  			
  			return_val = results2.getBody().toString();
      	}catch(Exception e){
      		logger.info("MoimApiService OrderListGet try e : "+e+"// RestURL:"+RestURL+"// path:"+path);
      		return_val = "OrderListGet error:"+e+"// RestURL:"+RestURL+"// path:"+path;
      	}
		return return_val;
	}
	

	/**
	 * @desc  data sync
	 */
	public String SyncIdDataGet(String connect_type, String seller_id, String after){
		String return_val = "";
		String RestURL = "";
		String header_auth = "";
		String group_id = "";
		
		logger.info("SyncIdDataGet : connect_type:"+connect_type);
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
			group_id = defaultConfig.getGroupIdProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
			group_id = defaultConfig.getGroupIdStage();
		}

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("x-moim-group-id", group_id);
		headers.set("Authorization", "Bearer "+header_auth);
		
		String path = "/admin/sellers/"+seller_id+"/purchase_items/updated";
		
		logger.info("SyncIdDataGet RestURL"+RestURL);
		logger.info("SyncIdDataGet path:"+path);
		logger.info("SyncIdDataGet header_auth:"+header_auth);
		logger.info("SyncIdDataGet group_id:"+group_id);
		
		RestTemplate restTemplate = new RestTemplate();
  		HttpEntity<String> entity = new HttpEntity<String>(headers);
  		
  		try {
  			
  			logger.info("SyncIdDataGet start");
  	        URI targetUrl = new URI("");
  	        if(EmptyUtils.isEmpty(after)==false) {
  	  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path)
  	  					.queryParam("limit", defaultConfig.getMoimApiOrderListLimit()).queryParam("after", after);
  	  			targetUrl = uriComponentsBuilder.build().encode().toUri();
  	        }else {
  	        	UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path)
  	  					.queryParam("limit", defaultConfig.getMoimApiOrderListLimit());
  	  			targetUrl = uriComponentsBuilder.build().encode().toUri();
  	        }
  	        
  			logger.info("SyncIdDataGet targetUrl:"+targetUrl.toString());

  			restTemplate.getMessageConverters()
  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
  			
  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
  			logger.info("SyncIdDataGet end");
  			
  			return_val = results2.getBody().toString();
      	}catch(Exception e){
      		logger.info("MoimApiService SyncIdDataGet try e : "+e+"// RestURL:"+RestURL+"// path:"+path);
      		return_val = "";
      	}
		
		return return_val;
	}
	

	/**
	 * @desc  order single 
	 */
	public String OrderSingleGet(String connect_type
			, String seller_id , List<String> ids){
		
		String return_val = ""; 
		String RestURL = "";
		String header_auth = "";
		String group_id = "";
		String top_seller_id = "";
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
			group_id = defaultConfig.getGroupIdProd();
			top_seller_id = defaultConfig.getTopSelleridProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
			group_id = defaultConfig.getGroupIdStage();
			top_seller_id = defaultConfig.getTopSelleridStage();
		}
  		DefaultDomain.OrderSinglePostBody order_single_post_body = new DefaultDomain.OrderSinglePostBody();
  		order_single_post_body.setIds(ids);
  		logger.info(" OrderSingleGet body:"+gson.toJson(order_single_post_body)+": "+seller_id);
  		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("x-moim-group-id", group_id);
		headers.set("Authorization", "Bearer "+header_auth);
		
		String path = "/admin/sellers/"+top_seller_id+"/purchase_items/_batch";
		RestTemplate restTemplate = new RestTemplate();
  		HttpEntity<String> entity = new HttpEntity<String>(gson.toJson(order_single_post_body), headers);
  		
  		for(int j = 0; j < 10; j++) {
  			try {
  	  	        logger.info("OrderSingleGet start: "+seller_id);
  	  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path);
  	  			URI targetUrl = uriComponentsBuilder.build().encode().toUri();
  	  			logger.info("OrderSingleGet targetUrl:"+targetUrl.toString()+": "+seller_id);

  	  			restTemplate.getMessageConverters()
  	  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
  	  			
  	  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
  	  			return_val= results2.getBody().toString();
  	  			logger.info("OrderSingleGet end: "+seller_id);
  	  			if(results2.getStatusCode().toString().equals("200 OK")) {
  	  				break;	
  	  			}
  	      	}catch(Exception e){
  	      		logger.info("MoimApiService OrderSingleGet try e : "+e+"// RestURL:"+RestURL+"// path:"+path);
  	      		return_val = null;
  	      	}
  		}
		return return_val;
	}
	

	/**
	 * @desc  seller delivery list
	 */
	public SellerIdDeliveryDomain SellerInfoDeliveryListGet(String connect_type, String seller_id){
		SellerIdDeliveryDomain delivery_list = new SellerIdDeliveryDomain();
		String RestURL = "";
		String header_auth = "";
		String group_id = "";
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
			group_id = defaultConfig.getGroupIdProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
			group_id = defaultConfig.getGroupIdStage();
		}
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("x-moim-group-id", group_id);
		headers.set("Authorization", "Bearer "+header_auth);
		
		String path = "/admin/sellers/"+seller_id+"/delivery_contracts";
		
		RestTemplate restTemplate = new RestTemplate();
  		HttpEntity<String> entity = new HttpEntity<String>(headers);
  		
  		try {
  			
  			logger.info("SellerInfoDeliveryListGet start: "+seller_id);
  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path);
  			URI targetUrl = uriComponentsBuilder.build().encode().toUri();
  			logger.info("SellerInfoDeliveryListGet targetUrl:"+targetUrl.toString()+": "+seller_id);

  			restTemplate.getMessageConverters()
  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
  			
  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
  			logger.info("SellerInfoDeliveryListGet end: "+seller_id);
  			if(EmptyUtils.isEmpty(results2.getBody().toString())==false) {
  				delivery_list = gson.fromJson(results2.getBody().toString(), delivery_list.getClass());
  				logger.info("delivery_list : "+results2.getBody().toString()+": "+seller_id);
  				if(EmptyUtils.isEmpty(delivery_list.getData())==true) {
  	  				delivery_list = null;
  	  			}
  			}else {
  				delivery_list = null;
  			}
      	}catch(Exception e){
      		logger.info("MoimApiService SellerInfoDeliveryListGet try e : "+e+"// RestURL:"+RestURL+"// path:"+path);
      		delivery_list = null;
      	}
		return delivery_list;
	}
	

	/**
	 * @desc  order status preparing_for_delivery
	 */
	public SetDomain.StatusModifyReturn OrderStatusModifyPreparingFroDelivery(String connect_type
			, String seller_id, List<String> ids){
		SetDomain.StatusModifyReturn  return_val = new SetDomain.StatusModifyReturn();
		int error_code = 0;
		String error_val = "";
		String RestURL = "";
		String header_auth = "";
		String group_id = "";
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
			group_id = defaultConfig.getGroupIdProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
			group_id = defaultConfig.getGroupIdStage();
		}
		
  		DefaultDomain.OrderSinglePostBody order_single_post_body = new DefaultDomain.OrderSinglePostBody();
  		order_single_post_body.setIds(ids);
  		logger.info("OrderStatusModifyPreparingFroDelivery:body: "+gson.toJson(order_single_post_body)+": "+seller_id);
  		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("x-moim-group-id", group_id);
		headers.set("Authorization", "Bearer "+header_auth);
		
		String path = "/admin/sellers/"+seller_id+"/purchases/preparing_for_delivery";
		RestTemplate restTemplate = new RestTemplate();
  		HttpEntity<String> entity = new HttpEntity<String>(gson.toJson(order_single_post_body), headers);
  		
  		try {
  			
  	        logger.info("OrderStatusModifyPreparingFroDelivery start: "+seller_id);
  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path);
  			URI targetUrl = uriComponentsBuilder.build().encode().toUri();
  			logger.info("OrderStatusModifyPreparingFroDelivery targetUrl:"+targetUrl.toString()+": "+seller_id);

  			restTemplate.getMessageConverters()
  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
  			
  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
  			logger.info("OrderStatusModifyPreparingFroDelivery results2.toString() : "+results2.toString()+": "+seller_id);
			logger.info("OrderStatusModifyPreparingFroDelivery results2 getStatusCode : "+results2.getStatusCode()+": "+seller_id);// results2 getStatusCode : 200 OK 
  			
  			String return_result_val= "";
  			
  			if(results2.getStatusCode().toString().equals("200 OK")==false) {
  				error_code = 113;
  	      		error_val = "MoimApiService OrderStatusModifyPreparingFroDelivery error : "+results2.getStatusCode().toString()+"//"+results2.toString();
  	      		
  	      		logger.info("OrderStatusModifyPreparingFroDelivery /purchases/preparing_for_delivery results2 : "+results2.toString());
  	      		return_result_val= "";
  			}else {
  				return_result_val= results2.getBody().toString();	
  			}
	  		
	  		return_val.setResult_return(return_result_val);
	  		logger.info("OrderStatusModifyPreparingFroDelivery end: "+seller_id);
      	}catch(Exception e){
      		logger.info("MoimApiService OrderStatusModifyPreparingFroDelivery try e : "+e+"// RestURL:"+RestURL+"// path:"+path);
      		error_code = 111;
      		error_val = "";
      	}
		
		return_val.setError_code(error_code);
		return_val.setError_val(error_val);
		
		return return_val;
	}


	/**
	 * @desc order delivery reception
	 */
	public SetDomain.StatusModifyReturn OrderDeliveryReception(String connect_type
			, String seller_id, String deliveries_body){
		
		SetDomain.StatusModifyReturn  return_val = new SetDomain.StatusModifyReturn();
		int error_code = 0;
		String error_val = "";
		String RestURL = "";
		String header_auth = "";
		String group_id = "";
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
			group_id = defaultConfig.getGroupIdProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
			group_id = defaultConfig.getGroupIdStage();
		}
  		
  		JSONParser parser = new JSONParser();
  		Object obj = new Object();
  		JSONObject jsonObj = new JSONObject();
		try {
			obj = parser.parse(deliveries_body);
			jsonObj = (JSONObject) obj;
			logger.info("OrderDeliveryReception->send body:"+gson.toJson(jsonObj)+": "+seller_id);
		} catch (ParseException e) {
			error_code = 111;
			error_val = "OrderDeliveryReception JSONParser error : "+e;
		}
	  		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("x-moim-group-id", group_id);
		headers.set("Authorization", "Bearer "+header_auth);
		
		String path = "/admin/sellers/"+seller_id+"/purchases/delivery_reception";
		RestTemplate restTemplate = new RestTemplate();
  		HttpEntity<String> entity = new HttpEntity<String>(gson.toJson(jsonObj), headers);
  		
  		try {
  			
  	        logger.info("OrderDeliveryReception start: "+seller_id);
  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path);
  			URI targetUrl = uriComponentsBuilder.build().encode().toUri(); 
  			logger.info("OrderDeliveryReception targetUrl:"+targetUrl.toString()+": "+seller_id);

  			restTemplate.getMessageConverters()
  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
  			
  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
  			
  			String return_result_val= "";
  			if(results2.getStatusCode().toString().equals("200 OK")==false) {
  				error_code = 113;
  	      		error_val = "MoimApiService OrderDeliveryReception error : "+results2.getStatusCode().toString()+"//"+results2.toString()+": "+seller_id;
  	      		
  	      		logger.info("OrderDeliveryReception purchases/delivery_reception results2 : "+results2.toString()+": "+seller_id);
  	      		
  	      		return_result_val= "";
  			}else {
  				return_result_val= results2.getBody().toString();	
  			}
  			
	  		return_val.setResult_return(return_result_val);
	  		logger.info("OrderDeliveryReception end: "+seller_id);
	  		
      	}catch(Exception e){
      		logger.info("MoimApiService OrderDeliveryReception try e : "+e+"// RestURL:"+RestURL+"// path:"+path);
      		error_code = 113;
      		error_val = "MoimApiService OrderDeliveryReception try e : "+e;
      	}
		
		return_val.setError_code(error_code);
		return_val.setError_val(error_val);
		
		return return_val;
	}
	

	/**
	 * @desc  account list
	 */
	public SetDomain.StatusModifyReturn AccountsListGet(String connect_type, String seller_id, String account_body){
		
		SetDomain.StatusModifyReturn  return_val = new SetDomain.StatusModifyReturn();
		int error_code = 0;
		String error_val = "";
		String RestURL = "";
		String header_auth = "";
		String group_id = "";
		String top_seller_id = "";
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
			group_id = defaultConfig.getGroupIdProd();
			top_seller_id = defaultConfig.getTopSelleridProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
			group_id = defaultConfig.getGroupIdStage();
			top_seller_id = defaultConfig.getTopSelleridStage();
		}
		
  		JSONParser parser = new JSONParser();
  		Object obj = new Object();
  		JSONObject jsonObj = new JSONObject();
		try {
			obj = parser.parse(account_body);
			jsonObj = (JSONObject) obj;
			logger.info("AccountsListGet send body:"+gson.toJson(jsonObj)+": "+seller_id);
		} catch (ParseException e) {
			error_code = 111;
			error_val = "JSONParser error : "+e;
		}
	  		
  		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("x-moim-group-id", group_id);
		headers.set("Authorization", "Bearer "+header_auth);
		
		String path = "/admin/sellers/"+top_seller_id+"/settlements/search";
		
		RestTemplate restTemplate = new RestTemplate();
  		HttpEntity<String> entity = new HttpEntity<String>(gson.toJson(jsonObj), headers);
  		
  		try {
  			
  	        logger.info("AccountsListGet start"+": "+seller_id);
  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path);
  			URI targetUrl = uriComponentsBuilder.build().encode().toUri(); 
  			logger.info("AccountsListGet targetUrl:"+targetUrl.toString()+": "+seller_id);

  			restTemplate.getMessageConverters()
  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
  			
  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
  			String return_result_val= results2.getBody().toString();
	  		return_val.setResult_return(return_result_val);
  			
	  		logger.info("AccountsListGet end"+": "+seller_id);
      	}catch(Exception e){
      		logger.info("MoimApiService AccountsListGet try e : "+e+"// RestURL:"+RestURL+"// path:"+path+": "+seller_id);
      		error_code = 111;
      		error_val = "";
      	}
		
		return_val.setError_code(error_code);
		return_val.setError_val(error_val);
		
		return return_val;
		
	}
	
	/**
	 * @desc  account single
	 */
	public SetDomain.StatusModifyReturn AccountsSingleGet(String connect_type, String after
			, String account_id){
		
		SetDomain.StatusModifyReturn  return_val = new SetDomain.StatusModifyReturn();
		int error_code = 0;
		String error_val = "";
		String RestURL = "";
		String header_auth = "";
		String group_id = "";
		String top_seller_id = "";
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
			group_id = defaultConfig.getGroupIdProd();
			top_seller_id = defaultConfig.getTopSelleridProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
			group_id = defaultConfig.getGroupIdStage();
			top_seller_id = defaultConfig.getTopSelleridStage();
		}
  		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("x-moim-group-id", group_id);
		headers.set("Authorization", "Bearer "+header_auth);
		
		String path = "/admin/sellers/"+top_seller_id+"/settlements/"+account_id+"/settlement_items";

		RestTemplate restTemplate = new RestTemplate();
  		HttpEntity<String> entity = new HttpEntity<String>(headers);
  		
  		try {
  			
  	        logger.info("AccountsSingleGet start"+": "+account_id);
  	        URI targetUrl = new URI("");
  			 
  			 if(EmptyUtils.isEmpty(after)==false) {
	  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path)
	  					.queryParam("after", after);
	  			targetUrl = uriComponentsBuilder.build().encode().toUri();
	        }else {
	        	UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path);
	  			targetUrl = uriComponentsBuilder.build().encode().toUri();
	        }
  	        
  			logger.info("AccountsSingleGet targetUrl:"+targetUrl.toString()+": "+account_id);

  			restTemplate.getMessageConverters()
  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
  			
  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
  			String return_result_val= results2.getBody().toString();
	  		return_val.setResult_return(return_result_val);
	  		logger.info("AccountsSingleGet end"+": "+account_id);
      	}catch(Exception e){
      		logger.info("MoimApiService AccountsSingleGet try e : "+e+"// RestURL:"+RestURL+"// path:"+path+": "+account_id);
      		error_code = 111;
      		error_val = "";
      	}
		
		return_val.setError_code(error_code);
		return_val.setError_val(error_val);
		
		return return_val;
	}
	
	/**
	 * @desc kakao alarm talk   
	 */
	public String AlarmTalkPost(List<DefaultDomain.KakaoAlramTalkPostBody> post_body){
		String return_val = "";
		String RestURL = defaultConfig.getKakaoRestUrl();
		String connect_userid = defaultConfig.getKakaoUserid();
		String profile_key = defaultConfig.getKakaoProfileKey();

  		logger.info("AlarmTalkPost body:"+gson.toJson(post_body));
  		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("userid", connect_userid);
		
		String path = "/v2/"+profile_key+"/sendMessage";
		
		RestTemplate restTemplate = new RestTemplate();
  		HttpEntity<String> entity = new HttpEntity<String>(gson.toJson(post_body), headers);
  		
  		if(defaultConfig.getKakaoSendYn().equals("on")==true) {
  	  		try {
  	  			
  	  	        logger.info("AlarmTalkPost start");
  	  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path);
  	  			URI targetUrl = uriComponentsBuilder.build().encode().toUri();
  	  			logger.info("AlarmTalkPost targetUrl:"+targetUrl.toString());

  	  			restTemplate.getMessageConverters()
  	  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
  	  			
  	  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);
  	  			return_val = results2.getBody().toString();
  	  			logger.info("AlarmTalkPost end");
  	  			
  	      	}catch(Exception e){
  	      		logger.info("MoimApiService AlarmTalkPost try e : "+e+"// RestURL:"+RestURL+"// path:"+path+"// post_body:"+gson.toJson(post_body));
  	      		return_val = null;
  	      	}
  		}else {
  			return_val = null;
  		}
		
		return return_val;
	}
	
	/**
	 * @desc  seller list
	 */
	public SellerIdListDomain GetAllSellerIdList(String connect_type, String after){
		
		SellerIdListDomain sellerList = new SellerIdListDomain();

		String RestURL = "";
		String header_auth = "";
		String group_id = "";
		String padobox_parentId = "";
		
		if(connect_type.equals("prod")==true) {
			RestURL = defaultConfig.getMoimapiUrlProd();
			header_auth = defaultConfig.getMoimapiAuthProd();
			group_id = defaultConfig.getGroupIdProd();
			padobox_parentId = defaultConfig.getTopSelleridProd();
		}else {
			RestURL = defaultConfig.getMoimapiUrlStage();;
			header_auth = defaultConfig.getMoimapiAuthStage();
			group_id = defaultConfig.getGroupIdStage();
			padobox_parentId = defaultConfig.getTopSelleridStage();
		}
		
		logger.info("GetAllSellerIdList group_id:"+group_id);
		logger.info("GetAllSellerIdList padobox_parentId:"+padobox_parentId);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("Authorization", "Bearer "+header_auth);
		
		String path = "/sellers/"+padobox_parentId+"/sub_sellers";
		RestTemplate restTemplate = new RestTemplate();
  		HttpEntity<String> entity = new HttpEntity<String>(headers);
  		
  		try {
  			
  	        logger.info("GetAllSellerIdList sub_sellers start");
  			URI targetUrl = new URI("");
  	        
  	        if(EmptyUtils.isEmpty(after)==false) {
  	  			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path)
  	  					.queryParam("limit", defaultConfig.getMoimApiSelleridListLimit()).queryParam("after", after);
  	  			targetUrl = uriComponentsBuilder.build().encode().toUri();
  	        }else {
  	        	UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.fromUriString(RestURL).path(path)
  	  					.queryParam("limit", defaultConfig.getMoimApiSelleridListLimit());
  	  			targetUrl = uriComponentsBuilder.build().encode().toUri();
  	        }

  			logger.info("GetAllSellerIdList targetUrl:"+targetUrl.toString());

  			restTemplate.getMessageConverters()
  			.add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));
  			
  			ResponseEntity<String> results2 = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);
  			JSONParser jsonParser = new JSONParser(); 
  			JSONObject jsonObject = (JSONObject) jsonParser.parse(results2.getBody().toString());
  			logger.info("GetAllSellerIdList results2 : "+jsonObject);
  			
  			logger.info("GetAllSellerIdList sub_sellers end");
  			sellerList = gson.fromJson(results2.getBody().toString(), sellerList.getClass());  	
  			
  			if(EmptyUtils.isEmpty(sellerList.getData())==true) {
  				sellerList = null;
  			}
      	}catch(Exception e){
      		logger.info("MoimApiService GetAllSellerIdList try e : "+e+"// RestURL:"+RestURL+"// path:"+path);
      		sellerList = null;
      	}
		return sellerList;
	}
	
}

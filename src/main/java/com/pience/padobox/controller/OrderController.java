package com.pience.padobox.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pience.padobox.config.DefaultConfig;
import com.pience.padobox.model.*;
import com.pience.padobox.model.SetDomain.CourierComList;
import com.pience.padobox.model.SetDomain.ReturnModifyResultV2;
import com.pience.padobox.service.AsyncStatusModifyService;
import com.pience.padobox.service.BatchStatusService;
import com.pience.padobox.service.ConnectService;
import com.pience.padobox.service.OrderConnectService;
import com.pience.padobox.utility.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class OrderController {
	
	private final DefaultConfig defaultConfig;
	
    public OrderController(DefaultConfig defaultConfig) {
        this.defaultConfig = defaultConfig;
    }
	
	@Autowired
	OrderConnectService orderConnectService;
	
	@Autowired
	ConnectService connectService;
	
	@Autowired
	BatchStatusService batchStatusService;
	
	@Autowired
	AsyncStatusModifyService asyncStatusModifyService;
	
	@Autowired
	private JwtUtil jwtUtil;
	
	Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());

	
	/**
	 * @desc 새로 들어온 주문 리스트
	 */
	@GetMapping(value="/new-order-list/{version}", headers="Accept=application/json;charset=UTF-8", produces="application/json;charset=UTF-8" )
	public @ResponseBody Json NewOrderList(
			HttpServletRequest httpServletRequest
			, @RequestHeader(value="X-Pado-REST-API-Key") String rest_api_key
    		, @RequestHeader(value="X-Pado-Session-Token") String rest_session_token
    		, @PathVariable("version") String rest_version
    		, @RequestParam Map<String, String> paramMap
    		, Model model
			) {

		int error_code = 0;
		String error_val = "";
		String path_val = "/new-order-list/"+rest_version;
		String connect_type = defaultConfig.getConnectType();
		String json_result = null;
		String token_type = "";
		String authorizationHeader = httpServletRequest.getHeader("Authorization");
		String seller_token = "";
		String secret = "";
		String seller_id = "";
		
		JsonResult<Object> jsonResult = new JsonResult<Object>();
		JsonResult<Object> jsonResultError = new JsonResult<Object>();
		
		logger.info("NewOrderList connect_type:"+connect_type);
		logger.info("NewOrderList rest_version:"+rest_version);
		
		OrderDomain order_list = new OrderDomain();
		String order_type = "requested"; // requested, processing,  delivered, cancelled
		
		//=================================================================
		// connect check
		if(connect_type.equals("local")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("dev")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("prod")==true) {
			token_type = defaultConfig.getHeaderSessionTokenProd();
			secret = defaultConfig.getPadoboxJwtSecretProd();
		}
		
		if(error_code == 0) {
			if(rest_api_key.equals(defaultConfig.getHeaderRestApi())==false 
					|| rest_session_token.equals(token_type)==false){
				error_code = 100;
				error_val = "Invalid authentication! : "+rest_api_key+"//"+rest_session_token;
			}
		}
		
		if(error_code == 0) {
			if(EmptyUtils.isEmpty(authorizationHeader)==false 
					&& authorizationHeader.startsWith("Bearer ")) {
				seller_token = authorizationHeader.substring(7);
				logger.info("NewOrderList seller_token:"+seller_token);
			}else {
				error_code = 100;
				error_val = "Invalid authentication!!:"+authorizationHeader;
			}
		}
		
		//=================================================================
		// seller info check
		SellerIdInfoDomain sellerid_info = new SellerIdInfoDomain();
		if(error_code == 0) {

			try {
				seller_id = jwtUtil.paserToken(seller_token, secret);
				logger.info("NewOrderList seller_id"+seller_id);
	
				if(EmptyUtils.isEmpty(seller_id)==true) {
					error_code = 100;
					error_val = " seller_id:"+seller_id+"// jwtUtil.paserToken error ";
				}else {
					sellerid_info =  orderConnectService.SellerIdCheck(model, connect_type, seller_id);
					if(sellerid_info.getError_code()>0) {
						error_code = 102;
						error_val = "seller_token:"+seller_token+" // "+sellerid_info.getError_val();
					}else {
						sellerid_info.setError_code(0);
						sellerid_info.setError_val("");
					}
				}
			} catch (Exception e) {
				error_code = 100;
				error_val = "Unauthenticated user!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//=================================================================
		// check param 
		OrderBodyDomain.ParamBody param_body = new OrderBodyDomain.ParamBody();

		//=================================================================
		// list 
		if(error_code==0) {
			try {
				logger.info("NewOrderList rest_version:"+rest_version);
//				if(rest_version.equals("1")==true) {
//					order_list = orderConnectService.getOrderListNewOrder(model, order_type, connect_type, seller_id, param_body);
//				}
				if(rest_version.equals("2")==true) {
					order_list = orderConnectService.getOrderListNewOrderV2(model, order_type, connect_type, seller_id, param_body);
				}
				
				if(order_list.getError_code()>0) {
					error_code = order_list.getError_code();
					error_val = order_list.getError_val();
				}else {
					order_list.setError_code(null);
					order_list.setError_val(null);
				}
				
			} catch (Exception e) {
				error_code = 101;
				error_val = "getOrderList error!! seller_id:"+seller_id+"// e:"+e;
			}
		}
		
		//============================================================================= error logic 
		
		if(error_code != 0){
			
			logger.info("NewOrderList error_code:"+error_code);
			logger.info("NewOrderList error_val:"+error_val);
			logger.info("NewOrderList path_val:"+path_val+"//seller_id:"+seller_id);
			ErrorReturnDomain errorReturnDomain = new ErrorReturnDomain();
			errorReturnDomain =  connectService.ErrorCheck(connect_type
					, error_code
					, path_val+"//seller_id:"+seller_id+" //error_val "+error_val
					, seller_id
					, "get"); 
			jsonResultError.setRes_code(errorReturnDomain.getError_code());
			jsonResultError.setRes_value(errorReturnDomain.getError_value());
			jsonResultError.setResult(new Object());
			
		}else{
			jsonResult.setRes_code(0);
			jsonResult.setRes_value("");
		}
		
		//=============================================================================	error logic end

		if(error_code != 0){
			json_result = new Gson().toJson(jsonResultError);	
		}else{
			jsonResult.setResult(order_list); 
			json_result = new Gson().toJson(jsonResult);
		}
		
		return new Json(json_result); 
	}
	
	/**
	 * @desc 배송 접수 중 리스트 : 현재 사용 안함. 
	 */
	@GetMapping(value="/receiving-delivery-order-list/{version}", headers="Accept=application/json;charset=UTF-8", produces="application/json;charset=UTF-8" )
	public @ResponseBody Json ReceiveingDiliveryList(
			HttpServletRequest httpServletRequest
			, @RequestHeader(value="X-Pado-REST-API-Key") String rest_api_key
    		, @RequestHeader(value="X-Pado-Session-Token") String rest_session_token
    		, @PathVariable("version") String rest_version
    		, @RequestParam Map<String, String> paramMap
    		, Model model
			) {

		int error_code = 0;
		String error_val = "";
		String path_val = "/receiving-delivery-order-list/"+rest_version;
		String connect_type = defaultConfig.getConnectType();
		String json_result = null;
		String token_type = "";
		String authorizationHeader = httpServletRequest.getHeader("Authorization");
		String seller_token = "";
		String secret = "";
		String seller_id = "";
		
		JsonResult<Object> jsonResult = new JsonResult<Object>();
		JsonResult<Object> jsonResultError = new JsonResult<Object>();
		
		logger.info("ReceiveingDiliveryList connect_type:"+connect_type);
		logger.info("ReceiveingDiliveryList rest_version:"+rest_version);
		
		OrderDomain order_list = new OrderDomain();
		String order_type = "receiveingDilivery";
		// requested, processing,  delivered, cancelled
		
		//=================================================================
		// connect check
		if(connect_type.equals("local")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("dev")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("prod")==true) {
			token_type = defaultConfig.getHeaderSessionTokenProd();
			secret = defaultConfig.getPadoboxJwtSecretProd();
		}
		
		if(error_code == 0) {
			if(rest_api_key.equals(defaultConfig.getHeaderRestApi())==false 
					|| rest_session_token.equals(token_type)==false){
				error_code = 100;
				error_val = "Invalid authentication! : "+rest_api_key+"//"+rest_session_token;
			}
		}
		
		if(error_code == 0) {
			if(EmptyUtils.isEmpty(authorizationHeader)==false 
					&& authorizationHeader.startsWith("Bearer ")) {
				seller_token = authorizationHeader.substring(7);
				logger.info("ReceiveingDiliveryList seller_token:"+seller_token);
			}else {
				error_code = 100;
				error_val = "Invalid authentication!!:"+authorizationHeader;
			}
		}
		
		//=================================================================
		// seller info check
		SellerIdInfoDomain sellerid_info = new SellerIdInfoDomain();
		if(error_code == 0) {

			try {
				seller_id = jwtUtil.paserToken(seller_token, secret);
				logger.info("ReceiveingDiliveryList seller_id"+seller_id);
	
				if(EmptyUtils.isEmpty(seller_id)==true) {
					error_code = 100;
					error_val = " seller_id:"+seller_id+"// jwtUtil.paserToken error ";
				}else {
					sellerid_info =  orderConnectService.SellerIdCheck(model, connect_type, seller_id);
					if(sellerid_info.getError_code()>0) {
						error_code = 102;
						error_val = "seller_token:"+seller_token+" // "+sellerid_info.getError_val();
					}else {
						sellerid_info.setError_code(0);
						sellerid_info.setError_val("");
					}
				}
			} catch (Exception e) {
				error_code = 100;
				error_val = "Unauthenticated user!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//=================================================================
		// check param 
		OrderBodyDomain.ParamBody param_body = new OrderBodyDomain.ParamBody();

		//=================================================================
		// list 
		if(error_code==0) {
			try {
								
				order_list = orderConnectService.getOrderListReceiveingDiliveryListV2(model, order_type, connect_type, seller_id, param_body);
				
				if(order_list.getError_code()>0) {
					error_code = order_list.getError_code();
					error_val = order_list.getError_val();
				}else {
					order_list.setError_code(null);
					order_list.setError_val(null);
				}
				
			} catch (Exception e) {
				error_code = 101;
				error_val = "getOrderList error!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//============================================================================= error logic 
		
		if(error_code != 0){
			
			logger.info("ReceiveingDiliveryList error_code:"+error_code);
			logger.info("ReceiveingDiliveryList error_val:"+error_val);
			logger.info("ReceiveingDiliveryList path_val:"+path_val+"//seller_token:"+seller_token);
			ErrorReturnDomain errorReturnDomain = new ErrorReturnDomain();
			errorReturnDomain =  connectService.ErrorCheck(connect_type
					, error_code
					, path_val+"//seller_token:"+seller_token+" //error_val "+error_val
					, seller_id
					, "get"); 
			jsonResultError.setRes_code(errorReturnDomain.getError_code());
			jsonResultError.setRes_value(errorReturnDomain.getError_value());
			jsonResultError.setResult(new Object());
			
		}else{
			jsonResult.setRes_code(0);
			jsonResult.setRes_value("");
		}
		
		//=============================================================================	error logic end

		if(error_code != 0){
			json_result = new Gson().toJson(jsonResultError);	
		}else{
			jsonResult.setResult(order_list); 
			json_result = new Gson().toJson(jsonResult);
		}
		
		return new Json(json_result); 
	}
	
	/**
	 * @desc 가능 주문 리스트
	 */
	@GetMapping(value="/possible-order-list/{version}", headers="Accept=application/json;charset=UTF-8", produces="application/json;charset=UTF-8" )
	public @ResponseBody Json PossibleOrderList(
			HttpServletRequest httpServletRequest
			, @RequestHeader(value="X-Pado-REST-API-Key") String rest_api_key
    		, @RequestHeader(value="X-Pado-Session-Token") String rest_session_token
    		, @PathVariable("version") String rest_version
    		, @RequestParam Map<String, String> paramMap
    		, Model model
			) {

		int error_code = 0;
		String error_val = "";
		String path_val = "/possible-order-list/"+rest_version;
		String connect_type = defaultConfig.getConnectType();
		String json_result = null;
		String token_type = "";
		String authorizationHeader = httpServletRequest.getHeader("Authorization");
		String seller_token = "";
		String secret = "";
		String seller_id = "";
		
		JsonResult<Object> jsonResult = new JsonResult<Object>();
		JsonResult<Object> jsonResultError = new JsonResult<Object>();
		
		logger.info("PossibleOrderList connect_type:"+connect_type);
		logger.info("PossibleOrderList rest_version:"+rest_version);
		
		OrderDomain order_list = new OrderDomain();
		String order_type = "processing"; 
		// requested, processing,  delivered, cancelled
		
		//=================================================================
		// connect check
		if(connect_type.equals("local")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("dev")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("prod")==true) {
			token_type = defaultConfig.getHeaderSessionTokenProd();
			secret = defaultConfig.getPadoboxJwtSecretProd();
		}
		
		if(error_code == 0) {
			if(rest_api_key.equals(defaultConfig.getHeaderRestApi())==false 
					|| rest_session_token.equals(token_type)==false){
				error_code = 100;
				error_val = "Invalid authentication! : "+rest_api_key+"//"+rest_session_token;
			}
		}
		
		if(error_code == 0) {
			if(EmptyUtils.isEmpty(authorizationHeader)==false 
					&& authorizationHeader.startsWith("Bearer ")) {
				seller_token = authorizationHeader.substring(7);
				logger.info("PossibleOrderList seller_token:"+seller_token);
			}else {
				error_code = 100;
				error_val = "Invalid authentication!!:"+authorizationHeader;
			}
		}
		
		//=================================================================
		// seller info check
		SellerIdInfoDomain sellerid_info = new SellerIdInfoDomain();
		if(error_code == 0) {

			try {
				
				seller_id = jwtUtil.paserToken(seller_token, secret);
				logger.info("PossibleOrderList seller_id"+seller_id);
	
				if(EmptyUtils.isEmpty(seller_id)==true) {
					error_code = 100;
					error_val = " seller_id:"+seller_id+"// jwtUtil.paserToken error ";
				}else {
					sellerid_info =  orderConnectService.SellerIdCheck(model, connect_type, seller_id);
					if(sellerid_info.getError_code()>0) {
						error_code = 102;
						error_val = "seller_token:"+seller_token+" // "+sellerid_info.getError_val();
					}else {
						sellerid_info.setError_code(0);
						sellerid_info.setError_val("");
					}
				}
			} catch (Exception e) {
				error_code = 100;
				error_val = "Unauthenticated user!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//=================================================================
		// check param 
		OrderBodyDomain.ParamBody param_body = new OrderBodyDomain.ParamBody();
		
		//=================================================================
		// list 
		if(error_code==0) {
			
			try {
//				if(rest_version.equals("1")==true) {
//					order_list = orderConnectService.getOrderListPossibleOrder(model, order_type, connect_type, seller_id, param_body);
//				}
				if(rest_version.equals("2")==true) {
					order_list = orderConnectService.getOrderListPossibleOrderV2(model, order_type, connect_type, seller_id, param_body);
				}
				
				if(order_list.getError_code()>0) {
					error_code = order_list.getError_code();
					error_val = order_list.getError_val();
				}else {
					order_list.setError_code(null);
					order_list.setError_val(null);
				}
				
			} catch (Exception e) {
				error_code = 101;
				error_val = "getOrderList error!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//============================================================================= error logic 
		
		if(error_code != 0){
			
			logger.info("PossibleOrderList error_code:"+error_code);
			logger.info("PossibleOrderList error_val:"+error_val);
			logger.info("PossibleOrderList path_val:"+path_val+"//seller_token:"+seller_token);
			ErrorReturnDomain errorReturnDomain = new ErrorReturnDomain();
			errorReturnDomain =  connectService.ErrorCheck(connect_type
					, error_code
					, path_val+"//seller_token:"+seller_token+" //error_val "+error_val
					, seller_id
					, "get"); 
			jsonResultError.setRes_code(errorReturnDomain.getError_code());
			jsonResultError.setRes_value(errorReturnDomain.getError_value());
			jsonResultError.setResult(new Object());
			
		}else{
			jsonResult.setRes_code(0);
			jsonResult.setRes_value("");
		}
		
		//=============================================================================	error logic end

		if(error_code != 0){
			json_result = new Gson().toJson(jsonResultError);	
		}else{
			jsonResult.setResult(order_list); 
			json_result = new Gson().toJson(jsonResult);
		}
		
		return new Json(json_result); 
	}
	
	/**
	 * @desc 취소된 주문 리스트
	 */
	@GetMapping(value="/cancelled-list/{version}", headers="Accept=application/json;charset=UTF-8", produces="application/json;charset=UTF-8" )
	public @ResponseBody Json CnacelledList(
			HttpServletRequest httpServletRequest
			, @RequestHeader(value="X-Pado-REST-API-Key") String rest_api_key
    		, @RequestHeader(value="X-Pado-Session-Token") String rest_session_token
    		, @PathVariable("version") String rest_version
    		, @RequestParam Map<String, String> paramMap
    		, Model model
			) {

		int error_code = 0;
		String error_val = "";
		String path_val = "/cancelled-list/"+rest_version;
		String connect_type = defaultConfig.getConnectType();
		String json_result = null;
		String token_type = "";
		String authorizationHeader = httpServletRequest.getHeader("Authorization");
		String seller_token = "";
		String secret = "";
		String seller_id = "";
		
		JsonResult<Object> jsonResult = new JsonResult<Object>();
		JsonResult<Object> jsonResultError = new JsonResult<Object>();
		
		logger.info("CnacelledList connect_type:"+connect_type);
		logger.info("CnacelledList rest_version:"+rest_version);
		
		OrderDomain order_list = new OrderDomain();
		String order_type = "cancelled"; 
		// requested, processing,  delivered, cancelled
		
		//=================================================================
		// connect check
		if(connect_type.equals("local")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("dev")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("prod")==true) {
			token_type = defaultConfig.getHeaderSessionTokenProd();
			secret = defaultConfig.getPadoboxJwtSecretProd();
		}
		
		if(error_code == 0) {
			if(rest_api_key.equals(defaultConfig.getHeaderRestApi())==false 
					|| rest_session_token.equals(token_type)==false){
				error_code = 100;
				error_val = "Invalid authentication! : "+rest_api_key+"//"+rest_session_token;
			}
		}
		
		if(error_code == 0) {
			if(EmptyUtils.isEmpty(authorizationHeader)==false 
					&& authorizationHeader.startsWith("Bearer ")) {
				seller_token = authorizationHeader.substring(7);
				logger.info("CnacelledList seller_token:"+seller_token);
			}else {
				error_code = 100;
				error_val = "Invalid authentication!!:"+authorizationHeader;
			}
		}
		
		//=================================================================
		// seller info check
		SellerIdInfoDomain sellerid_info = new SellerIdInfoDomain();
		if(error_code == 0) {

			try {
				seller_id = jwtUtil.paserToken(seller_token, secret);
				logger.info("CnacelledList seller_id"+seller_id);
	
				if(EmptyUtils.isEmpty(seller_id)==true) {
					error_code = 100;
					error_val = " seller_id:"+seller_id+"// jwtUtil.paserToken error ";
				}else {
					sellerid_info =  orderConnectService.SellerIdCheck(model, connect_type, seller_id);
					if(sellerid_info.getError_code()>0) {
						error_code = 102;
						error_val = "seller_token:"+seller_token+" // "+sellerid_info.getError_val();
					}else {
						sellerid_info.setError_code(0);
						sellerid_info.setError_val("");
					}
				}
			} catch (Exception e) {
				error_code = 100;
				error_val = "Unauthenticated user!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//=================================================================
		// check param 
		OrderBodyDomain.ParamBody param_body = new OrderBodyDomain.ParamBody();
		if(EmptyUtils.isEmpty(paramMap)==false) {
			if(EmptyUtils.isEmpty(paramMap.get("start_date"))==false) {
				param_body.setStart_date(paramMap.get("start_date"));
			}else {
				param_body.setStart_date("");
			}
			if(EmptyUtils.isEmpty(paramMap.get("end_date"))==false) {
				param_body.setEnd_date(paramMap.get("end_date"));
			}else {
				param_body.setEnd_date("");
			}
			if(EmptyUtils.isEmpty(paramMap.get("search_keyword"))==false) {
				param_body.setSearch_keyword(paramMap.get("search_keyword"));
			}else {
				param_body.setSearch_keyword("");
			}
			if(EmptyUtils.isEmpty(paramMap.get("last_idx"))==false) {
				param_body.setLast_idx(paramMap.get("last_idx"));
			}else {
				param_body.setLast_idx(null);
			}
			logger.info("CnacelledList paramMap:"+paramMap.toString());
		}
		
		//=================================================================
		// list 
		if(error_code==0) {
			try {
				
//				if(rest_version.equals("1")==true) {
//					order_list = orderConnectService.getOrderListCancelled(model, order_type, connect_type, seller_id, param_body);	
//				} 
				if(rest_version.equals("2")==true) {
					order_list = orderConnectService.getOrderListCancelledV2(model, order_type, connect_type, seller_id, param_body);	
				}
				
				if(order_list.getError_code()>0) {
					error_code = order_list.getError_code();
					error_val = order_list.getError_val();
				}else {
					order_list.setError_code(null);
					order_list.setError_val(null);
				}
				
			} catch (Exception e) {
				error_code = 101;
				error_val = "getOrderList error!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//============================================================================= error logic 
		
		if(error_code != 0){
			
			logger.info("CnacelledList error_code:"+error_code);
			logger.info("CnacelledList error_val:"+error_val);
			logger.info("CnacelledList path_val:"+path_val+"//seller_token:"+seller_token);
			ErrorReturnDomain errorReturnDomain = new ErrorReturnDomain();
			errorReturnDomain =  connectService.ErrorCheck(connect_type
					, error_code
					, path_val+"//seller_token:"+seller_token+" //error_val "+error_val
					, seller_id
					, "get:"+paramMap.toString()); 
			jsonResultError.setRes_code(errorReturnDomain.getError_code());
			jsonResultError.setRes_value(errorReturnDomain.getError_value());
			jsonResultError.setResult(new Object());
			
		}else{
			jsonResult.setRes_code(0);
			jsonResult.setRes_value("");
		}
		
		//=============================================================================	error logic end

		if(error_code != 0){
			json_result = new Gson().toJson(jsonResultError);	
		}else{
			jsonResult.setResult(order_list); 
			json_result = new Gson().toJson(jsonResult);
		}
		
		return new Json(json_result); 
	}

	/**
	 * @desc 발송 완료 리스트
	 */
	@GetMapping(value="/delivered-list/{version}", headers="Accept=application/json;charset=UTF-8", produces="application/json;charset=UTF-8" )
	public @ResponseBody Json DeliveredList(
			HttpServletRequest httpServletRequest
			, @RequestHeader(value="X-Pado-REST-API-Key") String rest_api_key
    		, @RequestHeader(value="X-Pado-Session-Token") String rest_session_token
    		, @PathVariable("version") String rest_version
    		, @RequestParam Map<String, String> paramMap
    		, Model model
			) {

		int error_code = 0;
		String error_val = "";
		String path_val = "/delivered-list/"+rest_version;
		String connect_type = defaultConfig.getConnectType();
		String json_result = null;
		String token_type = "";
		String authorizationHeader = httpServletRequest.getHeader("Authorization");
		String seller_token = "";
		String secret = "";
		String seller_id = "";
		
		JsonResult<Object> jsonResult = new JsonResult<Object>();
		JsonResult<Object> jsonResultError = new JsonResult<Object>();
		
		logger.info("DeliveredList connect_type:"+connect_type);
		logger.info("DeliveredList rest_version:"+rest_version);
		
		OrderDomain order_list = new OrderDomain();
		String order_type = "delivered"; 
		// requested, processing,  delivered, cancelled
		
		//=================================================================
		// connect check
		if(connect_type.equals("local")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("dev")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("prod")==true) {
			token_type = defaultConfig.getHeaderSessionTokenProd();
			secret = defaultConfig.getPadoboxJwtSecretProd();
		}
		
		if(error_code == 0) {
			if(rest_api_key.equals(defaultConfig.getHeaderRestApi())==false 
					|| rest_session_token.equals(token_type)==false){
				error_code = 100;
				error_val = "Invalid authentication! : "+rest_api_key+"//"+rest_session_token;
			}
		}
		
		if(error_code == 0) {
			if(EmptyUtils.isEmpty(authorizationHeader)==false 
					&& authorizationHeader.startsWith("Bearer ")) {
				seller_token = authorizationHeader.substring(7);
				logger.info("DeliveredList seller_token:"+seller_token);
			}else {
				error_code = 100;
				error_val = "Invalid authentication!!:"+authorizationHeader;
			}
		}
		
		//=================================================================
		// seller info check
		SellerIdInfoDomain sellerid_info = new SellerIdInfoDomain();
		if(error_code == 0) {

			try {
				seller_id = jwtUtil.paserToken(seller_token, secret);
				logger.info("DeliveredList seller_id"+seller_id);
	
				if(EmptyUtils.isEmpty(seller_id)==true) {
					error_code = 100;
					error_val = " seller_id:"+seller_id+"// jwtUtil.paserToken error ";
				}else {
					sellerid_info =  orderConnectService.SellerIdCheck(model, connect_type, seller_id);
					if(sellerid_info.getError_code()>0) {
						error_code = 102;
						error_val = "seller_token:"+seller_token+" // "+sellerid_info.getError_val();
					}else {
						sellerid_info.setError_code(0);
						sellerid_info.setError_val("");
					}
				}
			} catch (Exception e) {
				error_code = 100;
				error_val = "Unauthenticated user!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//=================================================================
		// check param 
		
		OrderBodyDomain.ParamBody param_body = new OrderBodyDomain.ParamBody();
		if(EmptyUtils.isEmpty(paramMap)==false) {
			if(EmptyUtils.isEmpty(paramMap.get("start_date"))==false) {
				param_body.setStart_date(paramMap.get("start_date"));
				logger.info("DeliveredList start_date:"+paramMap.get("start_date"));
			}else {
				param_body.setStart_date("");
			}
			if(EmptyUtils.isEmpty(paramMap.get("end_date"))==false) {
				param_body.setEnd_date(paramMap.get("end_date"));
				logger.info("DeliveredList end_date:"+paramMap.get("end_date"));
			}else {
				param_body.setEnd_date("");
			}
			if(EmptyUtils.isEmpty(paramMap.get("search_keyword"))==false) {
				param_body.setSearch_keyword(paramMap.get("search_keyword"));
				logger.info("DeliveredList search_keyword:"+paramMap.get("search_keyword"));
			}else {
				param_body.setSearch_keyword("");
			}
			if(EmptyUtils.isEmpty(paramMap.get("last_idx"))==false) {
				param_body.setLast_idx(paramMap.get("last_idx"));
				logger.info("DeliveredList last_idx:"+paramMap.get("last_idx"));
			}else {
				param_body.setLast_idx(null);
			}
			
			logger.info("DeliveredList paramMap : "+paramMap.toString());
		}
		
		//=================================================================
		// list 
		if(error_code==0) {
			try {
//				if(rest_version.equals("1")==true) {
//					order_list = orderConnectService.getOrderListDeilvered(model, order_type, connect_type, seller_id, param_body);	
//				}
				if(rest_version.equals("2")==true) {
					order_list = orderConnectService.getOrderListDeilveredV2(model, order_type, connect_type, seller_id, param_body);	
				}
				if(order_list.getError_code()>0) {
					error_code = order_list.getError_code();
					error_val = order_list.getError_val();
				}else {
					order_list.setError_code(null);
					order_list.setError_val(null);
				}
				
			} catch (Exception e) {
				error_code = 101;
				error_val = "getOrderList error!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//============================================================================= error logic 
		
		if(error_code != 0){
			
			logger.info("DeliveredList error_code:"+error_code);
			logger.info("DeliveredList error_val:"+error_val);
			logger.info("DeliveredList path_val:"+path_val+"//seller_token:"+seller_token);
			ErrorReturnDomain errorReturnDomain = new ErrorReturnDomain();
			errorReturnDomain =  connectService.ErrorCheck(connect_type
					, error_code
					, path_val+"//seller_token:"+seller_token+" //error_val "+error_val
					, seller_id
					, "get:"+paramMap.toString()); 
			jsonResultError.setRes_code(errorReturnDomain.getError_code());
			jsonResultError.setRes_value(errorReturnDomain.getError_value());
			jsonResultError.setResult(new Object());
			
		}else{
			jsonResult.setRes_code(0);
			jsonResult.setRes_value("");
		}
		
		//=============================================================================	error logic end

		if(error_code != 0){
			json_result = new Gson().toJson(jsonResultError);	
		}else{
			jsonResult.setResult(order_list); 
			json_result = new Gson().toJson(jsonResult);
		}
		return new Json(json_result); 
	}
	
	/**
	 * @desc 관망중 리스트
	 */
	@GetMapping(value="/observ-list/{version}", headers="Accept=application/json;charset=UTF-8", produces="application/json;charset=UTF-8" )
	public @ResponseBody Json ObservList(
			HttpServletRequest httpServletRequest
			, @RequestHeader(value="X-Pado-REST-API-Key") String rest_api_key
    		, @RequestHeader(value="X-Pado-Session-Token") String rest_session_token
    		, @PathVariable("version") String rest_version
    		, @RequestParam Map<String, String> paramMap
    		, Model model
			) {

		int error_code = 0;
		String error_val = "";
		String path_val = "/observ-list/"+rest_version;
		String connect_type = defaultConfig.getConnectType();
		String json_result = null;
		String token_type = "";
		String authorizationHeader = httpServletRequest.getHeader("Authorization");
		String seller_token = "";
		String secret = "";
		String seller_id = "";
		
		JsonResult<Object> jsonResult = new JsonResult<Object>();
		JsonResult<Object> jsonResultError = new JsonResult<Object>();
		
		logger.info("ObservList connect_type:"+connect_type);
		logger.info("ObservList rest_version:"+rest_version);
		
		OrderDomain order_list = new OrderDomain();
		String order_type = "observ"; 
		// requested, processing,  delivered, cancelled
		
		//=================================================================
		// connect check
		if(connect_type.equals("local")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("dev")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("prod")==true) {
			token_type = defaultConfig.getHeaderSessionTokenProd();
			secret = defaultConfig.getPadoboxJwtSecretProd();
		}
		
		if(error_code == 0) {
			if(rest_api_key.equals(defaultConfig.getHeaderRestApi())==false 
					|| rest_session_token.equals(token_type)==false){
				error_code = 100;
				error_val = "Invalid authentication! : "+rest_api_key+"//"+rest_session_token;
			}
		}
		
		if(error_code == 0) {
			if(EmptyUtils.isEmpty(authorizationHeader)==false 
					&& authorizationHeader.startsWith("Bearer ")) {
				seller_token = authorizationHeader.substring(7);
				logger.info("ObservList seller_token:"+seller_token);
			}else {
				error_code = 100;
				error_val = "Invalid authentication!!:"+authorizationHeader;
			}
		}
		
		//=================================================================
		// seller info check
		SellerIdInfoDomain sellerid_info = new SellerIdInfoDomain();
		if(error_code == 0) {

			try {
				seller_id = jwtUtil.paserToken(seller_token, secret);
				logger.info("ObservList seller_id"+seller_id);
	
				if(EmptyUtils.isEmpty(seller_id)==true) {
					error_code = 100;
					error_val = " seller_id:"+seller_id+"// jwtUtil.paserToken error ";
				}else {
					sellerid_info =  orderConnectService.SellerIdCheck(model, connect_type, seller_id);
					if(sellerid_info.getError_code()>0) {
						error_code = 102;
						error_val = "seller_token:"+seller_token+" // "+sellerid_info.getError_val();
					}else {
						sellerid_info.setError_code(0);
						sellerid_info.setError_val("");
					}
				}
			} catch (Exception e) {
				error_code = 100;
				error_val = "Unauthenticated user!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//=================================================================
		// list 
		if(error_code==0) {
			try {
//				if(rest_version.equals("1")==true) {
//					order_list = orderConnectService.getObservList(model, order_type, connect_type, seller_id);
//				}
				if(rest_version.equals("2")==true) {
					order_list = orderConnectService.getObservListV2(model, order_type, connect_type, seller_id);
				}
				
				if(order_list.getError_code()>0) {
					error_code = order_list.getError_code();
					error_val = order_list.getError_val();
				}else {
					order_list.setError_code(null);
					order_list.setError_val(null);
				}
				
			} catch (Exception e) {
				error_code = 101;
				error_val = "getOrderList error!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//============================================================================= error logic 
		
		if(error_code != 0){
			
			logger.info("ObservList error_code:"+error_code);
			logger.info("ObservList error_val:"+error_val);
			logger.info("ObservList path_val:"+path_val+"//seller_token:"+seller_token);
			ErrorReturnDomain errorReturnDomain = new ErrorReturnDomain();
			errorReturnDomain =  connectService.ErrorCheck(connect_type
					, error_code
					, path_val+"//seller_token:"+seller_token+" //error_val "+error_val
					, seller_id
					, "get"); 
			jsonResultError.setRes_code(errorReturnDomain.getError_code());
			jsonResultError.setRes_value(errorReturnDomain.getError_value());
			jsonResultError.setResult(new Object());
			
		}else{
			jsonResult.setRes_code(0);
			jsonResult.setRes_value("");
		}
		
		//=============================================================================	error logic end

		if(error_code != 0){
			json_result = new Gson().toJson(jsonResultError);	
		}else{
			jsonResult.setResult(order_list); 
			json_result = new Gson().toJson(jsonResult);
		}
		
		return new Json(json_result); 
	}
		
		
	/**
	 * @desc 정산서 
	 */
	@GetMapping(value="/accounts-list/{version}", headers="Accept=application/json;charset=UTF-8", produces="application/json;charset=UTF-8" )
	public @ResponseBody Json AccountsList(
			HttpServletRequest httpServletRequest
			, @RequestHeader(value="X-Pado-REST-API-Key") String rest_api_key
    		, @RequestHeader(value="X-Pado-Session-Token") String rest_session_token
    		, @PathVariable("version") String rest_version
    		, @RequestParam Map<String, String> paramMap
    		, Model model
			) {

		int error_code = 0;
		String error_val = "";
		String path_val = "/accounts-list/"+rest_version;
		String connect_type = defaultConfig.getConnectType();
		String json_result = null;
		String token_type = "";
		String authorizationHeader = httpServletRequest.getHeader("Authorization");
		String seller_token = "";
		String secret = "";
		String seller_id = "";
		
		JsonResult<Object> jsonResult = new JsonResult<Object>();
		JsonResult<Object> jsonResultError = new JsonResult<Object>();
		
		logger.info("AccountsList connect_type:"+connect_type);
		logger.info("AccountsList rest_version:"+rest_version);
		
		AccountDomain.ReturnData account_list = new AccountDomain.ReturnData();
		
		//=================================================================
		// connect check
		if(connect_type.equals("local")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("dev")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("prod")==true) {
			token_type = defaultConfig.getHeaderSessionTokenProd();
			secret = defaultConfig.getPadoboxJwtSecretProd();
		}
		
		if(error_code == 0) {
			if(rest_api_key.equals(defaultConfig.getHeaderRestApi())==false 
					|| rest_session_token.equals(token_type)==false){
				error_code = 100;
				error_val = "Invalid authentication! : "+rest_api_key+"//"+rest_session_token;
			}
		}
		
		if(error_code == 0) {
			if(EmptyUtils.isEmpty(authorizationHeader)==false 
					&& authorizationHeader.startsWith("Bearer ")) {
				seller_token = authorizationHeader.substring(7);
				logger.info("AccountsList seller_token:"+seller_token);
			}else {
				error_code = 100;
				error_val = "Invalid authentication!!:"+authorizationHeader;
			}
		}
		
		//=================================================================
		// seller info check
		SellerIdInfoDomain sellerid_info = new SellerIdInfoDomain();
		if(error_code == 0) {

			try {
				seller_id = jwtUtil.paserToken(seller_token, secret);
				logger.info("AccountsList seller_id"+seller_id);
	
				if(EmptyUtils.isEmpty(seller_id)==true) {
					error_code = 100;
					error_val = " seller_id:"+seller_id+"// jwtUtil.paserToken error ";
				}else {
					sellerid_info =  orderConnectService.SellerIdCheck(model, connect_type, seller_id);
					if(sellerid_info.getError_code()>0) {
						error_code = 102;
						error_val = "seller_token:"+seller_token+" // "+sellerid_info.getError_val();
					}else {
						sellerid_info.setError_code(0);
						sellerid_info.setError_val("");
					}
				}
			} catch (Exception e) {
				error_code = 100;
				error_val = "Unauthenticated user!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//=================================================================
		// check param 
		OrderBodyDomain.ParamBody param_body = new OrderBodyDomain.ParamBody();
		if(EmptyUtils.isEmpty(paramMap)==false) {
			if(EmptyUtils.isEmpty(paramMap.get("start_date"))==false) {
				param_body.setStart_date(paramMap.get("start_date"));
			}else {
				param_body.setStart_date("");
			}
			if(EmptyUtils.isEmpty(paramMap.get("end_date"))==false) {
				param_body.setEnd_date(paramMap.get("end_date"));
			}else {
				param_body.setEnd_date("");
			}
			if(EmptyUtils.isEmpty(paramMap.get("last_idx"))==false) {
				param_body.setLast_idx(paramMap.get("last_idx"));
			}else {
				param_body.setLast_idx("");
			}
			//1 : 검색, 주단위 모두 상단 항목만 , 2 : product_list , 3 : excel_data
			if(EmptyUtils.isEmpty(paramMap.get("view_type"))==false) {
				param_body.setView_type(Integer.parseInt(paramMap.get("view_type")));
			}else {
				param_body.setView_type(null);
			}
			//view_type > 1 & 주단위 리스트 상세 or 엑셀 다운로드 
			if(EmptyUtils.isEmpty(paramMap.get("accounts_idx"))==false) {
				try {
					param_body.setAccounts_idx(Integer.parseInt(paramMap.get("accounts_idx")));	
				} catch (Exception e) {
					error_code = 100;
					error_val = "accounts_idx error "+paramMap.get("accounts_idx")+": e"+e;
				}
			}else {
				param_body.setAccounts_idx(null);
			}
			
			if(EmptyUtils.isEmpty(paramMap.get("last_account_idx"))==false) {
				param_body.setLast_account_idx(paramMap.get("last_account_idx"));
			}else {
				param_body.setLast_account_idx(null);
			}
			
			if(EmptyUtils.isEmpty(paramMap.get("accounts_key"))==false) {
				param_body.setAccounts_key(paramMap.get("accounts_key"));
			}else {
				param_body.setAccounts_key(null);
			}
			
			logger.info("AccountsList paramMap : "+paramMap.toString());
			
		}else {
			param_body.setLast_idx("");
		}
		
		//=================================================================
		// list 
		if(error_code==0) {

			try {
				logger.info("AccountsList last_idx:"+param_body.getLast_idx());
//				if(rest_version.equals("1")==true) {
//					account_list = orderConnectService.getAccountsList(model, connect_type, seller_id, param_body);
//				}
				if(rest_version.equals("2")==true) {
					account_list = orderConnectService.getAccountsListV2(model, connect_type, seller_id, param_body);	
				}
				
				if(account_list.getError_code()>0) {
					error_code = account_list.getError_code();
					error_val = account_list.getError_val();
				}else {
					account_list.setError_code(null);
					account_list.setError_val(null);
				}	
			} catch (Exception e) {
				error_code = 101;
				error_val = "getAccountsList error!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//============================================================================= error logic 
		
		if(error_code != 0){
			
			logger.info("AccountsList error_code:"+error_code);
			logger.info("AccountsList error_val:"+error_val);
			logger.info("AccountsList path_val:"+path_val+"//seller_token:"+seller_token);
			ErrorReturnDomain errorReturnDomain = new ErrorReturnDomain();
			errorReturnDomain =  connectService.ErrorCheck(connect_type
					, error_code
					, path_val+"//seller_token:"+seller_token+" //error_val "+error_val
					, seller_id
					, "get:"+paramMap.toString()); 
			jsonResultError.setRes_code(errorReturnDomain.getError_code());
			jsonResultError.setRes_value(errorReturnDomain.getError_value());
			jsonResultError.setResult(new Object());

		}else{
			jsonResult.setRes_code(0);
			jsonResult.setRes_value("");
		}
		
		//=============================================================================	error logic end

		if(error_code != 0){
			json_result = new Gson().toJson(jsonResultError);	
		}else{
			jsonResult.setResult(account_list); 
			json_result = new Gson().toJson(jsonResult);
		}
		
		return new Json(json_result); 
	}
	
	/**
	 * @desc 상태 변경
	 */
	@PostMapping(value="/status-modify/{version}", headers="Accept=application/json;charset=UTF-8", produces="application/json;charset=UTF-8" )
	public @ResponseBody Json StatusModify(
			HttpServletRequest httpServletRequest
			, @RequestHeader(value="X-Pado-REST-API-Key") String rest_api_key
    		, @RequestHeader(value="X-Pado-Session-Token") String rest_session_token
    		, @PathVariable("version") String rest_version
    		, @RequestBody String body
    		, Model model
			) {

		int error_code = 0;
		String error_val = "";
		String path_val = "/status-modify/"+rest_version;
		String connect_type = defaultConfig.getConnectType();
		String json_result = null;
		String token_type = "";
		String authorizationHeader = httpServletRequest.getHeader("Authorization");
		String seller_token = "";
		String secret = "";
		String seller_id = "";
		
		JsonResult<Object> jsonResult = new JsonResult<Object>();
		JsonResult<Object> jsonResultError = new JsonResult<Object>();
		
		logger.info("StatusModify connect_type:"+connect_type);
		logger.info("StatusModify rest_version:"+rest_version);
		
		OrderBodyDomain.ModifyBodyList requestModifyBody = new OrderBodyDomain.ModifyBodyList();
		SetDomain.ControllerResultStatusModifyReturn ModifyReturnResult = new SetDomain.ControllerResultStatusModifyReturn();
		SetDomain.ControllerResultStatusModifyReturnV2 ModifyReturnResultV2 = new SetDomain.ControllerResultStatusModifyReturnV2();
		
		//=================================================================
		// connect check
		if(connect_type.equals("local")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("dev")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("prod")==true) {
			token_type = defaultConfig.getHeaderSessionTokenProd();
			secret = defaultConfig.getPadoboxJwtSecretProd();
		}
		
		if(error_code == 0) {
			if(rest_api_key.equals(defaultConfig.getHeaderRestApi())==false 
					|| rest_session_token.equals(token_type)==false){
				error_code = 100;
				error_val = "Invalid authentication! : "+rest_api_key+"//"+rest_session_token;
			}
		}
		
		if(error_code == 0) {
			if(EmptyUtils.isEmpty(authorizationHeader)==false 
					&& authorizationHeader.startsWith("Bearer ")) {
				seller_token = authorizationHeader.substring(7);
				logger.info("StatusModify seller_token:"+seller_token);
			}else {
				error_code = 100;
				error_val = "Invalid authentication!!:"+authorizationHeader;
			}
		}
		//=================================================================
		// seller info check
		SellerIdInfoDomain sellerid_info = new SellerIdInfoDomain();
		if(error_code == 0) {

			try {
				seller_id = jwtUtil.paserToken(seller_token, secret);
				logger.info("StatusModify seller_id"+seller_id);
	
				if(EmptyUtils.isEmpty(seller_id)==true) {
					error_code = 100;
					error_val = " seller_id:"+seller_id+"// jwtUtil.paserToken error ";
				}else {
					sellerid_info =  orderConnectService.SellerIdCheck(model, connect_type, seller_id);
					if(sellerid_info.getError_code()>0) {
						error_code = 102;
						error_val = "seller_token:"+seller_token+" // "+sellerid_info.getError_val();
					}else {
						sellerid_info.setError_code(0);
						sellerid_info.setError_val("");
					}
				}
			} catch (Exception e) {
				error_code = 100;
				error_val = "Unauthenticated user!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//=================================================================
		// body convert 
		if(error_code == 0) {
			logger.info("StatusModify body:"+body);
			try {
				requestModifyBody = gson.fromJson(body, OrderBodyDomain.ModifyBodyList.class);	
			} catch (Exception e) {
				error_code = 100;
				error_val = "error body:"+body+"// e:"+e;
			}
		}
		
		//=================================================================
		// create a result to return
		if(error_code == 0) {
			
			try {
//				if(rest_version.equals("1")==true) {
//					ModifyReturnResult = orderConnectService.postStatusModify(model
//							, connect_type, seller_id, requestModifyBody, sellerid_info, seller_token);
//					
//					if(ModifyReturnResult.getError_code()>0) {
//						error_code = ModifyReturnResult.getError_code();
//						error_val = ModifyReturnResult.getError_val();
//					}
//				}
//				if(rest_version.equals("2")==true) {
//					ModifyReturnResultV2 = orderConnectService.postStatusModifyV2(model
//							, connect_type, seller_id, requestModifyBody, sellerid_info, seller_token);
//					
//					if(ModifyReturnResultV2.getError_code()>0) {
//						error_code = ModifyReturnResultV2.getError_code();
//						error_val = ModifyReturnResultV2.getError_val();
//					}
//				}
				if(rest_version.equals("3")==true) {// 현재 사용중  1, 2는 이전 어부앱 
					
					if(requestModifyBody.getModify_key()==1 && requestModifyBody.getModify_status() ==2 ) {
						// 프론트에서 30개 이하는 여기로 옴. 2 
						if(EmptyUtils.isEmpty(requestModifyBody.getData()) == false
					            && requestModifyBody.getData().size() > 0) {
							// 개별 선택 : 기존과 동일
							logger.info("StatusModify getModify_key:"+requestModifyBody.getModify_key()+": "+seller_id);
							logger.info("StatusModify getModify_status:"+requestModifyBody.getModify_status()+": "+seller_id);
							logger.info("StatusModify 1 : 2 : 30개 이하 조업가능 변경 시작 !! "+": "+seller_id);
							// 프론트에서 30개 이하는 전체 선택해도 2로 처리 
							 
							// 가능으로 변경 v3
							ModifyReturnResultV2 = orderConnectService.postStatusModifyV3(model
									, connect_type, seller_id, requestModifyBody, sellerid_info, seller_token);
							
						}else {
							// 현실적으로 오류임. 프론트에서 2는 무조건 data를 넣어줌. 
							error_code = 100;
							error_val = gson.toJson(requestModifyBody)+": "+seller_id;					
							
						}
					
					}else if(requestModifyBody.getModify_key()==1 && requestModifyBody.getModify_status() ==3) {
						logger.info("StatusModify getModify_key:"+requestModifyBody.getModify_key()+": "+seller_id);
						logger.info("StatusModify getModify_status:"+requestModifyBody.getModify_status()+": "+seller_id);
						logger.info("StatusModify 1 : 3 : 전체 선택해서 조업가능 변경 시작 !! "+": "+seller_id);
						
						SellerIdDeliveryDomain delivery_list = new SellerIdDeliveryDomain();
						delivery_list = orderConnectService.SellerIdDeliveryContractsV2(model, connect_type
								, seller_id, seller_token, sellerid_info);
						
						if(delivery_list != null
						        && delivery_list.getData() != null
						        && delivery_list.getData().size() > 0
						        && EmptyUtils.isEmpty(delivery_list.getData().get(0).getId()) == false) {
							sellerid_info.setDelivery_contracts_id(delivery_list.getData().get(0).getId());
						}else {
							sellerid_info.setDelivery_contracts_id("");
						}
						
						String request_id = batchStatusService.createBatch(seller_id);
						
				            asyncStatusModifyService.executeStatusModifyAsync(
				                    request_id,
				                    model,
				                    connect_type,
				                    seller_id,
				                    requestModifyBody,
				                    sellerid_info,
				                    seller_token
				            );
						
						ReturnModifyResultV2 v2_result = new ReturnModifyResultV2();
						v2_result.setAll_process_yn("Y");
						
						ModifyReturnResultV2.setError_code(0);
						ModifyReturnResultV2.setError_val("");
						ModifyReturnResultV2.setResult_return(v2_result);
						
					}else {
						logger.info("StatusModify Observ v2 connect!!: "+seller_id);
						ModifyReturnResultV2 = orderConnectService.postStatusModifyV2(model
								, connect_type, seller_id, requestModifyBody, sellerid_info, seller_token);
					}
					if(ModifyReturnResultV2.getError_code()>0) {
						error_code = ModifyReturnResultV2.getError_code();
						error_val = ModifyReturnResultV2.getError_val();
						
						logger.info("StatusModify error_code:"+error_code);
						logger.info("StatusModify error_val:"+error_val);
					}
					if(defaultConfig.getConnectType().equals("dev")==true) {
						error_code = 0;
						error_val = "";	
					}
				}
				
			} catch (Exception e) {
				error_code = 101;
				error_val = "postStatusModify error!! seller_token:"+seller_token+"// e:"+e;
			}
			
		}
 	
		//============================================================================= error logic 
		
		if(error_code != 0){
			
			logger.info("StatusModify error_code:"+error_code);
			logger.info("StatusModify error_val:"+error_val);
			logger.info("StatusModify path_val:"+path_val+"//seller_token:"+seller_token);
			ErrorReturnDomain errorReturnDomain = new ErrorReturnDomain();
			errorReturnDomain =  connectService.ErrorCheck(connect_type
					, error_code
					, path_val+"//seller_token:"+seller_token+" //error_val "+error_val
					, seller_id
					, "post:"+body); 
			jsonResultError.setRes_code(errorReturnDomain.getError_code());
			jsonResultError.setRes_value(errorReturnDomain.getError_value());
			jsonResultError.setResult(new Object());
			
			jsonResult.setRes_code(0);
			jsonResult.setRes_value("");
			
		}else{
			jsonResult.setRes_code(0);
			jsonResult.setRes_value("");
		}
		
		//=============================================================================	error logic end

		if(error_code != 0){
			json_result = new Gson().toJson(jsonResultError);	
		}else{// new Object() 
			if(rest_version.equals("1")==true) {
				jsonResult.setResult(ModifyReturnResult.getResult_return());
			}
			if(rest_version.equals("2")==true) {
				jsonResult.setResult(ModifyReturnResultV2.getResult_return());
			}
			if(rest_version.equals("3")==true) {
				jsonResult.setResult(ModifyReturnResultV2.getResult_return()); 
			}
			json_result = new Gson().toJson(jsonResult);
		}
		
		logger.info("StatusModify status modify end !! //////////////////////////////////////////////////////////////////");
		
		return new Json(json_result); 
	}
	
	/**
	 * @desc 각종 세팅값 
	 */
	@GetMapping(value="/setting/{version}", headers="Accept=application/json;charset=UTF-8", produces="application/json;charset=UTF-8" )
	public @ResponseBody Json Setting(
			HttpServletRequest httpServletRequest
			, @RequestHeader(value="X-Pado-REST-API-Key") String rest_api_key
    		, @RequestHeader(value="X-Pado-Session-Token") String rest_session_token
    		, @PathVariable("version") String rest_version
    		, @RequestParam Map<String, String> paramMap
    		, Model model
			) {

		int error_code = 0;
		String error_val = "";
		String path_val = "/setting/"+rest_version;
		String connect_type = defaultConfig.getConnectType();
		String json_result = null;
		String token_type = "";
		String authorizationHeader = httpServletRequest.getHeader("Authorization");
		String seller_token = "";
		String secret = "";
		String seller_id = "";
		
		JsonResult<Object> jsonResult = new JsonResult<Object>();
		JsonResult<Object> jsonResultError = new JsonResult<Object>();
		
		logger.info("Setting connect_type:"+connect_type);
		logger.info("Setting rest_version:"+rest_version);
		
		int type_val = 0;
		
		SetDomain setDomain = new SetDomain();
		
		//=================================================================
		// connect check
		if(connect_type.equals("local")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("dev")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("prod")==true) {
			token_type = defaultConfig.getHeaderSessionTokenProd();
			secret = defaultConfig.getPadoboxJwtSecretProd();
		}
		
		if(error_code == 0) {
			if(rest_api_key.equals(defaultConfig.getHeaderRestApi())==false 
					|| rest_session_token.equals(token_type)==false){
				error_code = 100;
				error_val = "Invalid authentication! : "+rest_api_key+"//"+rest_session_token;
			}
		}
		
		if(error_code == 0) {
			if(EmptyUtils.isEmpty(authorizationHeader)==false 
					&& authorizationHeader.startsWith("Bearer ")) {
				seller_token = authorizationHeader.substring(7);
				logger.info("Setting seller_token:"+seller_token);
			}else {
				error_code = 100;
				error_val = "Invalid authentication!!:"+authorizationHeader;
			}
		}
		//=================================================================
		// seller info check
		SellerIdInfoDomain sellerid_info = new SellerIdInfoDomain();
		if(error_code == 0) {

			try {
				seller_id = jwtUtil.paserToken(seller_token, secret);
				logger.info("Setting seller_id"+seller_id);
	
				if(EmptyUtils.isEmpty(seller_id)==true) {
					error_code = 100;
					error_val = " seller_id:"+seller_id+"// jwtUtil.paserToken error ";
				}else {
					sellerid_info =  orderConnectService.SellerIdCheck(model, connect_type, seller_id);
					if(sellerid_info.getError_code()>0) {
						error_code = 102;
						error_val = "seller_token:"+seller_token+" // "+sellerid_info.getError_val();
					}else {
						sellerid_info.setError_code(0);
						sellerid_info.setError_val("");
					}
				}
			} catch (Exception e) {
				error_code = 100;
				error_val = "Unauthenticated user!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		//==================================================================
		if(error_code == 0) {
			if(EmptyUtils.isEmpty(paramMap)==false) {
				if(EmptyUtils.isEmpty(paramMap.get("type_val"))==false) {
					type_val =  Integer.parseInt(paramMap.get("type_val"));
				}else {
					error_code = 100;
					error_val = "type_val error :"+paramMap.get("type_val");
				}
				
				logger.info("Setting paramMap:"+paramMap.toString());
			}
		}
		//=================================================================
		// create a result to return
		if(error_code == 0) {
			
			try {
//				if(rest_version.equals("1")==true) {
//					int sync_yn = 0;
//					sync_yn = orderConnectService.getSellerIdSync(model, connect_type, seller_id);
//					setDomain.setSellerid_sync(sync_yn);
//				}
				
				if(type_val==1) {
					SetDomain.SellerIdInfo return_val = new SetDomain.SellerIdInfo();
					SetDomain.SellerIdInfo.ContactInfo contact_information = new SetDomain.SellerIdInfo.ContactInfo();
					SetDomain.SellerIdInfo.DeliveryInfo delivery_information = new SetDomain.SellerIdInfo.DeliveryInfo();
					
					return_val.setId(sellerid_info.getId());
					return_val.setName(sellerid_info.getName());
					return_val.setGroupId(sellerid_info.getGroupId());
					
					if(EmptyUtils.isEmpty(sellerid_info.getContactInformation())==false) {
						if(EmptyUtils.isEmpty(sellerid_info.getContactInformation().getEmail())==false) {
							contact_information.setEmail(sellerid_info.getContactInformation().getEmail());	
						}
						if(EmptyUtils.isEmpty(sellerid_info.getContactInformation().getName())==false) {
							contact_information.setName(sellerid_info.getContactInformation().getName());	
						}
						if(EmptyUtils.isEmpty(sellerid_info.getContactInformation().getPhoneNumber())==false) {
							contact_information.setPhone_number(sellerid_info.getContactInformation().getPhoneNumber());	
						}
					}
					
					if(EmptyUtils.isEmpty(sellerid_info.getDeliveryInformation())==false) {
						if(EmptyUtils.isEmpty(sellerid_info.getDeliveryInformation().getZipcode())==false) {
							delivery_information.setZipcode(sellerid_info.getDeliveryInformation().getZipcode());	
						}
						if(EmptyUtils.isEmpty(sellerid_info.getDeliveryInformation().getAddress())==false) {
							delivery_information.setAddress(sellerid_info.getDeliveryInformation().getAddress());	
						}
						if(EmptyUtils.isEmpty(sellerid_info.getDeliveryInformation().getAddress2())==false) {
							delivery_information.setAddress2(sellerid_info.getDeliveryInformation().getAddress2());		
						}
					}
					
					return_val.setContact_information(contact_information);
					return_val.setDelivery_information(delivery_information);
					
					setDomain.setMain_menu_count(null);
					setDomain.setSellerId_info(return_val);
					setDomain.setCourier_com_list(null);
				
				}else if(type_val==2) {
					
					SetDomain.MainMenuCount return_val = new SetDomain.MainMenuCount(); 
					
//					if(rest_version.equals("1")==true) {
//						return_val = orderConnectService.getMainCntData(model, connect_type, seller_id);	
//					}
					if(rest_version.equals("2")==true) {
						return_val = orderConnectService.getMainCntDataV2(model, connect_type, seller_id);
						setDomain.setSellerid_sync(null);
					}
					// 모임 오류일때 대비함.
					if(EmptyUtils.isEmpty(return_val)==false) {
						setDomain.setMain_menu_count(return_val);
					}else {
						setDomain.setMain_menu_count(null);
					}
				
					setDomain.setMain_menu_count(return_val);
					setDomain.setSellerId_info(null);
					setDomain.setCourier_com_list(null);
					
				}else if(type_val==3) {
				
					SellerIdDeliveryDomain delivery_list = new SellerIdDeliveryDomain();
					SetDomain.CourierComList courier_com_single = new SetDomain.CourierComList();
					List<CourierComList> courier_com_list = new ArrayList<CourierComList>();
					
//					if(rest_version.equals("1")==true) {
//						delivery_list = orderConnectService.SellerIdDeliveryContracts(model, connect_type
//								, seller_id, seller_token, sellerid_info);
//					}
					if(rest_version.equals("2")==true) {
						delivery_list = orderConnectService.SellerIdDeliveryContractsV2(model, connect_type
								, seller_id, seller_token, sellerid_info);
						setDomain.setSellerid_sync(null);
					}
					
					if(EmptyUtils.isEmpty(delivery_list)==true) {
						List<CourierComList> emptyData = new ArrayList<CourierComList>();
						setDomain.setCourier_com_list(emptyData);	
					}else {
						for(int i = 0; i < delivery_list.getData().size(); i++) {
							courier_com_single.setCourier_id(delivery_list.getData().get(i).getId());
							courier_com_single.setCourier_name(delivery_list.getData().get(i).getName());
							courier_com_single.setCourier_code(delivery_list.getData().get(i).getCompanyCode());
							courier_com_list.add(courier_com_single);
						}
						setDomain.setCourier_com_list(courier_com_list);
					}
					setDomain.setMain_menu_count(null);
					setDomain.setSellerId_info(null);
				}
			} catch (Exception e) {
				error_code = 101;
				error_val = "getSellerIdSync getSettingData SellerIdDeliveryContracts error!! :type_val: "+type_val+":rest_version: "+rest_version+":seller_id:"+seller_id+"// e:"+e;
			}
			
		}
 	
		//============================================================================= error logic 
		
		if(error_code != 0){
			
			logger.info("Setting error_code:"+error_code);
			logger.info("Setting error_val:"+error_val);
			logger.info("Setting path_val:"+path_val+"//seller_token:"+seller_token);
			ErrorReturnDomain errorReturnDomain = new ErrorReturnDomain();
			errorReturnDomain =  connectService.ErrorCheck(connect_type
					, error_code
					, path_val+"//seller_token:"+seller_token+" //error_val "+error_val
					, seller_id
					, "get:"+paramMap.toString()); 
			jsonResultError.setRes_code(errorReturnDomain.getError_code());
			jsonResultError.setRes_value(errorReturnDomain.getError_value());
			jsonResultError.setResult(new Object());
			
		}else{
			jsonResult.setRes_code(0);
			jsonResult.setRes_value("");
		}
		
		//=============================================================================	error logic end

		if(error_code != 0){
			json_result = new Gson().toJson(jsonResultError);	
		}else{
			jsonResult.setResult(setDomain); 
			json_result = new Gson().toJson(jsonResult);
		}
		
		return new Json(json_result); 
	}
	
	/**
	 * @desc 수동 데이터 동기화 사용
	 */
	@GetMapping(value="/data-sync/{datatype}/{version}", headers="Accept=application/json;charset=UTF-8", produces="application/json;charset=UTF-8" )
	public @ResponseBody Json DatdSync(
			HttpServletRequest httpServletRequest
			, @RequestHeader(value="X-Pado-REST-API-Key") String rest_api_key
    		, @RequestHeader(value="X-Pado-Session-Token") String rest_session_token
    		, @PathVariable("datatype") String data_type
    		, @PathVariable("version") String rest_version
    		, Model model
			) {

		int error_code = 0;
		String error_val = "";
		String path_val = "/data-sync/"+rest_version;
		String connect_type = defaultConfig.getConnectType();
		String json_result = null;
		String token_type = "";
		String authorizationHeader = httpServletRequest.getHeader("Authorization");
		String seller_token = "";
		String secret = "";
		String seller_id = "";
		
		JsonResult<Object> jsonResult = new JsonResult<Object>();
		JsonResult<Object> jsonResultError = new JsonResult<Object>();
		
		logger.info("DatdSync connect_type:"+connect_type);
		logger.info("DatdSync rest_version:"+rest_version);
		logger.info("DatdSync data_type:"+data_type);
		
		DefaultDomain.ErrorCheck error_check = new DefaultDomain.ErrorCheck();
		
		//=================================================================
		// connect check
		if(connect_type.equals("local")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("dev")==true) {
			token_type = defaultConfig.getHeaderSessionTokenDev();
			secret = defaultConfig.getPadoboxJwtSecretDev();
		}else if(connect_type.equals("prod")==true) {
			token_type = defaultConfig.getHeaderSessionTokenProd();
			secret = defaultConfig.getPadoboxJwtSecretProd();
		}
		
		if(rest_version.equals(defaultConfig.getRestVersion())==false) {
			error_code = 100;
			error_val = "This is the wrong version!";
		}
		
		if(error_code == 0) {
			if(rest_api_key.equals(defaultConfig.getHeaderRestApi())==false 
					|| rest_session_token.equals(token_type)==false){
				error_code = 100;
				error_val = "Invalid authentication! : "+rest_api_key+"//"+rest_session_token;
			}
		}
		
		if(error_code == 0) {
			if(EmptyUtils.isEmpty(authorizationHeader)==false 
					&& authorizationHeader.startsWith("Bearer ")) {
				seller_token = authorizationHeader.substring(7);
				logger.info("DatdSync seller_token:"+seller_token);
			}else {
				error_code = 100;
				error_val = "Invalid authentication!!:"+authorizationHeader;
			}
		}
		//=================================================================
		// seller info check
		SellerIdInfoDomain sellerid_info = new SellerIdInfoDomain();
		if(error_code == 0) {

			try {
				seller_id = jwtUtil.paserToken(seller_token, secret);
				logger.info("DatdSync seller_id"+seller_id);
	
				if(EmptyUtils.isEmpty(seller_id)==true) {
					error_code = 100;
					error_val = " seller_id:"+seller_id+"// jwtUtil.paserToken error ";
				}else {
					sellerid_info =  orderConnectService.SellerIdCheck(model, connect_type, seller_id);
					if(sellerid_info.getError_code()>0) {
						error_code = 102;
						error_val = "seller_token:"+seller_token+" // "+sellerid_info.getError_val();
					}else {
						sellerid_info.setError_code(0);
						sellerid_info.setError_val("");
					}
				}
				
			} catch (Exception e) {
				error_code = 100;
				error_val = "Unauthenticated user!! seller_token:"+seller_token+"// e:"+e;
			}
		}
		
		//=================================================================
		// create a result to return
		if(error_code == 0) {
			try {
				if(data_type.equals("order")==true) {
					error_check = orderConnectService.GetMoimSyncData(connect_type, seller_id, "data-sync");
					if(error_check.getError_code()>0) {
						error_code = error_check.getError_code();
						error_val = error_check.getError_val();
					}				
				}else if(data_type.equals("account")==true) {
					error_check = orderConnectService.AccountsRdsSyncProcess(connect_type, seller_id, "data-sync");
					if(error_check.getError_code()>0) {
						error_code = error_check.getError_code();
						error_val = error_check.getError_val();
					}
				}
			} catch (Exception e) {
				error_code = 101;
				error_val = "GetMoimSyncData AccountsRdsSyncProcess "
						+ " error!! :data_type: "+data_type.equals("order")+":seller_token:"+seller_token+"// e:"+e;
			}
		}
	 	
		//============================================================================= error logic 
		
		if(error_code != 0){
			
			logger.info("DatdSync error_code:"+error_code);
			logger.info("DatdSync error_val:"+error_val);
			logger.info("DatdSync path_val:"+path_val+"//seller_token:"+seller_token);
			ErrorReturnDomain errorReturnDomain = new ErrorReturnDomain();
			errorReturnDomain =  connectService.ErrorCheck(connect_type
					, error_code
					, path_val+"//seller_token:"+seller_token+" //error_val "+error_val
					, seller_id
					, "get:"+data_type); 
			jsonResultError.setRes_code(errorReturnDomain.getError_code());
			jsonResultError.setRes_value(errorReturnDomain.getError_value());
			jsonResultError.setResult(new Object());
			
		}else{
			jsonResult.setRes_code(0);
			jsonResult.setRes_value("");
		}
		
		//=============================================================================	error logic end

		if(error_code != 0){
			json_result = new Gson().toJson(jsonResultError);	
		}else{
			jsonResult.setResult(new Object()); 
			json_result = new Gson().toJson(jsonResult);
		}
		
		return new Json(json_result);
	}
	
}

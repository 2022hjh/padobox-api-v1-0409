package com.pience.padobox.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.google.gson.Gson;
import com.pience.padobox.model.AlarmTalkPartnerDomain;
import com.pience.padobox.model.OrderDomain;
import com.pience.padobox.utility.EmptyUtils;
import com.pience.padobox.utility.GetNumber;

@Service
public class AlarmPartnerConnectService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	
	Gson gson = new Gson();
	
	@Autowired
	AlarmPartnerService alarmPartnerService;
	
	
	/**
	 * @desc 알림톡 파트너 CURD
	 */
	public AlarmTalkPartnerDomain.ReturnData PartnerCURD(String curd_type, String connect_type, AlarmTalkPartnerDomain.BodyData requestModifyBody) {
	
		AlarmTalkPartnerDomain.ReturnData returnData = new AlarmTalkPartnerDomain.ReturnData();
		List<AlarmTalkPartnerDomain.Data> getSingleData = new ArrayList<AlarmTalkPartnerDomain.Data>();
		String pater_id = "";
		
		logger.info("con curd_type:"+curd_type); //1 생성, 2 수정, 3 조회, 4 삭제
		
		returnData.setError_code(0);
		returnData.setError_val("");
		
		if(curd_type.equals("1")==true) {//생성
			
			if(EmptyUtils.isEmpty(requestModifyBody.getPartner_phone())==false) {
				
				try {
					List<OrderDomain.PluginSellerInfo> getSellerInfo = new ArrayList<OrderDomain.PluginSellerInfo>();
					Model model_get_2 = new ExtendedModelMap();
					model_get_2.addAttribute("seller_id", requestModifyBody.getSeller_id());
					getSellerInfo = alarmPartnerService.getKakaoAlarmTalkSellerList(model_get_2);
					if(EmptyUtils.isEmpty(getSellerInfo)==false) {
						if(getSellerInfo.size()>0) {
							//pass
						}else {
							returnData.setError_code(401);
							returnData.setError_val("requestModifyBody.getSeller_id() 없음."+requestModifyBody.getSeller_id());
						}
					}else {
						returnData.setError_code(401);
						returnData.setError_val("requestModifyBody.getSeller_id() 없음."+requestModifyBody.getSeller_id());
					}
					
					if(returnData.getError_code()==0) {
						
						Model model_get_1 = new ExtendedModelMap();
						model_get_1.addAttribute("get_type", 1);
						model_get_1.addAttribute("seller_id", requestModifyBody.getSeller_id());
						model_get_1.addAttribute("partner_phone", requestModifyBody.getPartner_phone());
						getSingleData = alarmPartnerService.getKakaoAlarmTalkPartnerList(model_get_1);
						
						if(getSingleData.size()>0) {
							returnData.setError_code(401);
							returnData.setError_val("Partner_phone 이미 있음."+requestModifyBody.getPartner_phone());
						}else {
							
							String code = "";
							code = GetNumber.getRandomKey(10);
							pater_id = "PI:"+code;
							String partner_phone = "";
							String check_val = requestModifyBody.getPartner_phone().substring(0, 3);
							if(check_val.equals("010")==true) {
								partner_phone  = requestModifyBody.getPartner_phone();
								Model model_1 = new ExtendedModelMap();
								model_1.addAttribute("seller_id", requestModifyBody.getSeller_id());
								model_1.addAttribute("partner_id", pater_id);
								model_1.addAttribute("partner_name", requestModifyBody.getPartner_name());
								model_1.addAttribute("partner_phone", partner_phone);
								int iRow = 0;
								iRow = alarmPartnerService.insertKakaoAlarmTalkPartnerList(model_1);
								if(iRow > 0){
									logger.info("con insert ok:");// 저장
								}else {
									logger.info("con insert error :");
								}
							}else {
								returnData.setError_code(401);
								returnData.setError_val("010 : Partner_phone : "+requestModifyBody.getPartner_phone());
							}
						}
					}

				} catch (Exception e) {
					returnData.setError_code(401);
					returnData.setError_val("try error : "+e);
				}
				
			}else {
				returnData.setError_code(401);
				returnData.setError_val("body Empty Partner_phone");
			}
			
		//================================================
		}else if(curd_type.equals("2")==true) {//2 수정
			
			if(EmptyUtils.isEmpty(requestModifyBody.getSeller_id())==false) {
				
				try {
					Model model_get_1 = new ExtendedModelMap();
					model_get_1.addAttribute("get_type", 3);
					model_get_1.addAttribute("seller_id", requestModifyBody.getSeller_id());
					model_get_1.addAttribute("partner_id", requestModifyBody.getPartner_id());
					getSingleData = alarmPartnerService.getKakaoAlarmTalkPartnerList(model_get_1);

					if(getSingleData.size()==0) {
						returnData.setError_code(401);
						returnData.setError_val("empty getPartner_id :"+requestModifyBody.getPartner_id());
					}else {
						
						Model model_1 = new ExtendedModelMap();
						model_1.addAttribute("up_type", 0);
						model_1.addAttribute("seller_id", requestModifyBody.getSeller_id());
						model_1.addAttribute("partner_id", requestModifyBody.getPartner_id());
						model_1.addAttribute("partner_name", requestModifyBody.getPartner_name());
						model_1.addAttribute("partner_phone", requestModifyBody.getPartner_phone());
						int iRow = 0;
						iRow = alarmPartnerService.updateKakaoAlarmTalkPartnerList(model_1);
						if(iRow > 0){
							logger.info("con insert ok:");// 수정
						}else {
							logger.info("con insert error :");
						}
					}
				} catch (Exception e) {
					returnData.setError_code(401);
					returnData.setError_val("try error : "+e);
				}

			}else {
				returnData.setError_code(401);
				returnData.setError_val("body Empty getSeller_id");
			}
			
		//================================================
		}else if(curd_type.equals("3")==true) {//3 조회
			
			if(EmptyUtils.isEmpty(requestModifyBody.getSeller_id())==false) {
				
				try {
					Model model_get_1 = new ExtendedModelMap();
					model_get_1.addAttribute("get_type", null);
					model_get_1.addAttribute("seller_id", requestModifyBody.getSeller_id());
					getSingleData = alarmPartnerService.getKakaoAlarmTalkPartnerList(model_get_1);
					
					
					returnData.setData(getSingleData);
					
				} catch (Exception e) {
					returnData.setError_code(401);
					returnData.setError_val("try error : "+e);
				}

			}else {
				returnData.setError_code(401);
				returnData.setError_val("body Empty getSeller_id");
				
			}
			
		//================================================	
		}else if(curd_type.equals("4")==true) {//4 삭제
			
			if(EmptyUtils.isEmpty(requestModifyBody.getSeller_id())==false) {
				
				try {
					Model model_get_1 = new ExtendedModelMap();
					model_get_1.addAttribute("get_type", 2);
					model_get_1.addAttribute("seller_id", requestModifyBody.getSeller_id());
					model_get_1.addAttribute("partner_id", requestModifyBody.getPartner_id());
					model_get_1.addAttribute("partner_phone", requestModifyBody.getPartner_phone());
					getSingleData = alarmPartnerService.getKakaoAlarmTalkPartnerList(model_get_1);

					if(getSingleData.size()==0) {
						returnData.setError_code(401);
						returnData.setError_val("empty getPartner_id :"+requestModifyBody.getPartner_id());
					}else {
						
						Model model_1 = new ExtendedModelMap();
						model_1.addAttribute("up_type", 1);
						model_1.addAttribute("seller_id", requestModifyBody.getSeller_id());
						model_1.addAttribute("partner_id", requestModifyBody.getPartner_id());
						int iRow = 0;
						iRow = alarmPartnerService.updateKakaoAlarmTalkPartnerList(model_1);
						if(iRow > 0){
							logger.info("con insert ok:");// 수정
						}else {
							logger.info("con insert error :");
						}
					}
				} catch (Exception e) {
					returnData.setError_code(401);
					returnData.setError_val("try error : "+e);
				}
			}else {
				returnData.setError_code(401);
				returnData.setError_val("body Empty getSeller_id");
			}
		}
		
		return returnData;
	
	}	
}

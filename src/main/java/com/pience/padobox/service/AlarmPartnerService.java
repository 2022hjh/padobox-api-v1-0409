package com.pience.padobox.service;

import java.util.List;

import org.springframework.ui.Model;

import com.pience.padobox.model.AlarmTalkPartnerDomain;
import com.pience.padobox.model.OrderDomain;

public interface AlarmPartnerService {

	public int insertKakaoAlarmTalkPartnerList(Model model);
	
	public List<OrderDomain.PluginSellerInfo> getKakaoAlarmTalkSellerList (Model model);
	
	public List<AlarmTalkPartnerDomain.Data> getKakaoAlarmTalkPartnerList (Model model);
	
	public int updateKakaoAlarmTalkPartnerList(Model model);
	
}

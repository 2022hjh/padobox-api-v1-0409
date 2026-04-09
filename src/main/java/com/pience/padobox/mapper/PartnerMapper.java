package com.pience.padobox.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.ui.Model;

import com.pience.padobox.model.AlarmTalkPartnerDomain;
import com.pience.padobox.model.OrderDomain;


@Mapper
public interface PartnerMapper {
	
	int insertKakaoAlarmTalkPartnerList(Model model);
	
	List<OrderDomain.PluginSellerInfo> getKakaoAlarmTalkSellerList (Model model);
	
	List<AlarmTalkPartnerDomain.Data> getKakaoAlarmTalkPartnerList (Model model);
	
	int updateKakaoAlarmTalkPartnerList(Model model);
		
}

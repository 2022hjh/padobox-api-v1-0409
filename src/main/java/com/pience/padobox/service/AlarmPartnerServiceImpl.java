package com.pience.padobox.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.pience.padobox.mapper.PartnerMapper;
import com.pience.padobox.model.AlarmTalkPartnerDomain;
import com.pience.padobox.model.OrderDomain;


@Service
public class AlarmPartnerServiceImpl implements AlarmPartnerService{
	
	@Autowired
	PartnerMapper partnerMapper;
	
	@Override
	public int insertKakaoAlarmTalkPartnerList(Model model) {
		int iRow = 0;
		iRow = partnerMapper.insertKakaoAlarmTalkPartnerList(model);
		return iRow;
	}
	
	@Override
	public List<OrderDomain.PluginSellerInfo> getKakaoAlarmTalkSellerList (Model model) {
		List<OrderDomain.PluginSellerInfo> select_list = new ArrayList<OrderDomain.PluginSellerInfo>();
		select_list = partnerMapper.getKakaoAlarmTalkSellerList(model);
		return select_list;
	}
	
	@Override
	public List<AlarmTalkPartnerDomain.Data> getKakaoAlarmTalkPartnerList (Model model) {
		List<AlarmTalkPartnerDomain.Data> select_list = new ArrayList<AlarmTalkPartnerDomain.Data>();
		select_list = partnerMapper.getKakaoAlarmTalkPartnerList(model);
		return select_list;
	}
	
	@Override
	public int updateKakaoAlarmTalkPartnerList(Model model) {
		int iRow = 0;
		iRow = partnerMapper.updateKakaoAlarmTalkPartnerList(model);
		return iRow;
	}
	

}

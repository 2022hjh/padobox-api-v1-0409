package com.pience.padobox.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.ui.Model;

import com.pience.padobox.model.AccountDomain;
import com.pience.padobox.model.DefaultDomain;
import com.pience.padobox.model.OrderDomain;
import com.pience.padobox.model.OrderDomain.*;
import com.pience.padobox.model.SetDomain;


@Mapper
public interface OrderMapper {
	
	int insertPurchaseItemGetLog(Model model);
	
	int insertPluginOrderList(PluginOrderListDomain plugin_order_list_domain);
	
	List<OrderDomain.PluginOrderListDomain> getPluginOrderGroupList (Model model);
	
	List<OrderDomain.PluginOrderListDomain> getPluginOrderProductList (Model model);
	
	List<OrderDomain.PluginOrderListDomain> getPluginOrderSingle (Model model);
	
	List<OrderDomain.PluginOrderStatusResult> getPluginOrderMasterSubSingle (Model model);
	
	List<OrderDomain.PluginOrderStatusResult> getPluginOrderStatusResult (Model model);
	
	int insertPluginOrderSubList(Model model);
	
	List<OrderDomain.PluginOrderSubListDomain> getPluginOrderSubList (Model model);
	
	SetDomain.MainMenuCount getPluginMainCnt(Model model);
		
	int insertPluginSellerInfo(PluginSellerInfo pluginSellerInfo);
	
	List<PluginSellerInfo> getPluginSellerInfo (Model model);
	
	int updatePluginSellerInfo(PluginSellerInfo pluginSellerInfo);
	
	int updateOrderStatus(Model model); 
	
	int insertPluginApiCallLog(Model model);
	
	DefaultDomain.CallLogBody getPluginApiCallLog(Model model);
	
	int insertPluginAccountList(AccountDomain.PluginAccountList pluginaccountlist);
	
	int insertPluginAccountDataList(AccountDomain.PluginAccountDataList pluginaccountdatalist);
	
	List<AccountDomain.PluginAccountList> getPluginAccountList(Model model);
	
	List<AccountDomain.PluginAccountDataList> getPluginAccountDataList(Model model);
	
	int insertPluginStatusPorcessLog(Model model);
	
	int insertSellerList(Model model);
	
	List<DefaultDomain.SellerList> getSellerListSchedule (Model model);
	
	List<DefaultDomain.SellerList> getSellerList (Model model);
	
	int updateSellerList(Model model);
		
	int insertKakaoAlarmTalkLog(Model model);
	
}

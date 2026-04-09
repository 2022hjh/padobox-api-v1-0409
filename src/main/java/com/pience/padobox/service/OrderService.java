package com.pience.padobox.service;

import java.util.List;

import org.springframework.ui.Model;

import com.pience.padobox.model.AccountDomain;
import com.pience.padobox.model.DefaultDomain;
import com.pience.padobox.model.OrderDomain;
import com.pience.padobox.model.SetDomain;
import com.pience.padobox.model.OrderDomain.PluginOrderListDomain;
import com.pience.padobox.model.OrderDomain.PluginSellerInfo;

public interface OrderService {
	
	public int insertPurchaseItemGetLog(Model model);
	
	public int insertPluginOrderList(PluginOrderListDomain plugin_order_list_domain);
	
	public List<OrderDomain.PluginOrderListDomain> getPluginOrderGroupList (Model model);
	
	public List<OrderDomain.PluginOrderListDomain> getPluginOrderProductList (Model model);
	
	public List<OrderDomain.PluginOrderListDomain> getPluginOrderSingle (Model model);
	
	public List<OrderDomain.PluginOrderStatusResult> getPluginOrderMasterSubSingle (Model model);
	
	public List<OrderDomain.PluginOrderStatusResult> getPluginOrderStatusResult (Model model);
	
	public int insertPluginOrderSubList(Model model);
	
	public List<OrderDomain.PluginOrderSubListDomain> getPluginOrderSubList (Model model);
	
	public SetDomain.MainMenuCount getPluginMainCnt(Model model);
	
	public int insertPluginSellerInfo(PluginSellerInfo pluginSellerInfo);
	
	public List<PluginSellerInfo> getPluginSellerInfo (Model model);
	
	public int updatePluginSellerInfo(PluginSellerInfo pluginSellerInfo);
	
	public int updateOrderStatus(Model model); 
	
	public int insertPluginApiCallLog(Model model);
	
	public DefaultDomain.CallLogBody getPluginApiCallLog(Model model);

	public int insertPluginAccountList(AccountDomain.PluginAccountList pluginaccountlist);
	
	public int insertPluginAccountDataList(AccountDomain.PluginAccountDataList pluginaccountdatalist);
	
	public List<AccountDomain.PluginAccountList> getPluginAccountList(Model model);
	
	public List<AccountDomain.PluginAccountDataList> getPluginAccountDataList(Model model);
	
	public int insertPluginStatusPorcessLog(Model model);
	
	public int insertSellerList(Model model);
	
	public List<DefaultDomain.SellerList> getSellerListSchedule (Model model);
	
	public List<DefaultDomain.SellerList> getSellerList (Model model);
	
	public int updateSellerList(Model model);
	
	public int insertKakaoAlarmTalkLog(Model model);
	
}

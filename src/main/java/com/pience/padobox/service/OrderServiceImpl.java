package com.pience.padobox.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.pience.padobox.mapper.OrderMapper;
import com.pience.padobox.model.AccountDomain;
import com.pience.padobox.model.DefaultDomain;
import com.pience.padobox.model.OrderDomain;
import com.pience.padobox.model.SetDomain;
import com.pience.padobox.model.OrderDomain.PluginOrderListDomain;
import com.pience.padobox.model.OrderDomain.PluginSellerInfo;


@Service
public class OrderServiceImpl implements OrderService{
	
	@Autowired
	OrderMapper orderMapper;
	
	@Override
	public int insertPurchaseItemGetLog(Model model) {
		int iRow = 0;
		iRow = orderMapper.insertPurchaseItemGetLog(model);
		return iRow;
	}
	
	@Override
	public int insertPluginOrderList(PluginOrderListDomain plugin_order_list_domain) {
		int iRow = 0;
		iRow = orderMapper.insertPluginOrderList(plugin_order_list_domain);
		return iRow;
	}

	@Override
	public List<OrderDomain.PluginOrderListDomain> getPluginOrderGroupList (Model model) {
		List<OrderDomain.PluginOrderListDomain> select_plugin_order_list = new ArrayList<OrderDomain.PluginOrderListDomain>();
		select_plugin_order_list = orderMapper.getPluginOrderGroupList(model);
		return select_plugin_order_list;
	}
	
	@Override
	public List<OrderDomain.PluginOrderListDomain> getPluginOrderProductList (Model model) {
		List<OrderDomain.PluginOrderListDomain> select_plugin_order_list = new ArrayList<OrderDomain.PluginOrderListDomain>();
		select_plugin_order_list = orderMapper.getPluginOrderProductList(model);
		return select_plugin_order_list;
	}
	
	@Override
	public List<OrderDomain.PluginOrderListDomain> getPluginOrderSingle (Model model) {
		List<OrderDomain.PluginOrderListDomain> select_plugin_order_list = new ArrayList<OrderDomain.PluginOrderListDomain>();
		select_plugin_order_list = orderMapper.getPluginOrderSingle(model);
		return select_plugin_order_list;
	}
	
	@Override
	public List<OrderDomain.PluginOrderStatusResult> getPluginOrderMasterSubSingle (Model model){
		List<OrderDomain.PluginOrderStatusResult> select_plugin_order_list = new ArrayList<OrderDomain.PluginOrderStatusResult>();
		select_plugin_order_list = orderMapper.getPluginOrderMasterSubSingle(model);
		return select_plugin_order_list;
	}
	
	@Override
	public List<OrderDomain.PluginOrderStatusResult> getPluginOrderStatusResult (Model model){
		List<OrderDomain.PluginOrderStatusResult> select_plugin_order_list = new ArrayList<OrderDomain.PluginOrderStatusResult>();
		select_plugin_order_list = orderMapper.getPluginOrderStatusResult(model);
		return select_plugin_order_list;
	}
	
	@Override
	public int insertPluginOrderSubList(Model model) {
		int iRow = 0;
		iRow = orderMapper.insertPluginOrderSubList(model);
		return iRow;
	}
	
	@Override
	public List<OrderDomain.PluginOrderSubListDomain> getPluginOrderSubList (Model model) {
		List<OrderDomain.PluginOrderSubListDomain> select_plugin_order_sub_list = new ArrayList<OrderDomain.PluginOrderSubListDomain>();
		select_plugin_order_sub_list = orderMapper.getPluginOrderSubList(model);
		return select_plugin_order_sub_list;
	}
	
	@Override
	public SetDomain.MainMenuCount getPluginMainCnt(Model model) {
		SetDomain.MainMenuCount main_cnt = new SetDomain.MainMenuCount();
		main_cnt = orderMapper.getPluginMainCnt(model);
		return main_cnt;
	}
	
	
	@Override
	public int insertPluginSellerInfo(PluginSellerInfo pluginSellerInfo) {
		int iRow = 0;
		iRow = orderMapper.insertPluginSellerInfo(pluginSellerInfo);
		return iRow;
	}
	
	@Override
	public List<PluginSellerInfo> getPluginSellerInfo (Model model) {
		List<PluginSellerInfo> select_plugin_seller_info_list = new ArrayList<PluginSellerInfo>();
		select_plugin_seller_info_list = orderMapper.getPluginSellerInfo(model);
		return select_plugin_seller_info_list;
	}
	
	@Override
	public int updatePluginSellerInfo(PluginSellerInfo pluginSellerInfo) {
		int iRow = 0;
		iRow = orderMapper.updatePluginSellerInfo(pluginSellerInfo);
		return iRow;
	}
	
	@Override
	public int updateOrderStatus(Model model) {
		int iRow = 0;
		iRow = orderMapper.updateOrderStatus(model);
		return iRow;
	}
	
	@Override
	public int insertPluginApiCallLog(Model model){
		int iRow = 0;
		iRow = orderMapper.insertPluginApiCallLog(model);
		return iRow;
	}
	
	@Override
	public DefaultDomain.CallLogBody getPluginApiCallLog(Model model) {
		DefaultDomain.CallLogBody plugin_api_call_log = new DefaultDomain.CallLogBody();
		plugin_api_call_log = orderMapper.getPluginApiCallLog(model);
		return plugin_api_call_log;
	}
	
	@Override
	public int insertPluginAccountList(AccountDomain.PluginAccountList pluginaccountlist) {
		int iRow = 0;
		iRow = orderMapper.insertPluginAccountList(pluginaccountlist);
		return iRow;
	}
	
	@Override
	public int insertPluginAccountDataList(AccountDomain.PluginAccountDataList pluginaccountdatalist){
		int iRow = 0;
		iRow = orderMapper.insertPluginAccountDataList(pluginaccountdatalist);
		return iRow;
	}
	
	@Override
	public List<AccountDomain.PluginAccountList> getPluginAccountList(Model model) {
		List<AccountDomain.PluginAccountList> plugin_account_list = new ArrayList<AccountDomain.PluginAccountList>();
		plugin_account_list = orderMapper.getPluginAccountList(model);
		return plugin_account_list;
	}
	
	@Override
	public List<AccountDomain.PluginAccountDataList> getPluginAccountDataList(Model model) {
		List<AccountDomain.PluginAccountDataList> plugin_account_data_list = new ArrayList<AccountDomain.PluginAccountDataList>();
		plugin_account_data_list = orderMapper.getPluginAccountDataList(model);
		return plugin_account_data_list;
	}
	
	@Override
	public int insertPluginStatusPorcessLog(Model model) {
		int iRow = 0;
		iRow = orderMapper.insertPluginStatusPorcessLog(model);
		return iRow;
	}
	
	@Override
	public int insertSellerList(Model model) {
		int iRow = 0;
		iRow = orderMapper.insertSellerList(model);
		return iRow;
	}
	
	@Override
	public List<DefaultDomain.SellerList> getSellerListSchedule (Model model) {
		List<DefaultDomain.SellerList> seller_list = new ArrayList<DefaultDomain.SellerList>();
		seller_list = orderMapper.getSellerListSchedule(model);
		return seller_list;
	}
	
	@Override
	public List<DefaultDomain.SellerList> getSellerList (Model model) {
		List<DefaultDomain.SellerList> seller_list = new ArrayList<DefaultDomain.SellerList>();
		seller_list = orderMapper.getSellerList(model);
		return seller_list;
	}
	
	@Override
	public int updateSellerList(Model model) {
		int iRow = 0;
		iRow = orderMapper.updateOrderStatus(model);
		return iRow;
	}
	
	@Override
	public int insertKakaoAlarmTalkLog(Model model){
		int iRow = 0;
		iRow = orderMapper.insertKakaoAlarmTalkLog(model);
		return iRow;
	}


}

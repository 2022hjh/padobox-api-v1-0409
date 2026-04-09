package com.pience.padobox.model;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class OrderBodyDomain {
	
	@Getter
	@Setter
	public static class ModifyBodyList{
		private Integer modify_key;
		private Integer modify_status;
		private Integer order_count;
		private List<ModifyBody> data;
	}
	
	@Getter
	@Setter
	public static class ModifyBody{
		private String order_key;
		private List<String> product_key; 
		private List<String> product_variant_key;
		private String courier_id;
	}
	
	@Getter
	@Setter
	public static class GroupedModifyBody {
	    private String order_key;
	    private String order_id;
	    private List<String> product_variant_key = new ArrayList<>();
	    private String courier_id;
	    private String status_val;
	    private String delivery_val;
	    private String delivery_group_id;
	}
	
	@Getter
	@Setter
	public static class ParamBody{
		private String start_date;
		private String end_date;
		private String search_keyword;
		private String last_idx;
		private String after;
		private Integer view_type;
		private Integer accounts_idx;
		private String last_account_idx;
		private String accounts_key;
	}
}

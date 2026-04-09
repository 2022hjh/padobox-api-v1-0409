package com.pience.padobox.model;


import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class AlarmTalkPartnerDomain {

	@Getter
	@Setter
	public static class BodyData{
		private String seller_id;
		private String partner_id;
		private String partner_name;
		private String partner_phone;
	}
	
	@Getter
	@Setter
	public static class ReturnData{
		private List<Data> data;
		private Integer error_code;
		private String error_val;
		
	}
	
	@Getter
	@Setter
	public static class Data{
		private String seller_id;
		private String partner_id;
		private String partner_name;
		private String partner_phone;
	}
	
	
}

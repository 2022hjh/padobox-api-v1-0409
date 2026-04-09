package com.pience.padobox.model;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class SellerIdListDomain {

	private List<ReturnData> data;
	private Page paging;
	
	@Getter
	@Setter
	public static class ReturnData{
		private String id;
		private String name;
		private ContactInfo contactInformation;
		private String defaultDeliveryContractId;
	}
	
	@Getter
	@Setter
	public static class ContactInfo{
		private String name;
	}
	
	@Getter
	@Setter
	public static class PhoneInfo{
		private String regionCode;
		private String e164;
		private String nationalNumber;
		private String countryCode;
	}
	
	@Getter
	@Setter
	public static class Page{
		private String after;
	}

}

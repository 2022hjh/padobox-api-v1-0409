package com.pience.padobox.model;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class SellerIdDeliveryDomain {
	
	private List<DataSingle> data;
	
	@Getter
	@Setter
	public static class DataSingle{
		private String id;
		private String sellerId;
		private String name;
		private String companyCode;
		private String fareType;
		private ContactInfo contactInformation;
		private Long createdAt;
		private Long updatedAt;
	}
	
	@Getter
	@Setter
	public static class ContactInfo{
		private String name;
		private String email;
		private String phoneNumber;
	}

}

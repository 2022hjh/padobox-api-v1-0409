package com.pience.padobox.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class SellerIdInfoDomain {
	private String id;
	private String name;
	private String groupId;
	private String defaultDeliveryContractId;
	private String delivery_contracts_id;
	private ContactInfo contactInformation;
	private DeliveryInfo deliveryInformation;
	private ContactInfo partnerInfomation;
	private Integer error_code;
	private String error_val;
	
	@Getter
	@Setter
	public class ContactInfo{
		private String email;
		private String name;
		private String phoneNumber;
	}
	
	@Getter
	@Setter
	public class DeliveryInfo{
		private String address;
		private String address2;
		private String zipcode;
	}
	
}

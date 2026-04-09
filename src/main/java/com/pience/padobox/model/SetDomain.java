package com.pience.padobox.model;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class SetDomain {
	
	private Integer sellerid_sync;
	private SellerIdInfo sellerId_info;
	private MainMenuCount main_menu_count;
	private List<CourierComList> courier_com_list;
	
	@Getter
	@Setter
	public static class StatusModifyReturn{
		private int error_code;
		private String error_val;
		private String result_return;
	}
	
	@Getter
	@Setter
	public static class ControllerResultStatusModifyReturn{
		private int error_code;
		private String error_val;
		private ReturnModifyResult result_return;
	}
	
	@Getter
	@Setter
	public static class ControllerResultStatusModifyReturnV2{
		private int error_code;
		private String error_val;
		private ReturnModifyResultV2 result_return;
	}
	
	@Getter
	@Setter
	public static class StatusModifyResultReturn{
		private ModifyResult result;
	}
	
	@Getter
	@Setter
	public static class StatusModifyResultReturnDelivery{
		private DeliveryReceptionResult result;
		
		@Getter
		@Setter
		public static class DeliveryReceptionResult{
			private ResultV2 v2;
			
			@Getter
			@Setter
			public static class DeliveryReceptionResultSingle{
				private String id;
			}
			
			@Getter
			@Setter
			public static class ResultV2{
				private List<ResultV2TypeSingleData> failures;
				private List<ResultV2TypeSingleData> success;
				private List<ResultV2TypeSingleData> unavailables;
				
				@Getter
				@Setter
				public static class ResultV2TypeSingleData{
					private ResultV2TypeSingleDataValue data;
				}
				
				@Getter
				@Setter
				public static class ResultV2TypeSingleDataValue{
					private String purchaseId;
					private List<String> items;
				}
			}
		}
	}
	
	@Getter
	@Setter
	public static class ModifyResult{
		private List<String> failures;
		private List<String> success;
		private List<UnavailablesStatusModify> unavailables;
	}
	
	@Getter
	@Setter
	public static class UnavailablesStatusModify{
		private String id;
		private String errorMessage;
	}
	
	@Getter
	@Setter
	public static class ReturnModifyResult{
		private List<OrderDomain.ReturnData> failures;
		private List<OrderDomain.ReturnData> success;
		private List<OrderDomain.ReturnData> unavailables;
		private String alarm_send;
	}
	
	@Getter
	@Setter
	public static class ReturnModifyResultV2{
		private List<OrderDomain.ReturnDatav1> failures;
		private List<OrderDomain.ReturnDatav1> success;
		private List<OrderDomain.ReturnDatav1> unavailables;
		private String alarm_send;
		private String all_process_yn;
	}
	
	@Getter
	@Setter
	public static class DeliveriesBody{
		private DeliveriesBodySingle deliveries;
	}
	
	@Getter
	@Setter
	public static class DeliveriesBodySingle{
		private String purchaseId;
		private List<String> purchaseItemIds;
		private String deliveryContractId;
		
	}
	
	@Getter
	@Setter
	public static class SellerIdInfo{
		private String id;
		private String name;
		private String groupId;
		private ContactInfo contact_information;
		private DeliveryInfo delivery_information;
		
		@Getter
		@Setter
		public static class ContactInfo{
			private String email;
			private String name;
			private String phone_number;
		}
		
		@Getter
		@Setter
		public static class DeliveryInfo{
			private String address;
			private String address2;
			private String zipcode;
		}
	}
	
	@Getter
	@Setter
	public static class MainMenuCount{
		private Integer new_order_cnt = 0;
		private Integer possible_order_cnt = 0;
		private Integer observ_order_cnt = 0;
		private Integer delivery_order_cnt = 0;
		private Integer cancel_order_cnt = 0;
		private Integer receiving_order_cnt = 0;
		private Integer async_count = 0;
		private Integer observ_new_order_cnt = 0;
		private Integer observ_possible_order_cnt = 0;
		private Integer observ_2_order_cnt = 0;
	}
	
	@Getter
	@Setter
	public static class CourierComList{
		private String courier_id;
		private String courier_name;
		private String courier_code;
	}
	
	@Getter
	@Setter
	public static class ProcessOrdersCommonResult {
	    private int error_code = 0;
	    private String error_val = "";
	    private ReturnModifyResultV2 modify_result = new ReturnModifyResultV2();
	    private String status_modify_log = "";
	    private String delivery_reception_log = "";
	    private int success_count = 0;
	    private int failure_count = 0;
	    private int unavailable_count = 0;
	}

}

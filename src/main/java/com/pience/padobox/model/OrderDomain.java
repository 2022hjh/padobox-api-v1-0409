package com.pience.padobox.model;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class OrderDomain {
	
	private List<ReturnData> data;
	private List<ReturnDatav1> datav2;
	private String last_idx;
	private Integer order_product_total_count;
	private Integer search_product_total_count;
	private Integer order_product_cnt;
	private Integer order_new_cnt;
	private Integer order_poss_cnt;
	private Integer async_count;
	private Integer error_code;
	private String error_val;
	
	@Getter
	@Setter
	public static class ReturnData{
		private List<String> data_id;
		private String order_key;
		private String order_status;
		private String user_id;
		private String user_name;
		private List<ProductSingle> product_list;
		private String pay_date;
		private String failures_reason;
		private String unavailables_reason;
		private String delivery_reception;
	}
	
	@Getter
	@Setter
	public static class ReturnDatav1{
		private String order_key;
		private String order_status;
		private String user_id;
		private String user_name;
		private String recipient_name;
		private String product_key;
		private Integer product_status;
		private String product_name;
		private List<ProductVariantSingle> product_variant_list;
		private String pay_date;
		private String buyer_name;
		private String buyer_phone;
		private String recipient_phone;
		private String recipient_zipcode;
		private String recipient_address;
		private String recipient_memo;
		private String delivery_company_name;
		private String tracking_number;
		private String failures_reason;
		private String unavailables_reason;
		private String delivery_reception;
	}
	
	@Getter
	@Setter
	public static class ProductSingle{
		private String product_key;
		private Integer product_status;
		private String product_name;
		private List<ProductVariantSingle> product_variant_list;
		private String buyer_name;
		private String buyer_phone;
		private String recipient_name;
		private String recipient_phone;
		private String recipient_zipcode;
		private String recipient_address;
		private String recipient_memo;
	}
	
	@Getter
	@Setter
	public static class ProductVariantSingle{
		private String product_variant_key;
		private String product_variant_id;
		private String  product_variant_value;
		private String  product_variant_name;
		private Integer quantity;
		private Integer product_variant_type;
	}
	
	@Getter
	@Setter
	public static class PluginOrderListDomain{
		private Integer idx;
		private String data_id;
		private String seller_id;
		private String paymentId;
		private String purchaseId;
		private String productId;
		private String settlementId;
		private Integer order_status;
		private Integer order_sub_status;
		private String moim_status;
		private String user_id;
		private String buyer_name;
		private String paid_date;
		private String product_name;
		private String buyer_phone;
		private String recipient_name;
		private String recipient_phone;
		private String zipcode;
		private String address;
		private String address2;
		private String memo;
		private String last_status_updated_at;
		private String updated_at;
		private String refunded_at;
		private String created_at;
		private String paid_at;
		private String regdate;
		private String moddate;
		private Integer del_yn;
		private Long paid_date_unixtime;
		private Long updated_at_unixtime;
		private Long refunded_at_unixtime;
		private Long created_at_unixtime;
		private Integer search_group_cnt;
		private String product_variant_id;
		private String product_variant_name;
		private Integer product_variant_quantity;
		private Integer product_box_cnt;
		
	}
	
	@Getter
	@Setter
	public static class PluginOrderStatusResult{
		private Integer idx;
		private String data_id;
		private String purchaseId;
		private String user_id;
		private String user_name;
		private String pay_date;
		private String product_key;
		private String product_name;
		private String product_variant_id;
		private String product_variant_value;
		private Integer quantity;
	}
	@Getter
	@Setter
	public static class PluginOrderSubListDomain{
		private Integer idx;
		private Integer plugin_order_list_idx;
		private String data_id;
		private String seller_id;
		private String paymentId;
		private String purchaseId;
		private String product_variant_id;
		private String product_variant_name;
		private Integer product_variant_quantity;
		private Integer product_box_cnt;
		private String regdate;
		private String moddate;
		private Integer del_yn;
		private int order_status;
		private int order_sub_status;
	}

	@Getter
	@Setter
	public static class PluginSellerInfo{
		private Integer idx;
		private String seller_id;
		private String seller_name;
		private Integer list_type;
		private String token_val;
		private Integer delivery_num;
		private String delivery_id;
		private String delivery_name;
		private String delivery_company_code;
		private String delivery_contactInfo_name;
		private String delivery_contactInfo_email;
		private String delivery_contactInfo_phone;
		private Long delivery_createdAt;
		private Long delivery_updatedAt;
		private String regdate;
		private String moddate;
		private Integer del_yn;
	}
	
	
}

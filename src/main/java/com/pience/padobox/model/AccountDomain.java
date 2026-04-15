package com.pience.padobox.model;


import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class AccountDomain {
	
	@Getter
	@Setter
	public static class ReturnData{
		private AccountWeekData search_data;
		private List<AccountWeekData> account_week_list;
		private String last_idx;
		private String last_account_idx;
		private Integer error_code;
		private String error_val;
	}
	
	@Getter
	@Setter
	public static class AccountWeekData{
		private Integer accounts_idx; 
		private String accounts_key;
		private String accounts_start_date;
		private String accounts_end_date;
		private Integer accounts_total_price; 
		private Integer accounts_status;
		private Integer supply_price;
        private Integer delivery_fee;
        private Integer extra_price;
        private Integer refunded_price;
		private List<ProductOptionSingle> product_list;
		private List<ExcelProductSingle> excel_data;
		
	}

	@Getter
	@Setter
	public static class ProductSingle{
		private String product_key;
		private String product_name;
		private Integer product_total_price;
		private List<ProductVariantSingel> product_variant_list;
	}
		
	@Getter
	@Setter
	public static class ProductOptionSingle{
		private String product_name;
		private String product_variant_name;
		private Integer product_variant_total_price;
	}
	
	@Getter
	@Setter
	public static class ExcelProductSingle{
		private String order_id;
		private String settle_date;
		private String completed_at;
		public String targeted_date;
		private String product_name;
		private String order_user_name;
		private Integer quantity;
		private Integer supply_price;
		private Integer product_price;
		private Integer delivery_fee;
		private Integer commission;
		private Integer total_price;
		private Integer extra_price;
		private Integer refunded_price;
	}
	
	@Getter
	@Setter
	public static class ProductVariantSingel{
		private String order_id;
		private String product_variant_id;
		private String product_variant_name;
		private Integer product_variant_total_price;
	}

	
	@Getter
	@Setter
	public static class MoimGetList{
		private List<ListDataSingle> data;
		private TotalData total;
	}
	
	@Getter
	@Setter
	public static class ListDataSingle{
		private String id;
		private String name;
		private String sellerId;
		private Long settleDate;
		private Integer supplyPrice;
		private Integer productPrice;
		private Integer deliveryFee;
		private Integer commission;
		private Integer refundedPrice;
		private Integer extraPrice;
		private  String status;
		private Integer totalPrice;
		private Long completedAt;
	}
	
	@Getter
	@Setter
	public static class TotalData{
		private Integer count;
		private Integer supplyPrice;
		private Integer productPrice;
		private Integer deliveryFee;
		private Integer commission;
		private Integer refundedPrice;
		private Integer extraPrice;
		private Integer totalPrice;
	}
	
	@Getter
	@Setter
	public static class MoimGetSingle{
		private List<DataSingle> data;
		private SetTlement settlement;
		private Pageing paging;
	}
	
	@Getter
	@Setter
	public static class SetTlement{
		private String id;
		private String parentSellerId;
		private String sellerId;
		private Integer productPrice;
		private Integer discountedPrice;
		private Integer supplyPrice;
		private Integer deliveryFee;
		private Integer commission;
		private Integer refundedPrice;
		private Integer extraPrice;
		private Integer couponDiscount;
		private Integer subSellerCouponDiscount;
		private String status;
		private Long startAt;
		private Long settleDate;
		private Long closedAt;
		private Long completedAt;
		private String referenceField;
		private Integer defaultTerm;
		private Long createdAt;
		private Long updatedAt;
		private Integer totalPrice;
	}
	
	@Getter
	@Setter
	public static class DataSingle{
		private String id;
		private String settlementId;
		private String parentSellerId;
		private String sellerId;
		private String type;
		private String purchaseItemId;
		private Integer quantity;
		private Integer productPrice;
		private Integer discountedPrice;
		private Integer supplyPrice;
		private Integer deliveryFee;
		private Integer commission;
		private Integer refundedPrice;
		private Integer extraPrice;
		private Integer couponDiscount;
		private Integer subSellerCouponDiscount;
		private Long targetedDate;
		private Long createdAt;
		private Long updatedAt;
		private Integer totalPrice;
		private SetTlement settlement;
		private String description;
		private String product_name;
		private String product_variant_name;
		private Integer product_variant_total_price;
		private String order_user_name;
	}
	
	@Getter
	@Setter
	public static class Pageing{
		private String after;
	}
	
	@Getter
	@Setter
	public static class PluginAccountList{
		private int idx;
		private String settlement_id;
		private String seller_id;
		private String seller_name;
		private Long settle_date;
		private Integer supply_price;
		private Integer product_price;
		private Integer delivery_fee;
		private Integer commission;
		private Integer refunded_price;
		private Integer extra_price;
		private String account_status;
		private Integer total_price;
		private Integer discounted_price;
		private Integer coupon_discount;
		private Integer sub_seller_coupon_discount;
		private Long start_at;
		private Long closed_at;
		private Long completed_at;
		private String reference_field;
		private Integer default_term;
		private Long created_at;
		private Long updated_at;
	}
	
	
	@Getter
	@Setter
	public static class PluginAccountDataList{
		private int idx;
		private Integer plugin_account_list_idx;
		private String settlement_id;
		private String settlement_data_id;
		private String parent_seller_id;
		private String seller_id;
		private String sett_type;
		private String purchase_item_id;
		private Integer quantity;
		private Integer product_price;
		private Integer discounted_price;
		private Integer supply_price;
		private Integer delivery_fee;
		private Integer commission;
		private Integer refunded_price;
		private Integer extra_price;
		private Integer coupon_discount;
		private Integer sub_seller_coupon_discount;
		private Long targeted_date;
		private Long created_at;
		private Long updated_at;
		private Integer total_price;
		private String product_name;
		private String product_variant_name;
		private String order_name;
	}
}

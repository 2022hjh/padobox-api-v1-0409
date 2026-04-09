package com.pience.padobox.model;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class OrderGetDomain {

	@Getter
	@Setter
	public static class GetData{
		private List<productVarinatSingle> data;
		private Page paging;
	} 
	
	@Getter
	@Setter
	public static class productVarinatSingle{
		private String deliveryId;
		private String deliveryCompanyCode;
		private String deliveryCompanyName;
		private String trackingNumber;
		private String deliveryGroupId;
		private String productVariantId;
		private SupplyPricePrice supplyPrice_price;
		private Integer paidOrder;
		private DeliveryGroup deliveryGroup;
		private String type;
		private String productName;
		private Long createdAt;
		private boolean shippingRequired;
		private String sellerId;
		private String paymentId;
		private Integer price;
		private boolean confirmable;
		private String displayingStatus;
		private String id; 
		private String parentSellerId;
		private CouponDiscountPrice couponDiscount_price;
		private Long updatedAt;
		private Long refundedAt;
		private String productId;
		private Integer weight;
		private Integer refundedQuantity;
		private boolean deliverAlone;
		private Integer couponDiscount;
		private String reviewable;
		private String profileId;
		private Integer normalPrice;
		private Long paidAt;
		private Integer creditExpiration;
		private Integer creditAmount;
		private Object productVariantValue;
		private String status;
		private String purchaseId;
		private String settlementId;
		private SubSellerCouponDiscountPrice subSellerCouponDiscount_price;
		private String imageUrl;
		private String currency;
		private Integer refundedProductPrice;
		private Integer subSellerCouponDiscount;
		private Integer refundedSubSellerCouponDiscount;
		private Integer supplyPrice;
		private Integer quantity;
		private boolean isGuestCheckout;
		private Purchase purchase;
		private String deliveryType;
		private CreditAmountPrice creditAmount_price;
		private Integer refundedDeliveryFee;
		private PricePrice price_price;
		private DeliveryFeePrice deliveryFee_price;
		private String userId;
		private boolean isBackOrder;
		private Integer deliveryFee;
		private Integer refundedCouponDiscount;
		private NormalPricePrice normalPrice_price;
		private boolean anonymous;
	}
	
	@Getter
	@Setter
	public static class SupplyPricePrice{
		private String formattedValue;
		private String  currency;
		private String value;
		private Integer numValue;
	}
	
	@Getter
	@Setter
	public static class DeliveryGroup{
		private Policy policy;
		private String id;
		
		@Getter
		@Setter
		public class Policy{
			private String name;
			private String description;
			private String id;
			private String type;
			private List<PriceList> priceList;
		}
		
		@Getter
		@Setter
		public class PriceList{
			private Integer price;
			private Integer gte;
		}
	}
	
	@Getter
	@Setter
	public static class CouponDiscountPrice{
		private String formattedValue;
		private String currency;
		private String value;
		private Integer numValue;
	}
	
	@Getter
	@Setter
	public static class AdditionalFees{
		
	}
	
	@Getter
	@Setter
	public static class SubSellerCouponDiscountPrice{
		private String formattedValue;
		private String currency;
		private String value;
		private Integer numValue;
	}
	
	@Getter
	@Setter
	public static class Purchase{
		private PhoneInfo recipientPhoneInfo;
		private Integer totalPrice;
		private String buyerEmail;
		private Integer paidOrder;
		private String memo;
		private String productName;
		private PhoneInfo buyerPhoneInfo;
		private Long createdAt;
		private String sellerId;
		private String paymentId;
		private String countryCode;
		private String recipientName;
		private String currency;
		private TotalPricePrice totalPrice_price;
		private String id;
		private String parentSellerId;
		private TotalPricePrice couponDiscount_price;
		private Integer refundedProductPrice;
		private Long updatedAt;
		private String recipientPhone;
		private String address;
		private String isGuestCheckout;
		private String address2;
		private Integer refundedDeliveryFee;
		private String buyerPhoneNumber;
		private boolean backOrderIncluded;
		private TotalPricePrice deliveryFee_price;
		private String buyerName;
		private String userId;
		private Integer cancelledCouponDiscount;
		private String zipcode;
		private Integer couponDiscount;
		private Integer deliveryFee;
		private String buyerPhone;
		private String recipientPhoneNumber;
		private String profileId;
		private Integer totalWeight;
		private boolean anonymous;
		private Long paidAt;
		private String status;
		private String username;
		
		@Getter
		@Setter
		public class PhoneInfo{
			private String regionCode;
			private String e164;
			private String nationalNumber;
			private String countryCode;
		}
		
		@Getter
		@Setter
		public class TotalPricePrice{
			private String formattedValue;
			private String  currency;
			private String value;
			private Integer numValue;
		}
	}
	
	@Getter
	@Setter
	public static class CreditAmountPrice{
		private String formattedValue;
		private String  currency;
		private String value;
		private Integer numValue;
	}
	
	@Getter
	@Setter
	public static class PricePrice{
		private String formattedValue;
		private String  currency;
		private String value;
		private Integer numValue;
	}
	
	@Getter
	@Setter
	public static class DeliveryFeePrice{
		private String formattedValue;
		private String  currency;
		private String value;
		private Integer numValue;
	}
	@Getter
	@Setter
	public static class NormalPricePrice{
		private String formattedValue;
		private String  currency;
		private String value;
		private Integer numValue;
	}
	
	@Getter
	@Setter
	public static class Page{
		private Integer total;
		private String after = "";
	}
	
	@Getter
	@Setter
	public static class StatusGroupIds{
		private String purchase_id;
		private String product_id;
		private List<String> data_id;
		private String order_status;
	}
	
}


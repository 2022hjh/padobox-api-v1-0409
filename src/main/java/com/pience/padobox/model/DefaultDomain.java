package com.pience.padobox.model;

import java.util.List;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class DefaultDomain {
	
	private String id;
	
	@Getter
	@Setter
	public static class SellerCheck{
		private String sellerid;
	} 
	
	@Getter
	@Setter
	public static class OrderSinglePostBody{
		private List<String> ids;
	}
	
	@Getter
	@Setter
	public static class OrderListPostBody{
		private List<String> status;
		private int limit;
		private List<SortSingle> sorts;
		private PaidAt paidAt;
		private RefundedAt refundedAt;
		private String after;
		private Long updateat;
	}
	
	@Getter
	@Setter
	public static class SortSingle{
		private CreatedAt createdAt;
	} 
	
	@Getter
	@Setter
	public static class CreatedAt{
		private String order;
	}
	
	@Getter
	@Setter
	public static class PaidAt{
		private Long gte;
		private Long lte;
	}
	
	@Getter
	@Setter
	public static class RefundedAt{
		private int lte;
	}
	
	@Getter
	@Setter
	public static class ErrorCheck{
		private int error_code;
		private String error_val;
	}
	
	@Getter
	@Setter
	public static class CallLogBody{
		private int idx;
		private String api_call_type;
		private String seller_id;
		private String after_val;
		private Long at_val;
		private String sync_type;
		private String regdate;
	}
	
	@Getter
	@Setter
	public static class SellerList{
		private int idx;
		private String seller_id;
		private String seller_name;
		private String regdate;
		private String moddate;
	}
	
	@Getter
	@Setter
	public static class KakaoAlramTalkPostBody{
		private String msgid;
		private String message_type;
		private String profile_key;
		private String template_code;
		private String receiver_num;
		private String reserved_time;
		private String message;
		private ButtonObject button1;
		private ButtonObject button2;
		
		@Getter
		@Setter
		public static class ButtonObject{
			private String name;
			private String type;
			private String url_mobile;
			private String url_pc;
		}
	}
	
	@Getter
	@Setter
	public static class KakaoAlramTalkPostResult{
		private String result;
		private String code;
		private String kind;
		private String msgid;
		private String originCode;
		private String error;
		private String sendtime;
		private String originError;
	}

}

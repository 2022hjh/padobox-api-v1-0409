package com.pience.padobox.model;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class AsyncDomain {
	
	@Getter
	@Setter
	public static class StatusProcessAsyncLog{
		private int idx;
		private String request_id;
		private String seller_id;
		private String order_data_all;
		private String process_status;
		private String order_status_result;
		private String async_yn;
		private String async_error;
		private int total_count;
		private int success_count;
		private int failure_count;
		private int unavailable_count;
		private String start_date;
		private String end_date;
		private String alarm_send;
		
	}
	
}

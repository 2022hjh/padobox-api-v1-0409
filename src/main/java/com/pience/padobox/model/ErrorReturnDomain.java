package com.pience.padobox.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorReturnDomain {
	
	private int idx;
	private int rest_type;
	private String type_comment;
	private int error_code;
	private String error_value;
	private String rest_path_refer;
	private String regdate;
	private String moddate;
	private int del_yn;
	
}